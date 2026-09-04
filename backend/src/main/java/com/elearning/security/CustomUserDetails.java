package com.elearning.security;

import com.elearning.entity.Account;
import com.elearning.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Security principal representing an authenticated or loaded user in Spring Security.
 * Encapsulates core user account credentials, identifier, status, and granted authorities.
 * Decoupled from the JPA Entity graph to prevent LazyInitializationException and unintended leaks.
 */
public class CustomUserDetails implements UserDetails {

    private final Long accountId;
    private final String emailOrPhone;
    private final String passwordHash;
    private final String status;
    private final Long authorizationVersion;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(
            Long accountId,
            String emailOrPhone,
            String passwordHash,
            String status,
            Collection<? extends GrantedAuthority> authorities) {
        this(accountId, emailOrPhone, passwordHash, status, 1L, authorities);
    }

    public CustomUserDetails(
            Long accountId,
            String emailOrPhone,
            String passwordHash,
            String status,
            Long authorizationVersion,
            Collection<? extends GrantedAuthority> authorities) {
        this.accountId = accountId;
        this.emailOrPhone = emailOrPhone;
        this.passwordHash = passwordHash;
        this.status = status;
        this.authorizationVersion = (authorizationVersion != null) ? authorizationVersion : 1L;
        this.authorities = (authorities != null)
                ? Collections.unmodifiableSet(Set.copyOf(authorities))
                : Collections.emptySet();
    }

    /**
     * Factory method creating CustomUserDetails from a JPA Account entity.
     * Maps each assigned Role to a GrantedAuthority with standard "ROLE_" prefix.
     *
     * @param account JPA Account entity
     * @return populated CustomUserDetails instance
     */
    public static CustomUserDetails fromAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account entity cannot be null");
        }

        Set<GrantedAuthority> grantedAuthorities = (account.getRoles() != null)
                ? account.getRoles().stream()
                        .filter(Objects::nonNull)
                        .map(Role::getRoleName)
                        .filter(Objects::nonNull)
                        .map(roleName -> roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toUnmodifiableSet())
                : Collections.emptySet();

        return new CustomUserDetails(
                account.getAccountId(),
                account.getEmailOrPhone(),
                account.getPasswordHash(),
                account.getStatus(),
                account.getAuthorizationVersion() != null ? account.getAuthorizationVersion() : 1L,
                grantedAuthorities
        );
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public String getStatus() {
        return status;
    }

    public Long getAuthorizationVersion() {
        return authorizationVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return emailOrPhone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"Banned".equalsIgnoreCase(status) && !"Locked".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "Active".equalsIgnoreCase(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomUserDetails that)) return false;
        return Objects.equals(accountId, that.accountId) && Objects.equals(emailOrPhone, that.emailOrPhone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, emailOrPhone);
    }

    @Override
    public String toString() {
        return "CustomUserDetails{" +
                "accountId=" + accountId +
                ", emailOrPhone='" + emailOrPhone + '\'' +
                ", status='" + status + '\'' +
                ", authorities=" + authorities +
                '}';
    }
}
