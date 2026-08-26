package com.elearning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping the REVIEW_LOG table.
 * Historical audit log of spaced repetition review attempts.
 * Uses a business polymorphic reference pair (item_type, item_id) to track reviews for
 * Vocabulary or Radical items without physical foreign keys at the database level.
 */
@Entity
@Table(name = "review_log")
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "rating", nullable = false)
    private Byte rating;

    @Column(name = "interval_before", nullable = false)
    private Integer intervalBefore;

    @Column(name = "interval_after", nullable = false)
    private Integer intervalAfter;

    @Column(name = "review_time_seconds", nullable = false)
    private Integer reviewTimeSeconds = 0;

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private LocalDateTime reviewedAt;

    public ReviewLog() {
    }

    public ReviewLog(UserProfile user, String itemType, Long itemId, Byte rating, Integer intervalBefore, Integer intervalAfter, Integer reviewTimeSeconds) {
        this.user = user;
        this.itemType = itemType;
        this.itemId = itemId;
        this.rating = rating;
        this.intervalBefore = intervalBefore;
        this.intervalAfter = intervalAfter;
        this.reviewTimeSeconds = reviewTimeSeconds;
    }

    public ReviewLog(UserProfile user, String itemType, Long itemId, Integer rating, Integer intervalBefore, Integer intervalAfter, Integer reviewTimeSeconds) {
        this(user, itemType, itemId, rating != null ? rating.byteValue() : null, intervalBefore, intervalAfter, reviewTimeSeconds);
    }

    @PrePersist
    protected void onCreate() {
        if (this.reviewedAt == null) {
            this.reviewedAt = LocalDateTime.now();
        }
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public UserProfile getUser() {
        return user;
    }

    public void setUser(UserProfile user) {
        this.user = user;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Byte getRating() {
        return rating;
    }

    public void setRating(Byte rating) {
        this.rating = rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating != null ? rating.byteValue() : null;
    }

    public Integer getIntervalBefore() {
        return intervalBefore;
    }

    public void setIntervalBefore(Integer intervalBefore) {
        this.intervalBefore = intervalBefore;
    }

    public Integer getIntervalAfter() {
        return intervalAfter;
    }

    public void setIntervalAfter(Integer intervalAfter) {
        this.intervalAfter = intervalAfter;
    }

    public Integer getReviewTimeSeconds() {
        return reviewTimeSeconds;
    }

    public void setReviewTimeSeconds(Integer reviewTimeSeconds) {
        this.reviewTimeSeconds = reviewTimeSeconds;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReviewLog that)) return false;
        return logId != null && logId.equals(that.logId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
