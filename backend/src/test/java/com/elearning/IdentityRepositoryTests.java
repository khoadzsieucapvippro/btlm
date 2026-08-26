package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("Identity Spring Data JPA Repository Tests")
class IdentityRepositoryTests {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("AccountRepository Tests")
    class AccountRepositoryTests {

        @Test
        @DisplayName("GIVEN existing account WHEN findByEmailOrPhone THEN returns correct Account in Optional")
        void testFindByEmailOrPhoneExisting() {
            Account account = new Account();
            account.setEmailOrPhone("repo_learner@example.com");
            account.setPasswordHash("$2a$12$hashedpasswordforemaillookup");
            account.setStatus("Active");
            Account saved = entityManager.persistAndFlush(account);
            entityManager.clear();

            Optional<Account> found = accountRepository.findByEmailOrPhone("repo_learner@example.com");

            assertThat(found).isPresent();
            assertThat(found.get().getAccountId()).isEqualTo(saved.getAccountId());
            assertThat(found.get().getEmailOrPhone()).isEqualTo("repo_learner@example.com");
            assertThat(found.get().getStatus()).isEqualTo("Active");
        }

        @Test
        @DisplayName("GIVEN nonexistent email or phone WHEN findByEmailOrPhone THEN returns Optional.empty()")
        void testFindByEmailOrPhoneNotFound() {
            Optional<Account> found = accountRepository.findByEmailOrPhone("nonexistent_repo@example.com");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("GIVEN email or phone WHEN existsByEmailOrPhone THEN returns true for existing and false for missing")
        void testExistsByEmailOrPhone() {
            Account account = new Account();
            account.setEmailOrPhone("exists_check@example.com");
            account.setPasswordHash("$2a$12$hashedpasswordforexistscheck");
            entityManager.persistAndFlush(account);
            entityManager.clear();

            assertThat(accountRepository.existsByEmailOrPhone("exists_check@example.com")).isTrue();
            assertThat(accountRepository.existsByEmailOrPhone("does_not_exist@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("RoleRepository Tests")
    class RoleRepositoryTests {

        @Test
        @DisplayName("GIVEN seeded role WHEN findByRoleName THEN returns correct Role in Optional")
        void testFindByRoleNameExisting() {
            Optional<Role> found = roleRepository.findByRoleName("Learner");

            assertThat(found).isPresent();
            assertThat(found.get().getRoleId()).isEqualTo(1);
            assertThat(found.get().getRoleName()).isEqualTo("Learner");
        }

        @Test
        @DisplayName("GIVEN nonexistent role name WHEN findByRoleName THEN returns Optional.empty()")
        void testFindByRoleNameNotFound() {
            Optional<Role> found = roleRepository.findByRoleName("ROLE_NONEXISTENT");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("UserProfileRepository Tests")
    class UserProfileRepositoryTests {

        @Test
        @DisplayName("GIVEN account with profile WHEN findByAccount THEN returns correct UserProfile")
        void testFindByAccountExisting() {
            Account account = new Account();
            account.setEmailOrPhone("profile_owner@example.com");
            account.setPasswordHash("$2a$12$hashedpwdforprofileowner");

            UserProfile profile = new UserProfile();
            profile.setFullName("Tran Thi B");
            profile.setAvatarUrl("https://cdn.example.com/avatar/user2.jpg");
            account.setUserProfile(profile);

            Account savedAccount = entityManager.persistAndFlush(account);
            Long profileId = profile.getUserId();
            entityManager.clear();

            Optional<UserProfile> found = userProfileRepository.findByAccount(savedAccount);

            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(profileId);
            assertThat(found.get().getFullName()).isEqualTo("Tran Thi B");
            assertThat(found.get().getAvatarUrl()).isEqualTo("https://cdn.example.com/avatar/user2.jpg");
            assertThat(found.get().getAccount().getAccountId()).isEqualTo(savedAccount.getAccountId());
        }

        @Test
        @DisplayName("GIVEN account without profile WHEN findByAccount THEN returns Optional.empty()")
        void testFindByAccountNotFound() {
            Account account = new Account();
            account.setEmailOrPhone("no_profile@example.com");
            account.setPasswordHash("$2a$12$hashedpwdfornoprofile");
            Account savedAccount = entityManager.persistAndFlush(account);
            entityManager.clear();

            Optional<UserProfile> found = userProfileRepository.findByAccount(savedAccount);

            assertThat(found).isEmpty();
        }
    }
}
