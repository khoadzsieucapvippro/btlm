package com.elearning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity mapping the CARD_PROGRESS table.
 * Tracks spaced repetition system (SRS) learning progress for a user on a flashcard item.
 * Uses a business polymorphic reference pair (item_type, item_id) to reference either
 * a Vocabulary or a Radical without physical foreign keys at the database level.
 */
@Entity
@Table(name = "card_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_card_progress_user_item", columnNames = {"user_id", "item_type", "item_id"})
})
public class CardProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Long progressId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactor = new BigDecimal("2.50");

    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays = 0;

    @Column(name = "repetitions", nullable = false)
    private Integer repetitions = 0;

    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;

    public CardProgress() {
    }

    public CardProgress(UserProfile user, String itemType, Long itemId, BigDecimal easeFactor, Integer intervalDays, Integer repetitions, LocalDateTime nextReviewAt) {
        this.user = user;
        this.itemType = itemType;
        this.itemId = itemId;
        this.easeFactor = easeFactor;
        this.intervalDays = intervalDays;
        this.repetitions = repetitions;
        this.nextReviewAt = nextReviewAt;
    }

    public Long getProgressId() {
        return progressId;
    }

    public void setProgressId(Long progressId) {
        this.progressId = progressId;
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

    public BigDecimal getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(BigDecimal easeFactor) {
        this.easeFactor = easeFactor;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public Integer getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(Integer repetitions) {
        this.repetitions = repetitions;
    }

    public LocalDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(LocalDateTime nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CardProgress that)) return false;
        return progressId != null && progressId.equals(that.progressId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
