package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.controller.LessonController;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.dto.response.LessonVocabItemResponse;
import com.elearning.exception.BusinessException;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.service.LessonService;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 5A.2: LessonController Unit Contract Tests")
class LessonControllerTests {

    @Mock
    private LessonService lessonService;

    @InjectMocks
    private LessonController lessonController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(lessonController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Nested
    @DisplayName("1. GET /api/v1/lessons (Public Paginated List)")
    class GetApprovedLessonsTests {

        @Test
        @DisplayName("GIVEN approved lessons exist WHEN GET /api/v1/lessons THEN returns 200 OK and PageResponse")
        void testGetApprovedLessons_success() throws Exception {
            LocalDateTime now = LocalDateTime.now();
            List<LessonSummaryResponse> content = List.of(
                    new LessonSummaryResponse(1L, "Bài 1 - Gia đình", "Approved", 5, now, now),
                    new LessonSummaryResponse(2L, "Bài 2 - Trường học", "Approved", 8, now, now)
            );
            Page<LessonSummaryResponse> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);

            when(lessonService.getApprovedLessons(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/lessons")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].lessonId").value(1))
                    .andExpect(jsonPath("$.data.items[0].title").value("Bài 1 - Gia đình"))
                    .andExpect(jsonPath("$.data.items[0].status").value("Approved"))
                    .andExpect(jsonPath("$.data.items[0].vocabularyCount").value(5))
                    .andExpect(jsonPath("$.data.items[1].lessonId").value(2))
                    .andExpect(jsonPath("$.data.items[1].title").value("Bài 2 - Trường học"))
                    .andExpect(jsonPath("$.data.items[1].vocabularyCount").value(8));
        }

        @Test
        @DisplayName("GIVEN explicit page and size query params WHEN GET /api/v1/lessons?page=1&size=5 THEN binds Pageable correctly")
        void testGetApprovedLessons_withQueryParams() throws Exception {
            Page<LessonSummaryResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(1, 5), 12);

            when(lessonService.getApprovedLessons(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/lessons")
                            .param("page", "1")
                            .param("size", "5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.size").value(5))
                    .andExpect(jsonPath("$.data.totalElements").value(12))
                    .andExpect(jsonPath("$.data.totalPages").value(3));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(lessonService).getApprovedLessons(captor.capture());
            Pageable captured = captor.getValue();
            assertThat(captured.getPageNumber()).isEqualTo(1);
            assertThat(captured.getPageSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("GIVEN no approved lessons WHEN GET /api/v1/lessons THEN returns 200 OK with empty items list")
        void testGetApprovedLessons_empty() throws Exception {
            Page<LessonSummaryResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

            when(lessonService.getApprovedLessons(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/lessons")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.items").isEmpty());
        }
    }

    @Nested
    @DisplayName("2. GET /api/v1/lessons/{id} (Public Detail Lookup)")
    class GetApprovedLessonByIdTests {

        @Test
        @DisplayName("GIVEN existing Approved lesson ID WHEN GET /api/v1/lessons/{id} THEN returns 200 OK and ordered vocabularies")
        void testGetApprovedLessonById_success() throws Exception {
            LocalDateTime now = LocalDateTime.now();
            List<LessonVocabItemResponse> vocabList = List.of(
                    new LessonVocabItemResponse(10L, "你", "nǐ", "ni", "Nhĩ", "Bạn",
                            "http://audio/ni.mp3", null, null, null, 1),
                    new LessonVocabItemResponse(20L, "好", "hǎo", "hao", "Hảo", "Tốt",
                            "http://audio/hao.mp3", null, null, null, 2)
            );
            LessonDetailResponse detail = new LessonDetailResponse(1L, "Bài 1 - Chào hỏi", "Approved", 2, vocabList, now, now);

            when(lessonService.getApprovedLessonById(1L)).thenReturn(detail);

            mockMvc.perform(get("/api/v1/lessons/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.lessonId").value(1))
                    .andExpect(jsonPath("$.data.title").value("Bài 1 - Chào hỏi"))
                    .andExpect(jsonPath("$.data.status").value("Approved"))
                    .andExpect(jsonPath("$.data.vocabularyCount").value(2))
                    .andExpect(jsonPath("$.data.vocabularies").isArray())
                    .andExpect(jsonPath("$.data.vocabularies[0].vocabId").value(10))
                    .andExpect(jsonPath("$.data.vocabularies[0].hanzi").value("你"))
                    .andExpect(jsonPath("$.data.vocabularies[0].orderIndex").value(1))
                    .andExpect(jsonPath("$.data.vocabularies[1].vocabId").value(20))
                    .andExpect(jsonPath("$.data.vocabularies[1].hanzi").value("好"))
                    .andExpect(jsonPath("$.data.vocabularies[1].orderIndex").value(2));
        }

        @Test
        @DisplayName("GIVEN nonexistent lesson ID WHEN GET /api/v1/lessons/{id} THEN returns 404 NOT_FOUND")
        void testGetApprovedLessonById_notFound() throws Exception {
            when(lessonService.getApprovedLessonById(999L))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: 999"));

            mockMvc.perform(get("/api/v1/lessons/999")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy bài học với ID: 999"));
        }

        @Test
        @DisplayName("GIVEN Draft lesson ID WHEN GET /api/v1/lessons/{id} THEN returns 404 NOT_FOUND (Checkpoint 5A)")
        void testGetDraftLessonById_hiddenAs404() throws Exception {
            when(lessonService.getApprovedLessonById(20L))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: 20"));

            mockMvc.perform(get("/api/v1/lessons/20")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy bài học với ID: 20"));
        }

        @Test
        @DisplayName("GIVEN Pending lesson ID WHEN GET /api/v1/lessons/{id} THEN returns 404 NOT_FOUND (Checkpoint 5A)")
        void testGetPendingLessonById_hiddenAs404() throws Exception {
            when(lessonService.getApprovedLessonById(21L))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: 21"));

            mockMvc.perform(get("/api/v1/lessons/21")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy bài học với ID: 21"));
        }

        @Test
        @DisplayName("GIVEN Rejected lesson ID WHEN GET /api/v1/lessons/{id} THEN returns 404 NOT_FOUND (Checkpoint 5A)")
        void testGetRejectedLessonById_hiddenAs404() throws Exception {
            when(lessonService.getApprovedLessonById(22L))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: 22"));

            mockMvc.perform(get("/api/v1/lessons/22")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy bài học với ID: 22"));
        }
    }
}
