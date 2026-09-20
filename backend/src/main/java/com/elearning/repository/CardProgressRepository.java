package com.elearning.repository;

import com.elearning.entity.CardProgress;
import com.elearning.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardProgressRepository extends JpaRepository<CardProgress, Long> {

    List<CardProgress> findByUserAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(UserProfile user, LocalDateTime now);

    List<CardProgress> findByUserAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(UserProfile user, LocalDateTime now, org.springframework.data.domain.Pageable pageable);

    List<CardProgress> findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(UserProfile user, String itemType, LocalDateTime now, org.springframework.data.domain.Pageable pageable);

    List<CardProgress> findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(UserProfile user, String itemType, LocalDateTime now);

    Optional<CardProgress> findByUserAndItemTypeAndItemId(UserProfile user, String itemType, Long itemId);

    /**
     * Finds card progress with pessimistic write lock (SELECT ... FOR UPDATE).
     * Bypasses MySQL REPEATABLE READ transaction snapshot during concurrent reviews (BE-CONC-001, BE-CONC-002).
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT cp FROM CardProgress cp WHERE cp.user = :user AND cp.itemType = :itemType AND cp.itemId = :itemId")
    Optional<CardProgress> findByUserAndItemTypeAndItemIdWithLock(
            @org.springframework.data.repository.query.Param("user") UserProfile user,
            @org.springframework.data.repository.query.Param("itemType") String itemType,
            @org.springframework.data.repository.query.Param("itemId") Long itemId);

    long countByUserAndNextReviewAtLessThanEqual(UserProfile user, LocalDateTime now);

    long countByUserAndItemTypeAndNextReviewAtLessThanEqual(UserProfile user, String itemType, LocalDateTime now);

    /**
     * Checks whether any card progress exists for the given polymorphic item pair (R3.1).
     * Used during vocabulary deletion to prevent orphaning active learner SRS state.
     *
     * @param itemType polymorphic item type (e.g. "VOCABULARY")
     * @param itemId the item primary key ID
     * @return true if card progress records exist, false otherwise
     */
    boolean existsByItemTypeAndItemId(String itemType, Long itemId);
}
