package com.elearning.dto.response;

import com.elearning.entity.UserSrsSetting;

/**
 * Response DTO representing personalized Spaced Repetition System (SRS) parameters for a learner.
 */
public class UserSrsSettingResponse {

    private Long settingId;
    private Integer newCardsPerDay;
    private Integer maxReviewPerDay;

    public UserSrsSettingResponse() {
    }

    public UserSrsSettingResponse(Long settingId, Integer newCardsPerDay, Integer maxReviewPerDay) {
        this.settingId = settingId;
        this.newCardsPerDay = newCardsPerDay;
        this.maxReviewPerDay = maxReviewPerDay;
    }

    public static UserSrsSettingResponse fromEntity(UserSrsSetting setting) {
        if (setting == null) {
            return new UserSrsSettingResponse(null, 20, 100);
        }
        return new UserSrsSettingResponse(
                setting.getSettingId(),
                setting.getNewCardsPerDay(),
                setting.getMaxReviewPerDay()
        );
    }

    public Long getSettingId() {
        return settingId;
    }

    public void setSettingId(Long settingId) {
        this.settingId = settingId;
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
}
