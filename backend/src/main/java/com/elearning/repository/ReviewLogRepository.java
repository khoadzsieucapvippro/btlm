package com.elearning.repository;

import com.elearning.entity.ReviewLog;
import com.elearning.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    List<ReviewLog> findByUserOrderByReviewedAtDesc(UserProfile user);

    Page<ReviewLog> findByUserOrderByReviewedAtDesc(UserProfile user, Pageable pageable);

    List<ReviewLog> findByUserAndItemTypeAndItemIdOrderByReviewedAtDesc(UserProfile user, String itemType, Long itemId);
}
