package com.elearning.repository;

import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.dto.response.ModerationQueueResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for {@link Lesson} entity.
 * Supports querying lessons by creator (Account), publication/moderation status,
 * combined creator and status filtering, with optional pagination.
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /**
     * Finds lessons created by a specific account with pagination.
     *
     * @param createdBy the creator account
     * @param pageable pagination parameters
     * @return Page of lessons created by the account
     */
    Page<Lesson> findByCreatedBy(Account createdBy, Pageable pageable);

    /**
     * Finds lessons created by a specific account (unpaged list).
     *
     * @param createdBy the creator account
     * @return List of lessons created by the account
     */
    List<Lesson> findByCreatedBy(Account createdBy);

    /**
     * Finds lessons by lifecycle status with pagination.
     * Supports public catalog ('Approved') and moderator queues ('Pending').
     *
     * @param status lesson status ('Draft', 'Pending', 'Approved', 'Rejected')
     * @param pageable pagination parameters
     * @return Page of lessons matching status
     */
    Page<Lesson> findByStatus(String status, Pageable pageable);

    /**
     * Finds lessons by lifecycle status (unpaged list).
     *
     * @param status lesson status ('Draft', 'Pending', 'Approved', 'Rejected')
     * @return List of lessons matching status
     */
    List<Lesson> findByStatus(String status);

    /**
     * Finds lessons created by a specific account with a specific status and pagination.
     *
     * @param createdBy the creator account
     * @param status lesson status
     * @param pageable pagination parameters
     * @return Page of lessons matching creator and status
     */
    Page<Lesson> findByCreatedByAndStatus(Account createdBy, String status, Pageable pageable);

    /**
     * Finds lessons created by a specific account with a specific status (unpaged list).
     *
     * @param createdBy the creator account
     * @param status lesson status
     * @return List of lessons matching creator and status
     */
    List<Lesson> findByCreatedByAndStatus(Account createdBy, String status);

    /**
     * Finds approved lessons as summary projections with vocabulary count in a single query.
     * Prevents N+1 and collection fetch join pagination warnings.
     *
     * @param pageable pagination parameters
     * @return Page of LessonSummaryResponse
     */
    @Query(value = "SELECT new com.elearning.dto.response.LessonSummaryResponse(" +
                   "l.lessonId, l.title, l.status, SIZE(l.lessonVocabularies), l.createdAt, l.updatedAt) " +
                   "FROM Lesson l WHERE l.status = 'Approved'",
           countQuery = "SELECT COUNT(l) FROM Lesson l WHERE l.status = 'Approved'")
    Page<LessonSummaryResponse> findApprovedLessonSummaries(Pageable pageable);

    /**
     * Finds pending lessons for moderation queue as summary projections with creator info and vocabulary count in a single query.
     * Prevents N+1 and collection fetch join pagination warnings.
     *
     * @param pageable pagination parameters
     * @return Page of ModerationQueueResponse
     */
    @Query(value = "SELECT new com.elearning.dto.response.ModerationQueueResponse(" +
                   "l.lessonId, l.title, l.status, l.createdBy.accountId, l.createdBy.emailOrPhone, SIZE(l.lessonVocabularies), l.createdAt, l.updatedAt) " +
                   "FROM Lesson l WHERE l.status = 'Pending'",
           countQuery = "SELECT COUNT(l) FROM Lesson l WHERE l.status = 'Pending'")
    Page<ModerationQueueResponse> findPendingLessonSummaries(Pageable pageable);

    /**
     * Finds lessons across all lifecycle statuses with optional status filtering for administrative oversight.
     *
     * @param status optional status filter (Draft, Pending, Approved, Rejected)
     * @param pageable pagination parameters
     * @return Page of LessonSummaryResponse
     */
    @Query(value = "SELECT new com.elearning.dto.response.LessonSummaryResponse(" +
                   "l.lessonId, l.title, l.status, SIZE(l.lessonVocabularies), l.createdAt, l.updatedAt) " +
                   "FROM Lesson l WHERE (:status IS NULL OR l.status = :status) ORDER BY l.createdAt DESC",
           countQuery = "SELECT COUNT(l) FROM Lesson l WHERE (:status IS NULL OR l.status = :status)")
    Page<LessonSummaryResponse> findAdminLessonSummaries(
            @org.springframework.data.repository.query.Param("status") String status,
            Pageable pageable);
}
