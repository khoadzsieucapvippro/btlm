package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.CreateLessonRequest;
import com.elearning.dto.request.ReorderVocabRequest;
import com.elearning.dto.request.UpdateLessonRequest;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.CreatorLessonService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("Task 5B.1: CreatorLessonService Real Database Integration Tests")
class CreatorLessonServiceIntegrationTests {

    @Autowired
    private CreatorLessonService creatorLessonService;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Account creatorA;
    private Account creatorB;
    private Vocabulary vocab1;
    private Vocabulary vocab2;
    private Vocabulary vocab3;

    @BeforeEach
    void setUp() {
        creatorA = new Account();
        creatorA.setEmailOrPhone("creator_a_integ@example.com");
        creatorA.setPasswordHash("$2a$12$dummyhashcreatorA123456789012345");
        creatorA.setStatus("Active");
        creatorA = accountRepository.save(creatorA);

        creatorB = new Account();
        creatorB.setEmailOrPhone("creator_b_integ@example.com");
        creatorB.setPasswordHash("$2a$12$dummyhashcreatorB123456789012345");
        creatorB.setStatus("Active");
        creatorB = accountRepository.save(creatorB);

        vocab1 = vocabularyRepository.save(new Vocabulary("学", "xué", "xue", "Học", "Học tập"));
        vocab2 = vocabularyRepository.save(new Vocabulary("生", "shēng", "sheng", "Sinh", "Học sinh"));
        vocab3 = vocabularyRepository.save(new Vocabulary("校", "xiào", "xiao", "Hiệu", "Trường học"));

        setAuthentication("creator_a_integ@example.com", "ROLE_CREATOR");
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

    @Test
    @DisplayName("GIVEN CreateLessonRequest with vocabularies WHEN createLesson THEN persists in MySQL with Draft status and ordered items")
    void testCreateLesson_integrationSuccess() {
        CreateLessonRequest request = new CreateLessonRequest(
                "Bài Học Tích Hợp",
                "http://files.example.com/sheet.xlsx",
                List.of(vocab1.getVocabId(), vocab2.getVocabId())
        );

        LessonDetailResponse response = creatorLessonService.createLesson(request);

        assertThat(response).isNotNull();
        assertThat(response.getLessonId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("Draft");
        assertThat(response.getVocabularyCount()).isEqualTo(2);
        assertThat(response.getVocabularies().get(0).getOrderIndex()).isEqualTo(1);
        assertThat(response.getVocabularies().get(1).getOrderIndex()).isEqualTo(2);

        // Verify direct DB query
        Lesson saved = lessonRepository.findById(response.getLessonId()).orElseThrow();
        assertThat(saved.getCreatedBy().getAccountId()).isEqualTo(creatorA.getAccountId());
    }

    @Test
    @DisplayName("GIVEN lessons from multiple creators WHEN getMyLessons THEN returns only Creator A's lessons")
    void testGetMyLessons_creatorIsolation() {
        // Create lesson for A
        Lesson lessonA = new Lesson("Bài của A", creatorA);
        lessonA.setStatus("Draft");
        lessonRepository.save(lessonA);

        // Create lesson for B
        Lesson lessonB = new Lesson("Bài của B", creatorB);
        lessonB.setStatus("Draft");
        lessonRepository.save(lessonB);

        Page<LessonSummaryResponse> page = creatorLessonService.getMyLessons(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Bài của A");
    }

    @Test
    @DisplayName("GIVEN Creator B's lesson WHEN Creator A tries to get/update/delete THEN throws FORBIDDEN (Checkpoint 5B)")
    void testOwnershipSecurity_forbiddenOnOtherCreatorsLesson() {
        Lesson lessonB = new Lesson("Bài của B", creatorB);
        lessonB.setStatus("Draft");
        lessonB = lessonRepository.save(lessonB);
        Long lessonId = lessonB.getLessonId();

        // Currently authenticated as creator A
        assertThatThrownBy(() -> creatorLessonService.getMyLessonById(lessonId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> creatorLessonService.updateMyLesson(lessonId, new UpdateLessonRequest("Hack")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> creatorLessonService.deleteMyLesson(lessonId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        assertThatThrownBy(() -> creatorLessonService.submitForModeration(lessonId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("GIVEN Creator B's lesson WHEN Admin accesses it THEN bypasses ownership and succeeds")
    void testAdminBypass_ownershipOverride() {
        Lesson lessonB = new Lesson("Bài của B", creatorB);
        lessonB.setStatus("Draft");
        lessonB = lessonRepository.save(lessonB);

        setAuthentication("admin_integ@example.com", "ROLE_ADMIN");
        Account admin = new Account();
        admin.setEmailOrPhone("admin_integ@example.com");
        admin.setPasswordHash("$2a$12$dummyhashadmin12345678901234567");
        admin.setStatus("Active");
        accountRepository.save(admin);

        LessonDetailResponse response = creatorLessonService.getMyLessonById(lessonB.getLessonId());
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Bài của B");
    }

    @Test
    @DisplayName("GIVEN Draft lesson with 2 vocabularies WHEN reordered and swapped THEN succeeds without MySQL UK collision")
    void testReorderVocabulary_swappingWithoutUniqueConstraintViolation() {
        Lesson lesson = new Lesson("Bài Reorder", creatorA);
        lesson.setStatus("Draft");
        lesson = lessonRepository.save(lesson);

        lessonVocabularyRepository.save(new LessonVocabulary(lesson, vocab1, 1));
        lessonVocabularyRepository.save(new LessonVocabulary(lesson, vocab2, 2));

        // Swap order: vocab2 first (1), vocab1 second (2)
        ReorderVocabRequest request = ReorderVocabRequest.of(List.of(vocab2.getVocabId(), vocab1.getVocabId()));
        LessonDetailResponse response = creatorLessonService.reorderVocabulary(lesson.getLessonId(), request);

        assertThat(response.getVocabularies().get(0).getVocabId()).isEqualTo(vocab2.getVocabId());
        assertThat(response.getVocabularies().get(0).getOrderIndex()).isEqualTo(1);
        assertThat(response.getVocabularies().get(1).getVocabId()).isEqualTo(vocab1.getVocabId());
        assertThat(response.getVocabularies().get(1).getOrderIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("GIVEN Draft lesson with items WHEN deleteMyLesson THEN cascades join table but leaves vocabularies intact")
    void testDeleteMyLesson_cascadesJoinRowsOnly() {
        Lesson lesson = new Lesson("Bài Cần Xóa", creatorA);
        lesson.setStatus("Draft");
        lesson = lessonRepository.save(lesson);

        lessonVocabularyRepository.save(new LessonVocabulary(lesson, vocab1, 1));
        lessonVocabularyRepository.save(new LessonVocabulary(lesson, vocab2, 2));

        Long lessonId = lesson.getLessonId();
        creatorLessonService.deleteMyLesson(lessonId);

        assertThat(lessonRepository.findById(lessonId)).isEmpty();
        assertThat(lessonVocabularyRepository.countByLesson_LessonId(lessonId)).isEqualTo(0);
        // Ensure Vocabulary records are NOT deleted
        assertThat(vocabularyRepository.findById(vocab1.getVocabId())).isPresent();
        assertThat(vocabularyRepository.findById(vocab2.getVocabId())).isPresent();
    }

    @Test
    @DisplayName("GIVEN Draft lesson with items WHEN submitForModeration THEN transitions to Pending")
    void testSubmitForModeration_transitionLifecycle() {
        Lesson lesson = new Lesson("Bài Chuẩn Bị Duyệt", creatorA);
        lesson.setStatus("Draft");
        lesson = lessonRepository.save(lesson);

        lessonVocabularyRepository.save(new LessonVocabulary(lesson, vocab1, 1));

        LessonDetailResponse response = creatorLessonService.submitForModeration(lesson.getLessonId());

        assertThat(response.getStatus()).isEqualTo("Pending");
        Lesson inDb = lessonRepository.findById(lesson.getLessonId()).orElseThrow();
        assertThat(inDb.getStatus()).isEqualTo("Pending");

        Long lessonId = lesson.getLessonId();
        // Attempting to submit again should fail with CONFLICT
        assertThatThrownBy(() -> creatorLessonService.submitForModeration(lessonId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }
}
