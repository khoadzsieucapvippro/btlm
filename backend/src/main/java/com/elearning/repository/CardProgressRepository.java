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

    Optional<CardProgress> findByUserAndItemTypeAndItemId(UserProfile user, String itemType, Long itemId);

    long countByUserAndNextReviewAtLessThanEqual(UserProfile user, LocalDateTime now);
}
