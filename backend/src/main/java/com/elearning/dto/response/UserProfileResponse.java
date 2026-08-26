package com.elearning.dto.response;

import com.elearning.entity.UserProfile;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Response DTO returning user profile data (/api/v1/users/profile).
 * Conforms to .agents/API.md and Roadmap Task 3C.1.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private Long userId;
    private Long accountId;
    private String emailOrPhone;
    private String fullName;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long userId, Long accountId, String emailOrPhone, String fullName, String avatarUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.accountId = accountId;
        this.emailOrPhone = emailOrPhone;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserProfileResponse fromEntity(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        Long accountId = profile.getAccount() != null ? profile.getAccount().getAccountId() : null;
        String emailOrPhone = profile.getAccount() != null ? profile.getAccount().getEmailOrPhone() : null;

        return new UserProfileResponse(
                profile.getUserId(),
                accountId,
                emailOrPhone,
                profile.getFullName(),
                profile.getAvatarUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public void setEmailOrPhone(String emailOrPhone) {
        this.emailOrPhone = emailOrPhone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UserProfileResponse{" +
                "userId=" + userId +
                ", accountId=" + accountId +
                ", emailOrPhone='" + emailOrPhone + '\'' +
                ", fullName='" + fullName + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
