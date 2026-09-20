package com.elearning.service;

import com.elearning.dto.request.CreateLessonRequest;
import com.elearning.dto.request.ReorderVocabRequest;
import com.elearning.dto.request.UpdateLessonRequest;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Creator Lesson Studio domain (Module 5B).
 * Supports lesson authoring, vocabulary management, reordering, and submission for moderation.
 * Strictly enforces Creator ownership isolation (Creator A cannot modify Creator B's lesson).
 * Conforms to .agents/API.md section 2.4 and zero-entity-leak rules.
 */
public interface CreatorLessonService {

    /**
     * Creates a new lesson owned by the current authenticated creator.
     * Default lifecycle status is 'Draft'.
     *
     * @param request lesson creation request
     * @return {@link LessonDetailResponse} of the created lesson
     */
    LessonDetailResponse createLesson(CreateLessonRequest request);

    /**
     * Retrieves paginated list of lessons created by the current authenticated creator.
     *
     * @param pageable pagination parameters
     * @return Page of {@link LessonSummaryResponse}
     */
    Page<LessonSummaryResponse> getMyLessons(Pageable pageable);

    /**
     * Retrieves details of a lesson owned by the current authenticated creator.
     * Throws 403 FORBIDDEN if the lesson belongs to another creator.
     * Throws 404 NOT_FOUND if the lesson does not exist.
     *
     * @param lessonId the primary ID of the lesson
     * @return {@link LessonDetailResponse}
     */
    LessonDetailResponse getMyLessonById(Long lessonId);

    /**
     * Updates an existing lesson (title, excelFileUrl) owned by the creator.
     * Allowed only for lessons in 'Draft' or 'Rejected' state.
     *
     * @param lessonId the primary ID of the lesson
     * @param request  lesson update request
     * @return updated {@link LessonDetailResponse}
     */
    LessonDetailResponse updateMyLesson(Long lessonId, UpdateLessonRequest request);

    /**
     * Deletes a lesson owned by the creator.
     * Allowed only for lessons in 'Draft' or 'Rejected' state.
     * Cascades deletion of associations in LESSON_VOCABULARY without deleting Vocabulary records.
     *
     * @param lessonId the primary ID of the lesson
     */
    void deleteMyLesson(Long lessonId);

    /**
     * Adds a vocabulary to a lesson owned by the creator.
     * Appends to the end with order_index = count + 1.
     * Allowed only for lessons in 'Draft' or 'Rejected' state.
     *
     * @param lessonId the primary ID of the lesson
     * @param vocabId  the primary ID of the vocabulary to add
     * @return updated {@link LessonDetailResponse}
     */
    LessonDetailResponse addVocabularyToLesson(Long lessonId, Long vocabId);

    /**
     * Removes a vocabulary from a lesson owned by the creator.
     * Re-indexes remaining items to maintain continuous 1, 2, 3... order_index.
     * Allowed only for lessons in 'Draft' or 'Rejected' state.
     *
     * @param lessonId the primary ID of the lesson
     * @param vocabId  the primary ID of the vocabulary to remove
     * @return updated {@link LessonDetailResponse}
     */
    LessonDetailResponse removeVocabularyFromLesson(Long lessonId, Long vocabId);

    /**
     * Reorders the vocabulary items within a lesson.
     * Atomically updates order_index strictly preserving lesson membership.
     * Allowed only for lessons in 'Draft' or 'Rejected' state.
     *
     * @param lessonId the primary ID of the lesson
     * @param request  reorder request containing ordered IDs or items
     * @return updated {@link LessonDetailResponse}
     */
    LessonDetailResponse reorderVocabulary(Long lessonId, ReorderVocabRequest request);

    /**
     * Submits a lesson for moderation queue:
     * - 'Draft' -> 'Pending'
     * - 'Rejected' -> 'Pending'
     * Rejects submission if status is already 'Pending' or 'Approved', or if lesson has no vocabulary.
     *
     * @param lessonId the primary ID of the lesson
     * @return updated {@link LessonDetailResponse}
     */
    LessonDetailResponse submitForModeration(Long lessonId);

    /**
     * Imports a lesson and its vocabulary items from an uploaded Excel file (.xlsx)
     * during Step 2 (Confirm) of the Two-Step Excel Import Engine.
     * Fully atomic / transactional: creates the Lesson, persists new Vocabulary items,
     * links existing/new Vocabulary to Lesson via LESSON_VOCABULARY preserving order_index.
     * Rolls back completely if file is invalid or any persistence step fails.
     *
     * @param title the title for the new lesson
     * @param file  the uploaded .xlsx file to parse and persist
     * @return {@link LessonDetailResponse} of the created lesson with constituent vocabularies
     */
    LessonDetailResponse importLessonFromExcel(String title, org.springframework.web.multipart.MultipartFile file);
}
