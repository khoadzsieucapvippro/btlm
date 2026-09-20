package com.elearning.service;

import com.elearning.dto.request.ApproveLessonRequest;
import com.elearning.dto.request.RejectLessonRequest;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.ModerationQueueResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Content Moderation domain (Phase 6).
 * Manages the moderation workflow for Pending lessons, including queue retrieval,
 * approval (transition to Approved), and rejection (transition to Rejected with feedback).
 * Conforms to .agents/API.md section 2.5 and zero-entity-leak rules.
 */
public interface ModerationService {

    /**
     * Retrieves paginated queue of lessons currently in 'Pending' moderation status.
     *
     * @param pageable pagination parameters
     * @return Page of {@link ModerationQueueResponse}
     */
    Page<ModerationQueueResponse> getPendingLessons(Pageable pageable);

    /**
     * Retrieves full detail of a specific pending lesson for review,
     * including its constituent vocabulary items sorted by order_index ascending.
     * Throws 404 NOT_FOUND if the lesson does not exist.
     *
     * @param lessonId the primary ID of the lesson
     * @return {@link LessonDetailResponse}
     */
    LessonDetailResponse getPendingLessonById(Long lessonId);

    /**
     * Approves a pending lesson, transitioning its status from 'Pending' to 'Approved'.
     *
     * @param lessonId the primary ID of the lesson to approve
     * @param request  approval request payload (optional notes/comments)
     * @return {@link LessonDetailResponse} with updated status 'Approved'
     */
    LessonDetailResponse approveLesson(Long lessonId, ApproveLessonRequest request);

    /**
     * Rejects a pending lesson, transitioning its status from 'Pending' to 'Rejected'.
     * Requires a non-blank rejection reason and optional flagged fields.
     *
     * @param lessonId the primary ID of the lesson to reject
     * @param request  rejection request payload containing rejection reason and flagged fields
     * @return {@link LessonDetailResponse} with updated status 'Rejected'
     */
    LessonDetailResponse rejectLesson(Long lessonId, RejectLessonRequest request);

    /**
     * Retrieves paginated moderation history.
     * If caller is Admin, returns global moderation logs.
     * If caller is Moderator, returns only caller's moderation logs.
     *
     * @param callerEmailOrPhone authenticated caller email or phone
     * @param pageable           pagination parameters
     * @return {@link com.elearning.dto.response.PageResponse} of {@link com.elearning.dto.response.ModerationLogResponse}
     */
    com.elearning.dto.response.PageResponse<com.elearning.dto.response.ModerationLogResponse> getModeratorHistory(
            String callerEmailOrPhone,
            Pageable pageable);
}
