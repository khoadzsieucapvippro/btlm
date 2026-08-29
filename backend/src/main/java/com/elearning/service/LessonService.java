package com.elearning.service;

import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Public Lesson Exploration domain (Module 5A).
 * Provides public discovery of approved lessons and detailed lesson content.
 * Enforces security invariant: ONLY lessons with status 'Approved' are discoverable.
 * Conforms to .agents/API.md section 2.3 and zero-entity-leak rules.
 */
public interface LessonService {

    /**
     * Retrieves a paginated list of approved public lessons.
     * Lessons with status Draft, Pending, or Rejected are strictly excluded.
     *
     * @param pageable pagination parameters
     * @return Page of {@link LessonSummaryResponse}
     */
    Page<LessonSummaryResponse> getApprovedLessons(Pageable pageable);

    /**
     * Retrieves detailed information of an approved public lesson by its ID,
     * including its constituent vocabulary items strictly sorted by order_index ascending.
     * If the lesson does not exist or its status is not 'Approved',
     * throws {@link com.elearning.exception.BusinessException} with ErrorCode.NOT_FOUND.
     *
     * @param lessonId the primary ID of the lesson
     * @return {@link LessonDetailResponse}
     * @throws com.elearning.exception.BusinessException if lesson is null, not found, or not approved
     */
    LessonDetailResponse getApprovedLessonById(Long lessonId);

    /**
     * Retrieves a paginated list of lessons across all lifecycle statuses for administrative oversight (Module 8D.4).
     *
     * @param status   optional status filter (Draft, Pending, Approved, Rejected)
     * @param pageable pagination parameters
     * @return Page of {@link LessonSummaryResponse}
     */
    Page<LessonSummaryResponse> getAdminLessons(String status, Pageable pageable);
}
