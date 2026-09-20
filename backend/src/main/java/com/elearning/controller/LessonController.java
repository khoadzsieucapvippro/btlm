package com.elearning.controller;

import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.dto.response.PageResponse;
import com.elearning.service.LessonService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST Controller for Public Lesson Exploration (Module 5A).
 * Base path: /api/v1/lessons
 * Publicly accessible without authentication.
 * Endpoints:
 * - GET /api/v1/lessons: Paginated list of approved public lessons
 * - GET /api/v1/lessons/{id}: Detailed view of an approved lesson with ordered vocabularies
 */
@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    /**
     * Retrieves a paginated list of approved public lessons.
     * Lessons with status 'Draft', 'Pending', or 'Rejected' are strictly excluded.
     *
     * @param pageable pagination parameters (page, size)
     * @return ApiResponse wrapping PageResponse of LessonSummaryResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LessonSummaryResponse>>> getApprovedLessons(Pageable pageable) {
        Page<LessonSummaryResponse> page = lessonService.getApprovedLessons(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    /**
     * Retrieves detailed information of an approved public lesson by ID,
     * including its constituent vocabulary items sorted by order_index ascending.
     * Non-approved or nonexistent lessons return 404 NOT_FOUND.
     *
     * @param id the lesson primary ID
     * @return ApiResponse wrapping LessonDetailResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LessonDetailResponse>> getApprovedLessonById(@PathVariable("id") Long id) {
        LessonDetailResponse response = lessonService.getApprovedLessonById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
