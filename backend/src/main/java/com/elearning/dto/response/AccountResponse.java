package com.elearning.dto.response;

import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Public response DTO for account management in Admin domain.
 * Sanitized view: excludes passwordHash and internal authorizationVersion.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse {

    private Long accountId;
    private String emailOrPhone;
    private String fullName;
    private String status;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AccountResponse() {
    }

    public AccountResponse(Long accountId, String emailOrPhone, String fullName, String status, List<String> roles, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.accountId = accountId;
        this.emailOrPhone = emailOrPhone;
        this.fullName = fullName;
        this.status = status;
        this.roles = roles != null ? roles : Collections.emptyList();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AccountResponse fromEntity(Account account) {
        if (account == null) {
            return null;
        }
        String fullName = (account.getUserProfile() != null) ? account.getUserProfile().getFullName() : null;
        List<String> roles = (account.getRoles() != null)
                ? account.getRoles().stream().map(Role::getRoleName).sorted().toList()
                : Collections.emptyList();

        return new AccountResponse(
                account.getAccountId(),
                account.getEmailOrPhone(),
                fullName,
                account.getStatus(),
                roles,
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
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
}
