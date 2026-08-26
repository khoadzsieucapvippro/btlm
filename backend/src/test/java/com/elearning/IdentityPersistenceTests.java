package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("Identity Persistence Mapping Tests (ACCOUNT, USER_PROFILE, ROLE, ACCOUNT_ROLE)")
class IdentityPersistenceTests {

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("Account & UserProfile (1:1 Association) Tests")
    class AccountUserProfileAssociationTests {

        @Test
        @DisplayName("GIVEN Account with UserProfile WHEN persisted THEN both entities are saved and association is loaded correctly")
        void testPersistAndReloadAccountWithUserProfile() {
            // GIVEN
            Account account = new Account();
            account.setEmailOrPhone("learner_test_1@example.com");
            account.setPasswordHash("$2a$12$e8Fj1.V7z9K2p6mQ4rTtY.abc123def456");
            account.setStatus("Active");

            UserProfile profile = new UserProfile();
            profile.setFullName("Nguyen Van A");
            profile.setAvatarUrl("https://storage.example.com/avatars/user1.png");

            account.setUserProfile(profile);

            // WHEN
            Account savedAccount = entityManager.persistAndFlush(account);
            Long accountId = savedAccount.getAccountId();
            Long profileId = profile.getUserId();

            entityManager.clear(); // Clear L1 cache to force SQL SELECT

            // THEN
            Account reloadedAccount = entityManager.find(Account.class, accountId);
            assertThat(reloadedAccount).isNotNull();
            assertThat(reloadedAccount.getEmailOrPhone()).isEqualTo("learner_test_1@example.com");
            assertThat(reloadedAccount.getStatus()).isEqualTo("Active");
            assertThat(reloadedAccount.getCreatedAt()).isNotNull();
            assertThat(reloadedAccount.getUpdatedAt()).isNotNull();

            UserProfile reloadedProfile = reloadedAccount.getUserProfile();
            assertThat(reloadedProfile).isNotNull();
            assertThat(reloadedProfile.getUserId()).isEqualTo(profileId);
            assertThat(reloadedProfile.getFullName()).isEqualTo("Nguyen Van A");
            assertThat(reloadedProfile.getAvatarUrl()).isEqualTo("https://storage.example.com/avatars/user1.png");
            assertThat(reloadedProfile.getAccount().getAccountId()).isEqualTo(accountId);
        }

        @Test
        @DisplayName("GIVEN Account without UserProfile WHEN persisted THEN account is saved with null profile")
        void testPersistAccountWithoutUserProfile() {
            Account account = new Account();
            account.setEmailOrPhone("standalone_account@example.com");
            account.setPasswordHash("$2a$12$e8Fj1.V7z9K2p6mQ4rTtY.standalone123");

            Account saved = entityManager.persistAndFlush(account);
            entityManager.clear();

            Account reloaded = entityManager.find(Account.class, saved.getAccountId());
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getUserProfile()).isNull();
            assertThat(reloaded.getStatus()).isEqualTo("Active");
        }
    }

    @Nested
    @DisplayName("Account & Role (N:N Association via ACCOUNT_ROLE) Tests")
    class AccountRoleAssociationTests {

        @Test
        @DisplayName("GIVEN Account with multiple Roles WHEN persisted THEN ACCOUNT_ROLE junction rows are created and reloaded correctly")
        void testPersistAccountWithRoles() {
            // GIVEN: Obtain seeded roles from database (seeded via V2__seed_roles.sql)
            Role roleLearner = entityManager.find(Role.class, 1);
            Role roleCreator = entityManager.find(Role.class, 2);

            Account account = new Account();
            account.setEmailOrPhone("multi_role_user@example.com");
            account.setPasswordHash("$2a$12$e8Fj1.V7z9K2p6mQ4rTtY.multirole");
            account.addRole(roleLearner);
            account.addRole(roleCreator);

            // WHEN
            Account savedAccount = entityManager.persistAndFlush(account);
            Long accountId = savedAccount.getAccountId();
            entityManager.clear();

            // THEN: Verify entity association graph
            Account reloaded = entityManager.find(Account.class, accountId);
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getRoles()).hasSize(2);
            assertThat(reloaded.getRoles())
                    .extracting(Role::getRoleName)
                    .containsExactlyInAnyOrder("Learner", "Creator");

            // AND: Verify physical composite key rows in ACCOUNT_ROLE junction table
            @SuppressWarnings("unchecked")
            List<Object[]> junctionRows = entityManager.getEntityManager()
                    .createNativeQuery("SELECT account_id, role_id FROM account_role WHERE account_id = :accId ORDER BY role_id")
                    .setParameter("accId", accountId)
                    .getResultList();

            assertThat(junctionRows).hasSize(2);
            assertThat(((Number) junctionRows.get(0)[0]).longValue()).isEqualTo(accountId);
            assertThat(((Number) junctionRows.get(0)[1]).intValue()).isEqualTo(1);
            assertThat(((Number) junctionRows.get(1)[0]).longValue()).isEqualTo(accountId);
            assertThat(((Number) junctionRows.get(1)[1]).intValue()).isEqualTo(2);
        }

        @Test
        @DisplayName("GIVEN Account adding duplicate Role WHEN persisted THEN Set semantics prevent duplicate junction rows")
        void testRoleSetSemanticsPreventDuplicates() {
            Role roleAdmin = entityManager.find(Role.class, 4);

            Account account = new Account();
            account.setEmailOrPhone("admin_unique@example.com");
            account.setPasswordHash("$2a$12$e8Fj1.V7z9K2p6mQ4rTtY.admin123");
            account.addRole(roleAdmin);
            account.addRole(roleAdmin); // Add duplicate in memory

            Account saved = entityManager.persistAndFlush(account);
            Long accountId = saved.getAccountId();
            entityManager.clear();

            @SuppressWarnings("unchecked")
            List<Object[]> junctionRows = entityManager.getEntityManager()
                    .createNativeQuery("SELECT account_id, role_id FROM account_role WHERE account_id = :accId")
                    .setParameter("accId", accountId)
                    .getResultList();

            assertThat(junctionRows).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Cascade and Delete Tests")
    class CascadeBehaviorTests {

        @Test
        @DisplayName("GIVEN Account with Profile and Roles WHEN Account is deleted THEN Profile and junction rows are removed, but Roles remain")
        void testDeleteAccountPreservesRoles() {
            Role roleModerator = entityManager.find(Role.class, 3);

            Account account = new Account();
            account.setEmailOrPhone("mod_to_delete@example.com");
            account.setPasswordHash("$2a$12$e8Fj1.V7z9K2p6mQ4rTtY.moddel");

            UserProfile profile = new UserProfile();
            profile.setFullName("Moderator User");
            account.setUserProfile(profile);
            account.addRole(roleModerator);

            Account saved = entityManager.persistAndFlush(account);
            Long accountId = saved.getAccountId();
            Long profileId = profile.getUserId();
            entityManager.clear();

            // WHEN: Delete Account
            Account toDelete = entityManager.find(Account.class, accountId);
            entityManager.remove(toDelete);
            entityManager.flush();
            entityManager.clear();

            // THEN: Account and UserProfile are gone
            assertThat(entityManager.find(Account.class, accountId)).isNull();
            assertThat(entityManager.find(UserProfile.class, profileId)).isNull();

            // AND: Junction row is deleted
            Number count = (Number) entityManager.getEntityManager()
                    .createNativeQuery("SELECT count(*) FROM account_role WHERE account_id = :accId")
                    .setParameter("accId", accountId)
                    .getSingleResult();
            assertThat(count.intValue()).isEqualTo(0);

            // BUT: Role itself is NOT deleted
            assertThat(entityManager.find(Role.class, 3)).isNotNull();
        }
    }
}
