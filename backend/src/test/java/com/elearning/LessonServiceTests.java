package com.elearning;

import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Vocabulary;
import com.elearning.common.ErrorCode;
import com.elearning.exception.BusinessException;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.service.impl.LessonServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LessonService Unit Tests (LessonServiceImpl with Mockito)")
class LessonServiceTests {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonVocabularyRepository lessonVocabularyRepository;

    @InjectMocks
    private LessonServiceImpl lessonService;

    @Nested
    @DisplayName("1. getApprovedLessons Tests")
    class GetApprovedLessonsTests {

        @Test
        @DisplayName("GIVEN valid Pageable WHEN getApprovedLessons THEN delegates to repository and returns Page")
        void testGetApprovedLessons_withPageable() {
            LocalDateTime now = LocalDateTime.now();
            List<LessonSummaryResponse> content = List.of(
                    new LessonSummaryResponse(1L, "Bài 1", "Approved", 5, now, now),
                    new LessonSummaryResponse(2L, "Bài 2", "Approved", 10, now, now)
            );
            Page<LessonSummaryResponse> expectedPage = new PageImpl<>(content, PageRequest.of(0, 10), 2);

            when(lessonRepository.findApprovedLessonSummaries(any(Pageable.class))).thenReturn(expectedPage);

            Page<LessonSummaryResponse> actualPage = lessonService.getApprovedLessons(PageRequest.of(0, 10));

            assertThat(actualPage).isNotNull();
            assertThat(actualPage.getTotalElements()).isEqualTo(2);
            assertThat(actualPage.getContent()).hasSize(2);
            assertThat(actualPage.getContent().get(0).getTitle()).isEqualTo("Bài 1");
            assertThat(actualPage.getContent().get(0).getVocabularyCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("GIVEN null Pageable WHEN getApprovedLessons THEN defaults to PageRequest.of(0, 20)")
        void testGetApprovedLessons_nullPageable() {
            Page<LessonSummaryResponse> emptyPage = new PageImpl<>(List.of());
            when(lessonRepository.findApprovedLessonSummaries(any(Pageable.class))).thenReturn(emptyPage);

            lessonService.getApprovedLessons(null);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(lessonRepository).findApprovedLessonSummaries(captor.capture());

            Pageable captured = captor.getValue();
            assertThat(captured.getPageNumber()).isEqualTo(0);
            assertThat(captured.getPageSize()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("2. getApprovedLessonById Tests")
    class GetApprovedLessonByIdTests {

        @Test
        @DisplayName("GIVEN existing Approved lesson WHEN getApprovedLessonById THEN returns complete LessonDetailResponse with ordered vocabularies")
        void testGetApprovedLessonById_success() {
            Lesson lesson = new Lesson("Bài 1 - Gia đình", new Account());
            lesson.setLessonId(10L);
            lesson.setStatus("Approved");

            Vocabulary v1 = new Vocabulary("爸爸", "bàba", "baba", "Bà ba", "Bố, cha");
            v1.setVocabId(1L);
            Vocabulary v2 = new Vocabulary("妈妈", "māma", "mama", "Ma ma", "Mẹ");
            v2.setVocabId(2L);

            List<LessonVocabulary> vocabList = List.of(
                    new LessonVocabulary(lesson, v1, 1),
                    new LessonVocabulary(lesson, v2, 2)
            );

            when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(10L)).thenReturn(vocabList);

            LessonDetailResponse detail = lessonService.getApprovedLessonById(10L);

            assertThat(detail).isNotNull();
            assertThat(detail.getLessonId()).isEqualTo(10L);
            assertThat(detail.getTitle()).isEqualTo("Bài 1 - Gia đình");
            assertThat(detail.getStatus()).isEqualTo("Approved");
            assertThat(detail.getVocabularyCount()).isEqualTo(2);
            assertThat(detail.getVocabularies()).hasSize(2);
            assertThat(detail.getVocabularies().get(0).getHanzi()).isEqualTo("爸爸");
            assertThat(detail.getVocabularies().get(0).getOrderIndex()).isEqualTo(1);
            assertThat(detail.getVocabularies().get(1).getHanzi()).isEqualTo("妈妈");
            assertThat(detail.getVocabularies().get(1).getOrderIndex()).isEqualTo(2);
        }

        @Test
        @DisplayName("GIVEN null lessonId WHEN getApprovedLessonById THEN throws BusinessException(NOT_FOUND)")
        void testGetApprovedLessonById_nullId() {
            assertThatThrownBy(() -> lessonService.getApprovedLessonById(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND)
                    .hasMessageContaining("ID bài học không được để trống");
        }

        @Test
        @DisplayName("GIVEN nonexistent lessonId WHEN getApprovedLessonById THEN throws BusinessException(NOT_FOUND)")
        void testGetApprovedLessonById_notFound() {
            when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.getApprovedLessonById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND)
                    .hasMessageContaining("Không tìm thấy bài học với ID: 999");
        }

        @Test
        @DisplayName("GIVEN lesson with status 'Draft' WHEN getApprovedLessonById THEN throws BusinessException(NOT_FOUND) (Checkpoint 5A)")
        void testGetApprovedLessonById_draftStatusHidden() {
            Lesson draftLesson = new Lesson("Bài nháp", new Account());
            draftLesson.setLessonId(20L);
            draftLesson.setStatus("Draft");

            when(lessonRepository.findById(20L)).thenReturn(Optional.of(draftLesson));

            assertThatThrownBy(() -> lessonService.getApprovedLessonById(20L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND)
                    .hasMessageContaining("Không tìm thấy bài học với ID: 20");
        }

        @Test
        @DisplayName("GIVEN lesson with status 'Pending' WHEN getApprovedLessonById THEN throws BusinessException(NOT_FOUND) (Checkpoint 5A)")
        void testGetApprovedLessonById_pendingStatusHidden() {
            Lesson pendingLesson = new Lesson("Bài chờ duyệt", new Account());
            pendingLesson.setLessonId(21L);
            pendingLesson.setStatus("Pending");

            when(lessonRepository.findById(21L)).thenReturn(Optional.of(pendingLesson));

            assertThatThrownBy(() -> lessonService.getApprovedLessonById(21L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND)
                    .hasMessageContaining("Không tìm thấy bài học với ID: 21");
        }

        @Test
        @DisplayName("GIVEN lesson with status 'Rejected' WHEN getApprovedLessonById THEN throws BusinessException(NOT_FOUND) (Checkpoint 5A)")
        void testGetApprovedLessonById_rejectedStatusHidden() {
            Lesson rejectedLesson = new Lesson("Bài bị từ chối", new Account());
            rejectedLesson.setLessonId(22L);
            rejectedLesson.setStatus("Rejected");

            when(lessonRepository.findById(22L)).thenReturn(Optional.of(rejectedLesson));

            assertThatThrownBy(() -> lessonService.getApprovedLessonById(22L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND)
                    .hasMessageContaining("Không tìm thấy bài học với ID: 22");
        }

        @Test
        @DisplayName("GIVEN vocabularies returned out of order (3, 1, 2) WHEN getApprovedLessonById THEN enforces strict ascending order (1, 2, 3)")
        void testGetApprovedLessonById_enforcesOrderIndexAscending() {
            Lesson lesson = new Lesson("Bài 2", new Account());
            lesson.setLessonId(30L);
            lesson.setStatus("Approved");

            Vocabulary v1 = new Vocabulary("一", "yī", "yi", "Nhất", "Một");
            Vocabulary v2 = new Vocabulary("二", "èr", "er", "Nhị", "Hai");
            Vocabulary v3 = new Vocabulary("三", "sān", "san", "Tam", "Ba");

            List<LessonVocabulary> outOfOrder = new ArrayList<>();
            outOfOrder.add(new LessonVocabulary(lesson, v3, 3));
            outOfOrder.add(new LessonVocabulary(lesson, v1, 1));
            outOfOrder.add(new LessonVocabulary(lesson, v2, 2));

            when(lessonRepository.findById(30L)).thenReturn(Optional.of(lesson));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(30L)).thenReturn(outOfOrder);

            LessonDetailResponse detail = lessonService.getApprovedLessonById(30L);

            assertThat(detail.getVocabularies()).hasSize(3);
            assertThat(detail.getVocabularies().get(0).getOrderIndex()).isEqualTo(1);
            assertThat(detail.getVocabularies().get(0).getHanzi()).isEqualTo("一");

            assertThat(detail.getVocabularies().get(1).getOrderIndex()).isEqualTo(2);
            assertThat(detail.getVocabularies().get(1).getHanzi()).isEqualTo("二");

            assertThat(detail.getVocabularies().get(2).getOrderIndex()).isEqualTo(3);
            assertThat(detail.getVocabularies().get(2).getHanzi()).isEqualTo("三");
        }
    }
}
