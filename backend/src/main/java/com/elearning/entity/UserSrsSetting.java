package com.elearning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA Entity mapping the USER_SRS_SETTING table.
 * Represents personalized Spaced Repetition System (SRS) review parameters for a learner.
 * Maintains a 1:1 relationship with UserProfile.
 */
@Entity
@Table(name = "user_srs_setting", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_srs_setting_user", columnNames = {"user_id"})
})
public class UserSrsSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserProfile user;

    @Column(name = "new_cards_per_day", nullable = false)
    private Integer newCardsPerDay = 20;

    @Column(name = "max_review_per_day", nullable = false)
    private Integer maxReviewPerDay = 100;

    public UserSrsSetting() {
    }

    public UserSrsSetting(UserProfile user, Integer newCardsPerDay, Integer maxReviewPerDay) {
        this.user = user;
        this.newCardsPerDay = newCardsPerDay;
        this.maxReviewPerDay = maxReviewPerDay;
    }

    public Long getSettingId() {
        return settingId;
    }

    public void setSettingId(Long settingId) {
        this.settingId = settingId;
    }

    public UserProfile getUser() {
        return user;
    }

    public void setUser(UserProfile user) {
        this.user = user;
    }

    public Integer getNewCardsPerDay() {
        return newCardsPerDay;
    }

    public void setNewCardsPerDay(Integer newCardsPerDay) {
        this.newCardsPerDay = newCardsPerDay;
    }

    public Integer getMaxReviewPerDay() {
        return maxReviewPerDay;
    }

    public void setMaxReviewPerDay(Integer maxReviewPerDay) {
        this.maxReviewPerDay = maxReviewPerDay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSrsSetting that)) return false;
        return settingId != null && settingId.equals(that.settingId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
