package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.repository.AccountRepository;
import com.elearning.security.CustomUserDetails;
import com.elearning.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTests {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setEmailOrPhone("user@elearning.com");
        testAccount.setPasswordHash("$2a$10$hashedPasswordHere1234567890ABCDEF");
        testAccount.setStatus("Active");
        testAccount.addRole(new Role(1, "Learner"));
        testAccount.addRole(new Role(2, "Creator"));
    }

    @Nested
    @DisplayName("User Loading Tests")
    class LoadingTests {

        @Test
        @DisplayName("GIVEN existing account in repository WHEN loadUserByUsername THEN returns populated CustomUserDetails")
        void testLoadExistingUser() {
            when(accountRepository.findByEmailOrPhone("user@elearning.com"))
                    .thenReturn(Optional.of(testAccount));

            UserDetails userDetails = userDetailsService.loadUserByUsername("user@elearning.com");

            assertThat(userDetails).isNotNull();
            assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
            assertThat(userDetails.getUsername()).isEqualTo("user@elearning.com");
            assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPasswordHere1234567890ABCDEF");
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_Learner", "ROLE_Creator");

            verify(accountRepository).findByEmailOrPhone("user@elearning.com");
        }

        @Test
        @DisplayName("GIVEN non-existent account identifier WHEN loadUserByUsername THEN throws UsernameNotFoundException without leaking SQL")
        void testUserNotFoundThrows() {
            when(accountRepository.findByEmailOrPhone("unknown@elearning.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@elearning.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found with email or phone: unknown@elearning.com")
                    .hasMessageNotContaining("SELECT")
                    .hasMessageNotContaining("FROM account");

            verify(accountRepository).findByEmailOrPhone("unknown@elearning.com");
        }

        @Test
        @DisplayName("GIVEN null or blank identifier WHEN loadUserByUsername THEN throws UsernameNotFoundException without querying repository")
        void testNullOrBlankIdentifierThrows() {
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                    .isInstanceOf(UsernameNotFoundException.class);

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                    .isInstanceOf(UsernameNotFoundException.class);

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("   "))
                    .isInstanceOf(UsernameNotFoundException.class);

            verify(accountRepository, never()).findByEmailOrPhone(anyString());
        }
    }

    @Nested
    @DisplayName("Role Boundary and Separation Tests")
    class RoleBoundaryTests {

        @Test
        @DisplayName("GIVEN account without roles WHEN loadUserByUsername THEN authorities are empty and Learner role is NOT auto-assigned")
        void testNoRolesNotAutoAssigned() {
            Account accountNoRoles = new Account();
            accountNoRoles.setEmailOrPhone("norole@elearning.com");
            accountNoRoles.setPasswordHash("hash");
            accountNoRoles.setStatus("Active");

            when(accountRepository.findByEmailOrPhone("norole@elearning.com"))
                    .thenReturn(Optional.of(accountNoRoles));

            UserDetails userDetails = userDetailsService.loadUserByUsername("norole@elearning.com");

            assertThat(userDetails.getAuthorities()).isEmpty();
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .doesNotContain("ROLE_Learner");
        }
    }

    @Nested
    @DisplayName("Status Semantics Tests")
    class StatusSemanticsTests {

        @Test
        @DisplayName("GIVEN banned account WHEN loadUserByUsername THEN isAccountNonLocked = false and isEnabled = false")
        void testBannedAccountStatus() {
            testAccount.setStatus("Banned");
            when(accountRepository.findByEmailOrPhone("user@elearning.com"))
                    .thenReturn(Optional.of(testAccount));

            UserDetails userDetails = userDetailsService.loadUserByUsername("user@elearning.com");

            assertThat(userDetails.isAccountNonLocked()).isFalse();
            assertThat(userDetails.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("GIVEN inactive account WHEN loadUserByUsername THEN isEnabled = false and isAccountNonLocked = true")
        void testInactiveAccountStatus() {
            testAccount.setStatus("Inactive");
            when(accountRepository.findByEmailOrPhone("user@elearning.com"))
                    .thenReturn(Optional.of(testAccount));

            UserDetails userDetails = userDetailsService.loadUserByUsername("user@elearning.com");

            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.isEnabled()).isFalse();
        }
    }
}
