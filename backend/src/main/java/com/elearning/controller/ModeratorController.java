package com.elearning.controller;

import com.elearning.dto.request.ApproveLessonRequest;
import com.elearning.dto.request.RejectLessonRequest;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.ModerationLogResponse;
import com.elearning.dto.response.ModerationQueueResponse;
import com.elearning.dto.response.PageResponse;
import com.elearning.service.ModerationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * REST Controller for Content Moderation and Moderation History (Module 6B & 8D.3).
 * Base path: /api/v1/moderator
 * Protected by Spring Security: Requires ROLE_MODERATOR or ROLE_ADMIN.
 * Enforces thin-controller architecture; delegates queue retrieval,
 * approval/rejection state machine transitions, and audit logging to {@link ModerationService}.
 */
@RestController
@RequestMapping("/api/v1/moderator")
public class ModeratorController {

    private final ModerationService moderationService;

    public ModeratorController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /**
     * Retrieves paginated queue of lessons currently awaiting moderation ('Pending').
     *
     * @param pageable pagination parameters (page, size)
     * @return ApiResponse wrapping PageResponse of ModerationQueueResponse
     */
    @GetMapping("/lessons/pending")
    public ResponseEntity<ApiResponse<PageResponse<ModerationQueueResponse>>> getPendingLessons(Pageable pageable) {
        Page<ModerationQueueResponse> page = moderationService.getPendingLessons(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    /**
     * Retrieves full detail of a specific pending lesson for review,
     * including its constituent vocabulary items sorted by orderIndex.
     *
     * @param id the lesson primary ID
     * @return ApiResponse wrapping LessonDetailResponse
     */
    @GetMapping("/lessons/{id}")
    public ResponseEntity<ApiResponse<LessonDetailResponse>> getPendingLessonById(@PathVariable("id") Long id) {
        LessonDetailResponse response = moderationService.getPendingLessonById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Approves a pending lesson, transitioning its status to 'Approved'
     * and recording an immutable audit trail in MODERATION_LOG.
     *
     * @param id      the lesson primary ID
     * @param request optional approval request payload
     * @return ApiResponse wrapping LessonDetailResponse
     */
    @PostMapping("/lessons/{id}/approve")
    public ResponseEntity<ApiResponse<LessonDetailResponse>> approveLesson(
            @PathVariable("id") Long id,
            @Valid @RequestBody(required = false) ApproveLessonRequest request) {
        ApproveLessonRequest req = (request != null) ? request : new ApproveLessonRequest();
        LessonDetailResponse response = moderationService.approveLesson(id, req);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt bài học thành công", response));
    }

    /**
     * Rejects a pending lesson, transitioning its status to 'Rejected'
     * and recording feedback reason, flagged fields, and audit trail in MODERATION_LOG.
     *
     * @param id      the lesson primary ID
     * @param request rejection payload containing non-blank rejection reason and optional flagged fields
     * @return ApiResponse wrapping LessonDetailResponse
     */
    @PostMapping("/lessons/{id}/reject")
    public ResponseEntity<ApiResponse<LessonDetailResponse>> rejectLesson(
            @PathVariable("id") Long id,
            @Valid @RequestBody RejectLessonRequest request) {
        LessonDetailResponse response = moderationService.rejectLesson(id, request);
        return ResponseEntity.ok(ApiResponse.success("Từ chối bài học thành công", response));
    }

    /**
     * Retrieves paginated moderation history (Module 8D.3).
     * Returns personal history for ROLE_MODERATOR and global history for ROLE_ADMIN.
     *
     * @param pageable  pagination parameters
     * @param principal authenticated user principal
     * @return ApiResponse wrapping PageResponse of ModerationLogResponse
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<ModerationLogResponse>>> getModeratorHistory(
            Pageable pageable,
            Principal principal) {
        String callerEmailOrPhone = (principal != null) ? principal.getName() : null;
        PageResponse<ModerationLogResponse> response = moderationService.getModeratorHistory(callerEmailOrPhone, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
