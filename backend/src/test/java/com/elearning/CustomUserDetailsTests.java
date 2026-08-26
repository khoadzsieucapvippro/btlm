package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CustomUserDetails Unit Tests")
class CustomUserDetailsTests {

    @Nested
    @DisplayName("Principal Properties and Credential Mapping")
    class PrincipalMappingTests {

        @Test
        @DisplayName("GIVEN Account entity WHEN CustomUserDetails created THEN preserves email/phone, password hash, and accountId")
        void testAccountFieldMapping() {
            Account account = new Account();
            account.setEmailOrPhone("student@elearning.com");
            account.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890ABCDEF");
            account.setStatus("Active");
            account.addRole(new Role(1, "Learner"));

            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);

            assertThat(userDetails.getUsername()).isEqualTo("student@elearning.com");
            assertThat(userDetails.getEmailOrPhone()).isEqualTo("student@elearning.com");
            assertThat(userDetails.getPassword()).isEqualTo("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890ABCDEF");
            assertThat(userDetails.getStatus()).isEqualTo("Active");
            assertThat(userDetails.isAccountNonExpired()).isTrue();
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("GIVEN null Account WHEN fromAccount called THEN throws IllegalArgumentException")
        void testNullAccountThrows() {
            assertThatThrownBy(() -> CustomUserDetails.fromAccount(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Role to GrantedAuthority Mapping")
    class RoleToAuthorityTests {

        @Test
        @DisplayName("GIVEN Account with single Role WHEN mapped THEN produces single ROLE_<RoleName> authority")
        void testSingleRoleMapping() {
            Account account = new Account();
            account.setEmailOrPhone("learner@elearning.com");
            account.setPasswordHash("hash123");
            account.addRole(new Role(1, "Learner"));

            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);

            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_Learner");
        }

        @Test
        @DisplayName("GIVEN Account with multiple Roles WHEN mapped THEN produces all corresponding ROLE_<RoleName> authorities without duplicates")
        void testMultipleRolesMapping() {
            Account account = new Account();
            account.setEmailOrPhone("admin@elearning.com");
            account.setPasswordHash("hash123");
            account.addRole(new Role(1, "Learner"));
            account.addRole(new Role(2, "Creator"));
            account.addRole(new Role(4, "Admin"));

            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);

            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_Learner", "ROLE_Creator", "ROLE_Admin");
        }

        @Test
        @DisplayName("GIVEN Role that already has ROLE_ prefix WHEN mapped THEN does not prepend duplicate prefix")
        void testRoleAlreadyHavingPrefix() {
            Account account = new Account();
            account.setEmailOrPhone("moderator@elearning.com");
            account.setPasswordHash("hash123");
            account.addRole(new Role(3, "ROLE_Moderator"));

            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);

            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_Moderator");
        }

        @Test
        @DisplayName("GIVEN Account without any roles WHEN mapped THEN returns empty, non-null, immutable authorities collection")
        void testNoRolesEmptyAuthorities() {
            Account account = new Account();
            account.setEmailOrPhone("noroles@elearning.com");
            account.setPasswordHash("hash123");

            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);

            assertThat(userDetails.getAuthorities()).isNotNull().isEmpty();
            assertThatThrownBy(() -> userDetails.getAuthorities().add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("GIVEN CustomUserDetails authorities WHEN modification attempted THEN throws UnsupportedOperationException")
        void testAuthoritiesImmutable() {
            Account account = new Account();
            account.setEmailOrPhone("user@elearning.com");
            account.setPasswordHash("hash123");
            account.addRole(new Role(1, "Learner"));

            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);

            assertThatThrownBy(() -> userDetails.getAuthorities().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Account Lifecycle Status Mapping")
    class AccountStatusTests {

        @Test
        @DisplayName("GIVEN status = 'Active' WHEN evaluated THEN isEnabled = true and isAccountNonLocked = true")
        void testActiveStatus() {
            Account account = new Account();
            account.setEmailOrPhone("active@elearning.com");
            account.setPasswordHash("hash");
            account.setStatus("Active");

            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);

            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("GIVEN status = 'Inactive' WHEN evaluated THEN isEnabled = false and isAccountNonLocked = true")
        void testInactiveStatus() {
            Account account = new Account();
            account.setEmailOrPhone("inactive@elearning.com");
            account.setPasswordHash("hash");
            account.setStatus("Inactive");

            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);

            assertThat(userDetails.isEnabled()).isFalse();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("GIVEN status = 'Banned' WHEN evaluated THEN isEnabled = false and isAccountNonLocked = false")
        void testBannedStatus() {
            Account account = new Account();
            account.setEmailOrPhone("banned@elearning.com");
            account.setPasswordHash("hash");
            account.setStatus("Banned");

            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);

            assertThat(userDetails.isEnabled()).isFalse();
            assertThat(userDetails.isAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("GIVEN status = 'Locked' WHEN evaluated THEN isEnabled = false and isAccountNonLocked = false")
        void testLockedStatus() {
            Account account = new Account();
            account.setEmailOrPhone("locked@elearning.com");
            account.setPasswordHash("hash");
            account.setStatus("Locked");

            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);

            assertThat(userDetails.isEnabled()).isFalse();
            assertThat(userDetails.isAccountNonLocked()).isFalse();
        }
    }
}
