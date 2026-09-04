package com.elearning.controller;

import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.dto.response.PageResponse;
import com.elearning.service.LessonService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for administrative global lesson oversight (Module 8D.4).
 * Base path: /api/v1/admin/lessons
 * Protected: Requires ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/lessons")
@PreAuthorize("hasAnyRole('Admin', 'ADMIN')")
public class AdminLessonController {

    private final LessonService lessonService;

    public AdminLessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    /**
     * Retrieves paginated lessons across all lifecycle statuses with optional status filtering.
     *
     * @param status   optional status filter (Draft, Pending, Approved, Rejected)
     * @param pageable pagination parameters
     * @return ApiResponse wrapping PageResponse of LessonSummaryResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LessonSummaryResponse>>> getAdminLessons(
            @RequestParam(name = "status", required = false) String status,
            Pageable pageable) {
        Page<LessonSummaryResponse> page = lessonService.getAdminLessons(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }
}
