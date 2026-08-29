package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.controller.ModeratorController;
import com.elearning.dto.request.ApproveLessonRequest;
import com.elearning.dto.request.RejectLessonRequest;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonVocabItemResponse;
import com.elearning.dto.response.ModerationQueueResponse;
import com.elearning.exception.BusinessException;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.service.ModerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 6B.1: ModeratorController Unit Contract Tests")
class ModeratorControllerTests {

    @Mock
    private ModerationService moderationService;

    @InjectMocks
    private ModeratorController moderatorController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(moderatorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("GET /api/v1/moderator/lessons/pending Tests")
    class GetPendingLessonsTests {

        @Test
        @DisplayName("GIVEN pending lessons exist WHEN calling GET pending THEN returns 200 with PageResponse")
        void testGetPendingLessons_success() throws Exception {
            ModerationQueueResponse item1 = new ModerationQueueResponse(100L, "Bài Học HSK 1", "Pending", 1L, "creator1@example.com", 5, LocalDateTime.now(), LocalDateTime.now());
            ModerationQueueResponse item2 = new ModerationQueueResponse(101L, "Bài Học HSK 2", "Pending", 2L, "creator2@example.com", 8, LocalDateTime.now(), LocalDateTime.now());
            Page<ModerationQueueResponse> page = new PageImpl<>(List.of(item1, item2), PageRequest.of(0, 10), 2);

            when(moderationService.getPendingLessons(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.items", hasSize(2)))
                    .andExpect(jsonPath("$.data.items[0].lessonId", is(100)))
                    .andExpect(jsonPath("$.data.items[0].title", is("Bài Học HSK 1")))
                    .andExpect(jsonPath("$.data.items[0].creatorEmail", is("creator1@example.com")))
                    .andExpect(jsonPath("$.data.items[0].vocabularyCount", is(5)))
                    .andExpect(jsonPath("$.data.items[1].lessonId", is(101)))
                    .andExpect(jsonPath("$.data.totalElements", is(2)));

            verify(moderationService).getPendingLessons(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/moderator/lessons/{id} Tests")
    class GetPendingLessonByIdTests {

        @Test
        @DisplayName("GIVEN existing pending lesson WHEN calling GET by id THEN returns 200 with LessonDetailResponse")
        void testGetPendingLessonById_success() throws Exception {
            LessonVocabItemResponse v1 = new LessonVocabItemResponse(1L, "学", "xué", "xue", "Học", "Học tập", null, null, null, null, 1);
            LessonDetailResponse detail = new LessonDetailResponse(100L, "Bài Học HSK 1", "Pending", 1, List.of(v1), LocalDateTime.now(), LocalDateTime.now());

            when(moderationService.getPendingLessonById(100L)).thenReturn(detail);

            mockMvc.perform(get("/api/v1/moderator/lessons/{id}", 100L)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.lessonId", is(100)))
                    .andExpect(jsonPath("$.data.title", is("Bài Học HSK 1")))
                    .andExpect(jsonPath("$.data.status", is("Pending")))
                    .andExpect(jsonPath("$.data.vocabularies", hasSize(1)))
                    .andExpect(jsonPath("$.data.vocabularies[0].hanzi", is("学")));

            verify(moderationService).getPendingLessonById(100L);
        }

        @Test
        @DisplayName("GIVEN nonexistent lesson ID WHEN calling GET by id THEN returns 404 NOT_FOUND")
        void testGetPendingLessonById_notFound() throws Exception {
            when(moderationService.getPendingLessonById(999L))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: 999"));

            mockMvc.perform(get("/api/v1/moderator/lessons/{id}", 999L)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.message", is("Không tìm thấy bài học với ID: 999")));

            verify(moderationService).getPendingLessonById(999L);
        }

        @Test
        @DisplayName("GIVEN non-Pending lesson WHEN calling GET by id THEN returns 404 NOT_FOUND (BE-AUTHZ-001)")
        void testGetPendingLessonById_nonPending_returnsNotFound() throws Exception {
            when(moderationService.getPendingLessonById(101L))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: 101"));

            mockMvc.perform(get("/api/v1/moderator/lessons/{id}", 101L)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.message", is("Không tìm thấy bài học với ID: 101")));

            verify(moderationService).getPendingLessonById(101L);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/moderator/lessons/{id}/approve Tests")
    class ApproveLessonTests {

        @Test
        @DisplayName("GIVEN pending lesson WHEN calling POST approve THEN returns 200 with approved detail")
        void testApproveLesson_success() throws Exception {
            LessonDetailResponse detail = new LessonDetailResponse(100L, "Bài Học HSK 1", "Approved", 0, List.of(), LocalDateTime.now(), LocalDateTime.now());

            when(moderationService.approveLesson(eq(100L), any(ApproveLessonRequest.class))).thenReturn(detail);

            ApproveLessonRequest request = new ApproveLessonRequest("Nội dung đạt chuẩn");

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.message", is("Phê duyệt bài học thành công")))
                    .andExpect(jsonPath("$.data.lessonId", is(100)))
                    .andExpect(jsonPath("$.data.status", is("Approved")));

            verify(moderationService).approveLesson(eq(100L), any(ApproveLessonRequest.class));
        }

        @Test
        @DisplayName("GIVEN non-pending lesson WHEN calling POST approve THEN returns 409 CONFLICT")
        void testApproveLesson_conflict() throws Exception {
            when(moderationService.approveLesson(eq(101L), any(ApproveLessonRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "Không thể phê duyệt bài học đang ở trạng thái Draft"));

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", 101L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")))
                    .andExpect(jsonPath("$.message", is("Không thể phê duyệt bài học đang ở trạng thái Draft")));

            verify(moderationService).approveLesson(eq(101L), any(ApproveLessonRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/moderator/lessons/{id}/reject Tests")
    class RejectLessonTests {

        @Test
        @DisplayName("GIVEN valid reject request WHEN calling POST reject THEN returns 200 with rejected detail")
        void testRejectLesson_success() throws Exception {
            LessonDetailResponse detail = new LessonDetailResponse(100L, "Bài Học HSK 1", "Rejected", 0, List.of(), LocalDateTime.now(), LocalDateTime.now());

            when(moderationService.rejectLesson(eq(100L), any(RejectLessonRequest.class))).thenReturn(detail);

            RejectLessonRequest request = new RejectLessonRequest("Pinyin từ vựng số 1 sai", "[\"pinyin\"]");

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.message", is("Từ chối bài học thành công")))
                    .andExpect(jsonPath("$.data.lessonId", is(100)))
                    .andExpect(jsonPath("$.data.status", is("Rejected")));

            verify(moderationService).rejectLesson(eq(100L), any(RejectLessonRequest.class));
        }

        @Test
        @DisplayName("GIVEN blank rejectionReason WHEN calling POST reject THEN returns 400 VALIDATION_ERROR and does NOT call service")
        void testRejectLesson_blankReason_validationError() throws Exception {
            RejectLessonRequest invalidRequest = new RejectLessonRequest("   ", null);

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            verify(moderationService, never()).rejectLesson(any(), any());
        }

        @Test
        @DisplayName("GIVEN null rejectionReason WHEN calling POST reject THEN returns 400 VALIDATION_ERROR and does NOT call service")
        void testRejectLesson_nullReason_validationError() throws Exception {
            RejectLessonRequest invalidRequest = new RejectLessonRequest(null, null);

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            verify(moderationService, never()).rejectLesson(any(), any());
        }

        @Test
        @DisplayName("GIVEN concurrent moderation conflict WHEN calling POST approve THEN returns 409 CONFLICT (BE-CONC-003)")
        void testApproveLesson_concurrentConflict_returns409() throws Exception {
            when(moderationService.approveLesson(eq(100L), any(ApproveLessonRequest.class)))
                    .thenThrow(new org.springframework.dao.OptimisticLockingFailureException("Row was updated or deleted by another transaction"));

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ApproveLessonRequest()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")))
                    .andExpect(jsonPath("$.message", is("Dữ liệu đã bị thay đổi bởi một phiên làm việc khác, vui lòng thử lại")));
        }

        @Test
        @DisplayName("GIVEN concurrent moderation conflict WHEN calling POST reject THEN returns 409 CONFLICT (BE-CONC-003)")
        void testRejectLesson_concurrentConflict_returns409() throws Exception {
            when(moderationService.rejectLesson(eq(100L), any(RejectLessonRequest.class)))
                    .thenThrow(new org.springframework.dao.OptimisticLockingFailureException("Row was updated or deleted by another transaction"));

            RejectLessonRequest request = new RejectLessonRequest("Lý do từ chối", null);

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", 100L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")))
                    .andExpect(jsonPath("$.message", is("Dữ liệu đã bị thay đổi bởi một phiên làm việc khác, vui lòng thử lại")));
        }
    }
}
