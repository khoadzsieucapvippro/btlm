package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.security.CustomUserDetails;
import com.elearning.security.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("CustomUserDetailsService Database Integration Tests")
class CustomUserDetailsServiceIntegrationTests {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("GIVEN persisted Account with seeded Role WHEN loaded through CustomUserDetailsService THEN returns populated CustomUserDetails with authorities")
    void testLoadUserFromDatabase() {
        Role learnerRole = roleRepository.findById(1).orElseThrow();

        Account account = new Account();
        account.setEmailOrPhone("integ_user@elearning.com");
        account.setPasswordHash("$2a$10$hashedRealIntegrationTestPasswordHere1234567890ABCDEF");
        account.setStatus("Active");
        account.addRole(learnerRole);

        accountRepository.saveAndFlush(account);

        UserDetails userDetails = userDetailsService.loadUserByUsername("integ_user@elearning.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        assertThat(userDetails.getUsername()).isEqualTo("integ_user@elearning.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedRealIntegrationTestPasswordHere1234567890ABCDEF");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_Learner");

        CustomUserDetails cud = (CustomUserDetails) userDetails;
        assertThat(cud.getAccountId()).isEqualTo(account.getAccountId());
    }

    @Test
    @DisplayName("GIVEN non-existent user in database WHEN loaded THEN throws UsernameNotFoundException")
    void testUserNotFoundInDatabase() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent_db_user@elearning.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email or phone");
    }
}
