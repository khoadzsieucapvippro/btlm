package com.elearning;

import com.elearning.controller.AdminLessonController;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.service.LessonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 8D.4: AdminLessonController Unit Contract Tests")
class AdminLessonControllerTests {

    @Mock
    private LessonService lessonService;

    @InjectMocks
    private AdminLessonController adminLessonController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminLessonController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GIVEN lessons exist across statuses WHEN calling GET /api/v1/admin/lessons THEN returns 200 with PageResponse")
    void testGetAdminLessons_success() throws Exception {
        LessonSummaryResponse l1 = new LessonSummaryResponse(1L, "Lesson Draft", "Draft", 5, LocalDateTime.now(), LocalDateTime.now());
        LessonSummaryResponse l2 = new LessonSummaryResponse(2L, "Lesson Pending", "Pending", 8, LocalDateTime.now(), LocalDateTime.now());
        Page<LessonSummaryResponse> page = new PageImpl<>(List.of(l1, l2), PageRequest.of(0, 10), 2);

        when(lessonService.getAdminLessons(eq("Draft"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/lessons")
                        .param("status", "Draft")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].title").value("Lesson Draft"))
                .andExpect(jsonPath("$.data.items[0].status").value("Draft"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }
}
