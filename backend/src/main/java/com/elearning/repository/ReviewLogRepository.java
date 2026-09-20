package com.elearning.repository;

import com.elearning.entity.ReviewLog;
import com.elearning.entity.UserProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    List<ReviewLog> findByUserOrderByReviewedAtDesc(UserProfile user);

    Page<ReviewLog> findByUserOrderByReviewedAtDesc(UserProfile user, Pageable pageable);

    List<ReviewLog> findByUserAndItemTypeAndItemIdOrderByReviewedAtDesc(UserProfile user, String itemType, Long itemId);

    long countByUserAndReviewedAtGreaterThanEqual(UserProfile user, LocalDateTime startOfDay);

    long countByUserAndItemTypeAndReviewedAtGreaterThanEqual(UserProfile user, String itemType, LocalDateTime startOfDay);

    /**
     * Finds list of ReviewLog IDs for the user created on or after startOfDay with pessimistic write lock (SELECT ... FOR UPDATE).
     * Used in reviewCard() to bypass MySQL REPEATABLE READ transaction snapshot and read the authoritative latest count (BE-CONC-002).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r.logId FROM ReviewLog r WHERE r.user = :user AND r.reviewedAt >= :startOfDay")
    List<Long> findTodayLogIdsWithLock(@Param("user") UserProfile user, @Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Counts the number of unique items (item_id) first introduced today by the user.
     * An item is defined as first introduced today if it has at least one ReviewLog on or after startOfDay
     * and zero ReviewLog records before startOfDay (R1-DEC-04).
     *
     * @param user the user profile
     * @param itemType the item type filter (e.g. "VOCABULARY")
     * @param startOfDay start of the current business day
     * @return count of unique items first reviewed today
     */
    @Query("SELECT COUNT(DISTINCT r.itemId) FROM ReviewLog r " +
           "WHERE r.user = :user " +
           "  AND r.itemType = :itemType " +
           "  AND r.reviewedAt >= :startOfDay " +
           "  AND NOT EXISTS (" +
           "      SELECT 1 FROM ReviewLog r2 " +
           "      WHERE r2.user = :user " +
           "        AND r2.itemType = :itemType " +
           "        AND r2.itemId = r.itemId " +
           "        AND r2.reviewedAt < :startOfDay" +
           "  )")
    long countNewItemsIntroducedToday(
            @Param("user") UserProfile user,
            @Param("itemType") String itemType,
            @Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Finds list of ReviewLog entries for the user created on or after startOfDay with pessimistic write lock (SELECT ... FOR UPDATE).
     * Used in reviewCard() to bypass MySQL REPEATABLE READ transaction snapshot and read authoritative latest logs (BE-CONC-002).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReviewLog r WHERE r.user = :user AND r.reviewedAt >= :startOfDay")
    List<ReviewLog> findTodayReviewLogsWithLock(@Param("user") UserProfile user, @Param("startOfDay") LocalDateTime startOfDay);

    boolean existsByUserAndItemTypeAndItemIdAndReviewedAtLessThan(UserProfile user, String itemType, Long itemId, LocalDateTime startOfDay);

    /**
     * Checks whether any review log entries exist for the given polymorphic item pair (R3.1).
     * Used during vocabulary deletion to prevent orphaning immutable study history.
     *
     * @param itemType polymorphic item type (e.g. "VOCABULARY")
     * @param itemId the item primary key ID
     * @return true if review logs exist, false otherwise
     */
    boolean existsByItemTypeAndItemId(String itemType, Long itemId);
}
