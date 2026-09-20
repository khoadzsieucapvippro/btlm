package com.elearning.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity mapping the ACCOUNT table.
 * Manages user credentials, account lifecycle status, 1:1 profile, and N:N roles.
 */
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "email_or_phone", nullable = false, length = 191, unique = true)
    private String emailOrPhone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "Active";

    @Column(name = "authorization_version", nullable = false)
    private Long authorizationVersion = 1L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserProfile userProfile;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "account_role",
            joinColumns = @JoinColumn(name = "account_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "role_id", nullable = false)
    )
    private Set<Role> roles = new HashSet<>();

    public Account() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = "Active";
        }
        if (this.authorizationVersion == null) {
            this.authorizationVersion = 1L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
        if (userProfile != null) {
            userProfile.setAccount(this);
        }
    }

    public void addRole(Role role) {
        if (role != null) {
            this.roles.add(role);
        }
    }

    public void removeRole(Role role) {
        if (role != null) {
            this.roles.remove(role);
        }
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAuthorizationVersion() {
        return authorizationVersion;
    }

    public void setAuthorizationVersion(Long authorizationVersion) {
        this.authorizationVersion = authorizationVersion;
    }

    public void incrementAuthorizationVersion() {
        if (this.authorizationVersion == null) {
            this.authorizationVersion = 1L;
        }
        this.authorizationVersion++;
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

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? roles : new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return accountId != null && accountId.equals(account.accountId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", emailOrPhone='" + emailOrPhone + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
