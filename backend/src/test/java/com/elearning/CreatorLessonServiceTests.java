package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.CreateLessonRequest;
import com.elearning.dto.request.ReorderVocabRequest;
import com.elearning.dto.request.UpdateLessonRequest;
import com.elearning.dto.request.VocabOrderItem;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.LessonVocabularyId;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.dto.response.ImportValidationReport;
import com.elearning.dto.response.ParsedVocabularyItem;
import com.elearning.service.ExcelParserService;
import com.elearning.service.impl.CreatorLessonServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 5B.1 & 5C.2: CreatorLessonService Unit Tests")
class CreatorLessonServiceTests {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private ExcelParserService excelParserService;

    @Mock
    private com.elearning.repository.ModerationLogRepository moderationLogRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CreatorLessonServiceImpl creatorLessonService;

    private Account creatorA;
    private Account creatorB;
    private Vocabulary vocab1;
    private Vocabulary vocab2;
    private Vocabulary vocab3;

    @BeforeEach
    void setUp() {
        creatorA = new Account();
        creatorA.setAccountId(1L);
        creatorA.setEmailOrPhone("creator_a@example.com");

        creatorB = new Account();
        creatorB.setAccountId(2L);
        creatorB.setEmailOrPhone("creator_b@example.com");

        vocab1 = new Vocabulary("学", "xué", "xue", "Học", "Học tập");
        vocab1.setVocabId(10L);

        vocab2 = new Vocabulary("生", "shēng", "sheng", "Sinh", "Học sinh");
        vocab2.setVocabId(20L);

        vocab3 = new Vocabulary("校", "xiào", "xiao", "Hiệu", "Trường học");
        vocab3.setVocabId(30L);

        setAuthentication("creator_a@example.com", "ROLE_CREATOR");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String username, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username, "N/A", List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("1. Create Lesson Tests")
    class CreateLessonTests {

        @Test
        @DisplayName("GIVEN valid CreateLessonRequest WHEN createLesson THEN returns Draft lesson owned by creator")
        void testCreateLesson_success() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson savedLesson = new Lesson("Bài 1 Mới", creatorA);
            savedLesson.setLessonId(100L);
            savedLesson.setStatus("Draft");
            when(lessonRepository.save(any(Lesson.class))).thenReturn(savedLesson);
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of());

            CreateLessonRequest request = new CreateLessonRequest("Bài 1 Mới");
            LessonDetailResponse response = creatorLessonService.createLesson(request);

            assertThat(response).isNotNull();
            assertThat(response.getLessonId()).isEqualTo(100L);
            assertThat(response.getTitle()).isEqualTo("Bài 1 Mới");
            assertThat(response.getStatus()).isEqualTo("Draft");
            assertThat(response.getVocabularyCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("GIVEN blank title WHEN createLesson THEN throws BAD_REQUEST")
        void testCreateLesson_blankTitle() {
            CreateLessonRequest request = new CreateLessonRequest("   ");

            assertThatThrownBy(() -> creatorLessonService.createLesson(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("GIVEN unauthenticated context WHEN createLesson THEN throws UNAUTHORIZED")
        void testCreateLesson_unauthenticated() {
            SecurityContextHolder.clearContext();
            CreateLessonRequest request = new CreateLessonRequest("Bài 1");

            assertThatThrownBy(() -> creatorLessonService.createLesson(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        }
    }

    @Nested
    @DisplayName("2. Get My Lessons Tests (BE-PERF-002 Optimized)")
    class GetMyLessonsTests {

        @Test
        @DisplayName("GIVEN authenticated creator WHEN getMyLessons THEN returns paginated summary of own lessons with bulk count")
        void testGetMyLessons_success() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson1 = new Lesson("Bài 1", creatorA);
            lesson1.setLessonId(101L);
            lesson1.setStatus("Draft");

            Page<Lesson> lessonPage = new PageImpl<>(List.of(lesson1), PageRequest.of(0, 10), 1);
            when(lessonRepository.findByCreatedBy(eq(creatorA), any(Pageable.class))).thenReturn(lessonPage);
            when(lessonVocabularyRepository.countVocabulariesByLessonIds(List.of(101L)))
                    .thenReturn(List.<Object[]>of(new Object[]{101L, 3L}));

            Page<LessonSummaryResponse> result = creatorLessonService.getMyLessons(PageRequest.of(0, 10));

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Bài 1");
            assertThat(result.getContent().get(0).getVocabularyCount()).isEqualTo(3);
            verify(lessonVocabularyRepository, times(1)).countVocabulariesByLessonIds(List.of(101L));
            verify(lessonVocabularyRepository, never()).countByLesson_LessonId(anyLong());
        }

        @Test
        @DisplayName("GIVEN multiple lessons with mixed vocabulary counts WHEN getMyLessons THEN correctly maps counts including zero-count lessons and preserves order")
        void testGetMyLessons_multipleLessonsMixedCounts() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson1 = new Lesson("Bài 1", creatorA);
            lesson1.setLessonId(101L);
            Lesson lesson2 = new Lesson("Bài 2 (Empty)", creatorA);
            lesson2.setLessonId(102L);
            Lesson lesson3 = new Lesson("Bài 3", creatorA);
            lesson3.setLessonId(103L);

            Page<Lesson> lessonPage = new PageImpl<>(List.of(lesson1, lesson2, lesson3), PageRequest.of(0, 10), 3);
            when(lessonRepository.findByCreatedBy(eq(creatorA), any(Pageable.class))).thenReturn(lessonPage);

            // DB returns aggregates only for lessons with rows (101 -> 3, 103 -> 5; 102 has 0 and is absent from DB result)
            when(lessonVocabularyRepository.countVocabulariesByLessonIds(List.of(101L, 102L, 103L)))
                    .thenReturn(List.<Object[]>of(
                            new Object[]{101L, 3L},
                            new Object[]{103L, 5L}
                    ));

            Page<LessonSummaryResponse> result = creatorLessonService.getMyLessons(PageRequest.of(0, 10));

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            // Lesson 1
            assertThat(result.getContent().get(0).getLessonId()).isEqualTo(101L);
            assertThat(result.getContent().get(0).getVocabularyCount()).isEqualTo(3);
            // Lesson 2 (zero count defaulting)
            assertThat(result.getContent().get(1).getLessonId()).isEqualTo(102L);
            assertThat(result.getContent().get(1).getVocabularyCount()).isEqualTo(0);
            // Lesson 3
            assertThat(result.getContent().get(2).getLessonId()).isEqualTo(103L);
            assertThat(result.getContent().get(2).getVocabularyCount()).isEqualTo(5);

            // Bounded query verification: exactly 1 bulk count query, 0 per-lesson queries
            verify(lessonVocabularyRepository, times(1)).countVocabulariesByLessonIds(List.of(101L, 102L, 103L));
            verify(lessonVocabularyRepository, never()).countByLesson_LessonId(anyLong());
        }

        @Test
        @DisplayName("GIVEN empty lesson list WHEN getMyLessons THEN returns empty page without executing count query")
        void testGetMyLessons_emptyPage() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Page<Lesson> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(lessonRepository.findByCreatedBy(eq(creatorA), any(Pageable.class))).thenReturn(emptyPage);

            Page<LessonSummaryResponse> result = creatorLessonService.getMyLessons(PageRequest.of(0, 10));

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
            verify(lessonVocabularyRepository, never()).countVocabulariesByLessonIds(any());
            verify(lessonVocabularyRepository, never()).countByLesson_LessonId(anyLong());
        }
    }

    @Nested
    @DisplayName("3. Get My Lesson By ID Tests (Ownership & Admin Bypass)")
    class GetMyLessonByIdTests {

        @Test
        @DisplayName("GIVEN owned lesson ID WHEN getMyLessonById THEN returns lesson detail")
        void testGetMyLessonById_ownedSuccess() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài của A", creatorA);
            lesson.setLessonId(100L);
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of());

            LessonDetailResponse response = creatorLessonService.getMyLessonById(100L);

            assertThat(response).isNotNull();
            assertThat(response.getLessonId()).isEqualTo(100L);
            assertThat(response.getTitle()).isEqualTo("Bài của A");
        }

        @Test
        @DisplayName("GIVEN lesson owned by Creator B WHEN Creator A calls getMyLessonById THEN throws FORBIDDEN (Checkpoint 5B)")
        void testGetMyLessonById_wrongOwnerThrowsForbidden() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lessonOfB = new Lesson("Bài của B", creatorB);
            lessonOfB.setLessonId(200L);
            when(lessonRepository.findById(200L)).thenReturn(Optional.of(lessonOfB));

            assertThatThrownBy(() -> creatorLessonService.getMyLessonById(200L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }

        @Test
        @DisplayName("GIVEN nonexistent lesson ID WHEN getMyLessonById THEN throws NOT_FOUND")
        void testGetMyLessonById_notFound() {
            when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> creatorLessonService.getMyLessonById(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("GIVEN lesson owned by Creator B WHEN user has ROLE_ADMIN THEN bypasses ownership and succeeds")
        void testGetMyLessonById_adminBypass() {
            setAuthentication("admin@example.com", "ROLE_ADMIN");
            Account adminAccount = new Account();
            adminAccount.setAccountId(99L);
            adminAccount.setEmailOrPhone("admin@example.com");
            when(accountRepository.findByEmailOrPhone("admin@example.com")).thenReturn(Optional.of(adminAccount));

            Lesson lessonOfB = new Lesson("Bài của B", creatorB);
            lessonOfB.setLessonId(200L);
            when(lessonRepository.findById(200L)).thenReturn(Optional.of(lessonOfB));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(200L)).thenReturn(List.of());

            LessonDetailResponse response = creatorLessonService.getMyLessonById(200L);
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("Bài của B");
        }
    }

    @Nested
    @DisplayName("4. Update Lesson Tests")
    class UpdateLessonTests {

        @Test
        @DisplayName("GIVEN Draft lesson WHEN updateMyLesson THEN updates title and returns updated response")
        void testUpdateMyLesson_draftSuccess() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Tiêu đề cũ", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Draft");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of());

            UpdateLessonRequest request = new UpdateLessonRequest("Tiêu đề mới");
            LessonDetailResponse response = creatorLessonService.updateMyLesson(100L, request);

            assertThat(response.getTitle()).isEqualTo("Tiêu đề mới");
            verify(lessonRepository).save(lesson);
        }

        @Test
        @DisplayName("GIVEN Rejected lesson WHEN updateMyLesson THEN also allowed to revise")
        void testUpdateMyLesson_rejectedSuccess() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài bị từ chối", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Rejected");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of());

            UpdateLessonRequest request = new UpdateLessonRequest("Bài sửa lại");
            LessonDetailResponse response = creatorLessonService.updateMyLesson(100L, request);

            assertThat(response.getTitle()).isEqualTo("Bài sửa lại");
        }

        @Test
        @DisplayName("GIVEN Pending lesson WHEN updateMyLesson THEN throws CONFLICT (locked during review)")
        void testUpdateMyLesson_pendingLocked() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài đang chờ duyệt", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Pending");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

            UpdateLessonRequest request = new UpdateLessonRequest("Tiêu đề mới");

            assertThatThrownBy(() -> creatorLessonService.updateMyLesson(100L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }

        @Test
        @DisplayName("GIVEN Approved lesson WHEN updateMyLesson THEN throws CONFLICT (cannot directly edit published)")
        void testUpdateMyLesson_approvedLocked() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài đã xuất bản", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Approved");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

            UpdateLessonRequest request = new UpdateLessonRequest("Tiêu đề mới");

            assertThatThrownBy(() -> creatorLessonService.updateMyLesson(100L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }
    }

    @Nested
    @DisplayName("5. Delete Lesson Tests")
    class DeleteLessonTests {

        @Test
        @DisplayName("GIVEN Draft lesson WHEN deleteMyLesson THEN deletes successfully")
        void testDeleteMyLesson_draftSuccess() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài nháp", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Draft");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

            creatorLessonService.deleteMyLesson(100L);
            verify(lessonRepository).delete(lesson);
        }

        @Test
        @DisplayName("GIVEN Approved lesson WHEN deleteMyLesson THEN throws CONFLICT")
        void testDeleteMyLesson_approvedThrowsConflict() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài đã xuất bản", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Approved");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

            assertThatThrownBy(() -> creatorLessonService.deleteMyLesson(100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }

        @Test
        @DisplayName("GIVEN Rejected lesson with ModerationLog history WHEN deleteMyLesson THEN throws CONFLICT to preserve audit trail")
        void testDeleteMyLesson_rejectedWithModerationLogThrowsConflict() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài bị từ chối", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Rejected");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
            when(moderationLogRepository.existsByLesson_LessonId(100L)).thenReturn(true);

            assertThatThrownBy(() -> creatorLessonService.deleteMyLesson(100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Không thể xóa bài học đã có lịch sử kiểm duyệt");
                    });

            verify(lessonRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("6. Add & Remove Vocabulary Tests")
    class AddRemoveVocabularyTests {

        @Test
        @DisplayName("GIVEN existing vocabulary WHEN addVocabularyToLesson THEN appends with next orderIndex")
        void testAddVocabularyToLesson_success() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài 1", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Draft");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
            when(lessonVocabularyRepository.existsByLesson_LessonIdAndVocabulary_VocabId(100L, 10L)).thenReturn(false);
            when(vocabularyRepository.findByIdWithLock(10L)).thenReturn(Optional.of(vocab1));
            when(lessonVocabularyRepository.countByLesson_LessonId(100L)).thenReturn(2L);

            LessonVocabulary lv = new LessonVocabulary(lesson, vocab1, 3);
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of(lv));

            LessonDetailResponse response = creatorLessonService.addVocabularyToLesson(100L, 10L);

            assertThat(response).isNotNull();
            verify(lessonVocabularyRepository).save(any(LessonVocabulary.class));
        }

        @Test
        @DisplayName("GIVEN already existing vocab in lesson WHEN addVocabularyToLesson THEN throws CONFLICT")
        void testAddVocabularyToLesson_alreadyExistsThrowsConflict() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài 1", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Draft");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
            when(lessonVocabularyRepository.existsByLesson_LessonIdAndVocabulary_VocabId(100L, 10L)).thenReturn(true);

            assertThatThrownBy(() -> creatorLessonService.addVocabularyToLesson(100L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }

        @Test
        @DisplayName("GIVEN existing association WHEN removeVocabularyFromLesson THEN deletes and re-indexes remaining")
        void testRemoveVocabularyFromLesson_success() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài 1", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Draft");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

            LessonVocabulary lv1 = new LessonVocabulary(lesson, vocab1, 1);
            LessonVocabulary lv2 = new LessonVocabulary(lesson, vocab2, 2);

            when(lessonVocabularyRepository.findById(new LessonVocabularyId(100L, 10L))).thenReturn(Optional.of(lv1));
            when(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(100L)).thenReturn(List.of(lv2));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of(lv2));

            LessonDetailResponse response = creatorLessonService.removeVocabularyFromLesson(100L, 10L);

            assertThat(response).isNotNull();
            verify(lessonVocabularyRepository).delete(lv1);
            verify(lessonVocabularyRepository).saveAll(any());
        }
    }

    @Nested
    @DisplayName("7. Reorder Vocabulary Tests")
    class ReorderVocabularyTests {

        @Test
        @DisplayName("GIVEN valid ReorderVocabRequest WHEN reorderVocabulary THEN applies two-phase update and returns ordered list")
        void testReorderVocabulary_success() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài 1", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Draft");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

            LessonVocabulary lv1 = new LessonVocabulary(lesson, vocab1, 1);
            LessonVocabulary lv2 = new LessonVocabulary(lesson, vocab2, 2);
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L))
                    .thenReturn(List.of(lv1, lv2));

            // Swap order: vocab 20 first, vocab 10 second
            ReorderVocabRequest request = ReorderVocabRequest.of(List.of(20L, 10L));
            LessonDetailResponse response = creatorLessonService.reorderVocabulary(100L, request);

            assertThat(response).isNotNull();
            verify(entityManager, org.mockito.Mockito.atLeast(2)).flush();
        }

        @Test
        @DisplayName("GIVEN ReorderVocabRequest with duplicate IDs WHEN reorderVocabulary THEN throws BAD_REQUEST")
        void testReorderVocabulary_duplicateIds() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài 1", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Draft");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

            ReorderVocabRequest request = ReorderVocabRequest.of(List.of(10L, 10L));

            assertThatThrownBy(() -> creatorLessonService.reorderVocabulary(100L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("GIVEN ReorderVocabRequest with vocabulary not in lesson WHEN reorderVocabulary THEN throws BAD_REQUEST")
        void testReorderVocabulary_vocabNotInLesson() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài 1", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Draft");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

            LessonVocabulary lv1 = new LessonVocabulary(lesson, vocab1, 1);
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of(lv1));

            ReorderVocabRequest request = ReorderVocabRequest.of(List.of(999L));

            assertThatThrownBy(() -> creatorLessonService.reorderVocabulary(100L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }
    }

    @Nested
    @DisplayName("8. Submit for Moderation Tests")
    class SubmitForModerationTests {

        @Test
        @DisplayName("GIVEN Draft lesson with vocabularies WHEN submitForModeration THEN status transitions to Pending")
        void testSubmitForModeration_draftSuccess() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài 1", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Draft");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
            when(lessonVocabularyRepository.countByLesson_LessonId(100L)).thenReturn(2L);
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of());

            LessonDetailResponse response = creatorLessonService.submitForModeration(100L);

            assertThat(response.getStatus()).isEqualTo("Pending");
        }

        @Test
        @DisplayName("GIVEN Rejected lesson with vocabularies WHEN submitForModeration THEN transitions back to Pending")
        void testSubmitForModeration_rejectedSuccess() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài sửa lại", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Rejected");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
            when(lessonVocabularyRepository.countByLesson_LessonId(100L)).thenReturn(1L);
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of());

            LessonDetailResponse response = creatorLessonService.submitForModeration(100L);

            assertThat(response.getStatus()).isEqualTo("Pending");
        }

        @Test
        @DisplayName("GIVEN already Pending lesson WHEN submitForModeration THEN throws CONFLICT")
        void testSubmitForModeration_alreadyPending() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài đang chờ duyệt", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Pending");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

            assertThatThrownBy(() -> creatorLessonService.submitForModeration(100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }

        @Test
        @DisplayName("GIVEN already Approved lesson WHEN submitForModeration THEN throws CONFLICT")
        void testSubmitForModeration_alreadyApproved() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài đã duyệt", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Approved");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));

            assertThatThrownBy(() -> creatorLessonService.submitForModeration(100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }

        @Test
        @DisplayName("GIVEN empty lesson without vocabularies WHEN submitForModeration THEN throws BAD_REQUEST")
        void testSubmitForModeration_emptyLessonThrowsBadRequest() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            Lesson lesson = new Lesson("Bài rỗng", creatorA);
            lesson.setLessonId(100L);
            lesson.setStatus("Draft");
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
            when(lessonVocabularyRepository.countByLesson_LessonId(100L)).thenReturn(0L);

            assertThatThrownBy(() -> creatorLessonService.submitForModeration(100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }
    }

    @Nested
    @DisplayName("importLessonFromExcel Tests")
    class ImportLessonFromExcelTests {

        @Test
        @DisplayName("GIVEN blank title WHEN importLessonFromExcel THEN throws BAD_REQUEST")
        void testImport_blankTitleThrowsBadRequest() {
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> creatorLessonService.importLessonFromExcel("   ", file))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("GIVEN oversized title (>100 chars) WHEN importLessonFromExcel THEN throws BAD_REQUEST")
        void testImport_oversizedTitleThrowsBadRequest() {
            String longTitle = "A".repeat(101);
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> creatorLessonService.importLessonFromExcel(longTitle, file))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("GIVEN null or empty file WHEN importLessonFromExcel THEN throws BAD_REQUEST")
        void testImport_emptyFileThrowsBadRequest() {
            MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

            assertThatThrownBy(() -> creatorLessonService.importLessonFromExcel("Bài học mới", emptyFile))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("GIVEN invalid excel report (isValid=false) WHEN importLessonFromExcel THEN throws UNPROCESSABLE_ENTITY")
        void testImport_invalidReportThrowsUnprocessableEntity() {
            MockMultipartFile file = new MockMultipartFile("file", "invalid.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
            ImportValidationReport report = ImportValidationReport.fileLevelError("INVALID", "Có lỗi dữ liệu", "VALIDATION_ERROR");

            when(excelParserService.parseAndValidate(file)).thenReturn(report);

            assertThatThrownBy(() -> creatorLessonService.importLessonFromExcel("Bài học mới", file))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("GIVEN valid file with new and existing words WHEN importLessonFromExcel THEN persists Lesson, Vocabularies, and links atomically")
        void testImport_success() {
            when(accountRepository.findByEmailOrPhone("creator_a@example.com")).thenReturn(Optional.of(creatorA));

            MockMultipartFile file = new MockMultipartFile("file", "vocab.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

            ImportValidationReport report = new ImportValidationReport();
            report.setIsValid(true);
            report.setTotalRows(2);

            ParsedVocabularyItem item1 = new ParsedVocabularyItem(2, "学", "xué", "xue", "Học", "Học tập", null, null, true, 10L, true);
            ParsedVocabularyItem item2 = new ParsedVocabularyItem(3, "生", "shēng", "sheng", "Sinh", "Học sinh", null, null, false, null, true);
            report.addRow(item1);
            report.addRow(item2);

            when(excelParserService.parseAndValidate(file)).thenReturn(report);

            Lesson savedLesson = new Lesson("Bài 1: Làm quen", creatorA);
            savedLesson.setLessonId(50L);
            savedLesson.setStatus("Draft");
            when(lessonRepository.save(any(Lesson.class))).thenReturn(savedLesson);

            Vocabulary existingVocab = new Vocabulary("学", "xué", "xue", "Học", "Học tập");
            existingVocab.setVocabId(10L);
            when(vocabularyRepository.findById(10L)).thenReturn(Optional.of(existingVocab));

            Vocabulary newVocab = new Vocabulary("生", "shēng", "sheng", "Sinh", "Học sinh");
            newVocab.setVocabId(20L);
            when(vocabularyRepository.findByHanziAndPinyinRaw("生", "sheng")).thenReturn(Optional.empty());
            when(vocabularyRepository.save(any(Vocabulary.class))).thenReturn(newVocab);

            LessonVocabulary lv1 = new LessonVocabulary(savedLesson, existingVocab, 1);
            LessonVocabulary lv2 = new LessonVocabulary(savedLesson, newVocab, 2);
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(50L)).thenReturn(List.of(lv1, lv2));

            LessonDetailResponse response = creatorLessonService.importLessonFromExcel("Bài 1: Làm quen", file);

            assertThat(response).isNotNull();
            assertThat(response.getLessonId()).isEqualTo(50L);
            assertThat(response.getTitle()).isEqualTo("Bài 1: Làm quen");
            assertThat(response.getVocabularies()).hasSize(2);
            assertThat(response.getVocabularies().get(0).getOrderIndex()).isEqualTo(1);
            assertThat(response.getVocabularies().get(1).getOrderIndex()).isEqualTo(2);

            verify(lessonRepository).save(any(Lesson.class));
            verify(vocabularyRepository).save(any(Vocabulary.class));
            verify(lessonVocabularyRepository, org.mockito.Mockito.times(2)).save(any(LessonVocabulary.class));
        }
    }
}
