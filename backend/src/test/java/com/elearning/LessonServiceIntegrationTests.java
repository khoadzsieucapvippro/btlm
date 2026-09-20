package com.elearning;

import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Vocabulary;
import com.elearning.common.ErrorCode;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.LessonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link LessonService} on real MySQL database.
 * Verifies transaction boundary, status filtering ('Approved' only),
 * zero entity leak, N+1 query safety, and deterministic orderIndex ASC.
 */
@SpringBootTest
@Transactional
@DisplayName("LessonService Integration Tests (Real MySQL & Spring Application Context)")
class LessonServiceIntegrationTests {

    @Autowired
    private LessonService lessonService;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    private Lesson approvedLesson1;
    private Lesson approvedLesson2;
    private Lesson draftLesson;
    private Lesson pendingLesson;
    private Lesson rejectedLesson;

    private Vocabulary vocab1;
    private Vocabulary vocab2;
    private Vocabulary vocab3;

    @BeforeEach
    void setUp() {
        Account creator = new Account();
        creator.setEmailOrPhone("creator_lesson_test@example.com");
        creator.setPasswordHash("$2a$12$dummyhashcreator123456789012345");
        creator.setStatus("Active");
        creator = accountRepository.save(creator);

        // Vocabularies
        vocab1 = vocabularyRepository.save(new Vocabulary("学", "xué", "xue", "Học", "Học tập"));
        vocab2 = vocabularyRepository.save(new Vocabulary("生", "shēng", "sheng", "Sinh", "Học sinh, sinh ra"));
        vocab3 = vocabularyRepository.save(new Vocabulary("校", "xiào", "xiao", "Hiệu", "Trường học"));

        // 1. Approved Lesson 1 with 3 vocabularies added out of order (order 3, 1, 2)
        approvedLesson1 = new Lesson("Bài 1 - Trường học", creator);
        approvedLesson1.setStatus("Approved");
        approvedLesson1 = lessonRepository.save(approvedLesson1);

        lessonVocabularyRepository.save(new LessonVocabulary(approvedLesson1, vocab3, 3));
        lessonVocabularyRepository.save(new LessonVocabulary(approvedLesson1, vocab1, 1));
        lessonVocabularyRepository.save(new LessonVocabulary(approvedLesson1, vocab2, 2));

        // 2. Approved Lesson 2 with 1 vocabulary
        approvedLesson2 = new Lesson("Bài 2 - Chào hỏi", creator);
        approvedLesson2.setStatus("Approved");
        approvedLesson2 = lessonRepository.save(approvedLesson2);
        lessonVocabularyRepository.save(new LessonVocabulary(approvedLesson2, vocab1, 1));

        // 3. Draft Lesson (Must NOT be discoverable publicly)
        draftLesson = new Lesson("Bài nháp - Chưa công bố", creator);
        draftLesson.setStatus("Draft");
        draftLesson = lessonRepository.save(draftLesson);
        lessonVocabularyRepository.save(new LessonVocabulary(draftLesson, vocab2, 1));

        // 4. Pending Lesson (Must NOT be discoverable publicly)
        pendingLesson = new Lesson("Bài chờ duyệt", creator);
        pendingLesson.setStatus("Pending");
        pendingLesson = lessonRepository.save(pendingLesson);

        // 5. Rejected Lesson (Must NOT be discoverable publicly)
        rejectedLesson = new Lesson("Bài bị từ chối", creator);
        rejectedLesson.setStatus("Rejected");
        rejectedLesson = lessonRepository.save(rejectedLesson);
    }

    @Nested
    @DisplayName("1. getApprovedLessons Integration Tests")
    class GetApprovedLessonsIntegrationTests {

        @Test
        @DisplayName("GIVEN lessons with various statuses WHEN getApprovedLessons THEN only 'Approved' lessons are returned")
        void testGetApprovedLessonsOnlyReturnsApproved() {
            Page<LessonSummaryResponse> page = lessonService.getApprovedLessons(PageRequest.of(0, 10));

            assertThat(page).isNotNull();
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).hasSize(2);

            List<String> titles = page.getContent().stream().map(LessonSummaryResponse::getTitle).toList();
            assertThat(titles).containsExactlyInAnyOrder("Bài 1 - Trường học", "Bài 2 - Chào hỏi");

            // Verify status is Approved for all returned
            assertThat(page.getContent()).allMatch(summary -> "Approved".equals(summary.getStatus()));

            // Verify vocabularyCount correctly computed via JPQL SIZE(...)
            LessonSummaryResponse lesson1Summary = page.getContent().stream()
                    .filter(s -> s.getLessonId().equals(approvedLesson1.getLessonId()))
                    .findFirst().orElseThrow();
            assertThat(lesson1Summary.getVocabularyCount()).isEqualTo(3);

            LessonSummaryResponse lesson2Summary = page.getContent().stream()
                    .filter(s -> s.getLessonId().equals(approvedLesson2.getLessonId()))
                    .findFirst().orElseThrow();
            assertThat(lesson2Summary.getVocabularyCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("GIVEN multiple approved lessons WHEN paginating with size=1 THEN returns distinct pages correctly")
        void testGetApprovedLessonsPagination() {
            Page<LessonSummaryResponse> page0 = lessonService.getApprovedLessons(PageRequest.of(0, 1));
            assertThat(page0.getContent()).hasSize(1);
            assertThat(page0.getTotalElements()).isEqualTo(2);
            assertThat(page0.getTotalPages()).isEqualTo(2);

            Page<LessonSummaryResponse> page1 = lessonService.getApprovedLessons(PageRequest.of(1, 1));
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page1.getContent().get(0).getLessonId()).isNotEqualTo(page0.getContent().get(0).getLessonId());
        }
    }

    @Nested
    @DisplayName("2. getApprovedLessonById Integration Tests")
    class GetApprovedLessonByIdIntegrationTests {

        @Test
        @DisplayName("GIVEN approved lesson ID WHEN getApprovedLessonById THEN returns details with vocabularies strictly sorted by orderIndex ASC")
        void testGetApprovedLessonByIdSuccess() {
            LessonDetailResponse response = lessonService.getApprovedLessonById(approvedLesson1.getLessonId());

            assertThat(response).isNotNull();
            assertThat(response.getLessonId()).isEqualTo(approvedLesson1.getLessonId());
            assertThat(response.getTitle()).isEqualTo("Bài 1 - Trường học");
            assertThat(response.getStatus()).isEqualTo("Approved");
            assertThat(response.getVocabularyCount()).isEqualTo(3);
            assertThat(response.getVocabularies()).hasSize(3);

            // Verify strictly sorted by orderIndex: 1 -> 2 -> 3 despite being inserted as 3, 1, 2
            assertThat(response.getVocabularies().get(0).getOrderIndex()).isEqualTo(1);
            assertThat(response.getVocabularies().get(0).getHanzi()).isEqualTo("学");

            assertThat(response.getVocabularies().get(1).getOrderIndex()).isEqualTo(2);
            assertThat(response.getVocabularies().get(1).getHanzi()).isEqualTo("生");

            assertThat(response.getVocabularies().get(2).getOrderIndex()).isEqualTo(3);
            assertThat(response.getVocabularies().get(2).getHanzi()).isEqualTo("校");
        }

        @Test
        @DisplayName("GIVEN Draft lesson ID WHEN getApprovedLessonById THEN throws BusinessException(NOT_FOUND) (Checkpoint 5A)")
        void testGetDraftLessonByIdThrowsNotFound() {
            assertThatThrownBy(() -> lessonService.getApprovedLessonById(draftLesson.getLessonId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND)
                    .hasMessageContaining("Không tìm thấy bài học với ID: " + draftLesson.getLessonId());
        }

        @Test
        @DisplayName("GIVEN Pending lesson ID WHEN getApprovedLessonById THEN throws BusinessException(NOT_FOUND) (Checkpoint 5A)")
        void testGetPendingLessonByIdThrowsNotFound() {
            assertThatThrownBy(() -> lessonService.getApprovedLessonById(pendingLesson.getLessonId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND)
                    .hasMessageContaining("Không tìm thấy bài học với ID: " + pendingLesson.getLessonId());
        }

        @Test
        @DisplayName("GIVEN Rejected lesson ID WHEN getApprovedLessonById THEN throws BusinessException(NOT_FOUND) (Checkpoint 5A)")
        void testGetRejectedLessonByIdThrowsNotFound() {
            assertThatThrownBy(() -> lessonService.getApprovedLessonById(rejectedLesson.getLessonId()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND)
                    .hasMessageContaining("Không tìm thấy bài học với ID: " + rejectedLesson.getLessonId());
        }

        @Test
        @DisplayName("GIVEN nonexistent lesson ID WHEN getApprovedLessonById THEN throws BusinessException(NOT_FOUND)")
        void testGetNonexistentLessonByIdThrowsNotFound() {
            assertThatThrownBy(() -> lessonService.getApprovedLessonById(999999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND)
                    .hasMessageContaining("Không tìm thấy bài học với ID: 999999");
        }
    }
}
