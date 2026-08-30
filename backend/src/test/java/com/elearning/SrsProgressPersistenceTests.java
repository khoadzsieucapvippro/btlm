package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.UserProfile;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("SRS Progress & Review Log Persistence Tests (CARD_PROGRESS, REVIEW_LOG - Polymorphic Reference)")
class SrsProgressPersistenceTests {

    @Autowired
    private TestEntityManager entityManager;

    private UserProfile createTestUserProfile(String email) {
        Account account = new Account();
        account.setEmailOrPhone(email);
        account.setPasswordHash("$2a$12$hash123456789");
        account.setStatus("Active");
        entityManager.persist(account);

        UserProfile profile = new UserProfile();
        profile.setAccount(account);
        profile.setFullName("Lê Thị B");
        profile.setAvatarUrl("https://storage.example.com/avatar_srs.jpg");
        return entityManager.persistAndFlush(profile);
    }

    @Nested
    @DisplayName("CardProgress Polymorphic Entity Tests")
    class CardProgressTests {

        @Test
        @DisplayName("GIVEN CardProgress with item_type=VOCABULARY WHEN persisted and reloaded THEN scalar values match without FK to Vocabulary")
        void testCardProgress_VocabularyPolymorphic() {
            UserProfile profile = createTestUserProfile("srs_vocab@example.com");

            LocalDateTime nextReview = LocalDateTime.now().plusDays(1);
            CardProgress card = new CardProgress();
            card.setUser(profile);
            card.setItemType("VOCABULARY");
            card.setItemId(101L);
            card.setEaseFactor(new BigDecimal("2.50"));
            card.setIntervalDays(1);
            card.setRepetitions(1);
            card.setNextReviewAt(nextReview);

            CardProgress saved = entityManager.persistAndFlush(card);
            Long cardId = saved.getProgressId();
            entityManager.clear();

            CardProgress reloaded = entityManager.find(CardProgress.class, cardId);
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getProgressId()).isEqualTo(cardId);
            assertThat(reloaded.getUser().getUserId()).isEqualTo(profile.getUserId());
            assertThat(reloaded.getItemType()).isEqualTo("VOCABULARY");
            assertThat(reloaded.getItemId()).isEqualTo(101L);
            assertThat(reloaded.getEaseFactor()).isEqualByComparingTo("2.50");
            assertThat(reloaded.getIntervalDays()).isEqualTo(1);
            assertThat(reloaded.getRepetitions()).isEqualTo(1);
            assertThat(reloaded.getNextReviewAt()).isNotNull();
        }

        @Test
        @DisplayName("GIVEN CardProgress with item_type=RADICAL WHEN persisted and reloaded THEN scalar values match without FK to Radical")
        void testCardProgress_RadicalPolymorphic() {
            UserProfile profile = createTestUserProfile("srs_radical@example.com");

            LocalDateTime nextReview = LocalDateTime.now().plusDays(3);
            CardProgress card = new CardProgress();
            card.setUser(profile);
            card.setItemType("RADICAL");
            card.setItemId(42L);
            card.setEaseFactor(new BigDecimal("2.60"));
            card.setIntervalDays(3);
            card.setRepetitions(2);
            card.setNextReviewAt(nextReview);

            CardProgress saved = entityManager.persistAndFlush(card);
            Long cardId = saved.getProgressId();
            entityManager.clear();

            CardProgress reloaded = entityManager.find(CardProgress.class, cardId);
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getItemType()).isEqualTo("RADICAL");
            assertThat(reloaded.getItemId()).isEqualTo(42L);
            assertThat(reloaded.getEaseFactor()).isEqualByComparingTo("2.60");
            assertThat(reloaded.getIntervalDays()).isEqualTo(3);
            assertThat(reloaded.getRepetitions()).isEqualTo(2);
        }

        @Test
        @DisplayName("GIVEN nonexistent item_id WHEN CardProgress is persisted THEN succeeds because no physical FK exists")
        void testPolymorphicIntegrityBoundary_NonExistentItemIdAllowed() {
            UserProfile profile = createTestUserProfile("srs_boundary@example.com");

            CardProgress card = new CardProgress();
            card.setUser(profile);
            card.setItemType("VOCABULARY");
            card.setItemId(999999999L); // Non-existent catalog ID
            card.setEaseFactor(new BigDecimal("2.50"));
            card.setIntervalDays(0);
            card.setRepetitions(0);

            CardProgress saved = entityManager.persistAndFlush(card);
            Long cardId = saved.getProgressId();
            entityManager.clear();

            CardProgress reloaded = entityManager.find(CardProgress.class, cardId);
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getItemId()).isEqualTo(999999999L);
        }

        @Test
        @DisplayName("GIVEN duplicate (user, item_type, item_id) WHEN persisted THEN unique constraint uk_card_progress_user_item triggers violation")
        void testCardProgress_UniqueConstraintViolation() {
            UserProfile profile = createTestUserProfile("srs_dup@example.com");

            CardProgress card1 = new CardProgress(profile, "VOCABULARY", 88L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now());
            entityManager.persistAndFlush(card1);

            CardProgress card2 = new CardProgress(profile, "VOCABULARY", 88L, new BigDecimal("2.50"), 2, 2, LocalDateTime.now().plusDays(2));

            assertThatThrownBy(() -> {
                entityManager.persist(card2);
                entityManager.flush();
            }).isInstanceOf(PersistenceException.class);
        }

        @Test
        @DisplayName("GIVEN CardProgress WHEN persisted and updated THEN @Version initializes to 0 and increments monotonically (BE-CONC-001)")
        void testCardProgress_VersionInitializationAndIncrement() {
            UserProfile profile = createTestUserProfile("srs_version@example.com");

            CardProgress card = new CardProgress(profile, "VOCABULARY", 99L, new BigDecimal("2.50"), 0, 0, LocalDateTime.now());
            CardProgress saved = entityManager.persistAndFlush(card);

            assertThat(saved.getVersion()).isEqualTo(0L);

            // Update state and flush
            saved.setRepetitions(1);
            saved.setIntervalDays(1);
            entityManager.flush();

            assertThat(saved.getVersion()).isEqualTo(1L);

            saved.setRepetitions(2);
            saved.setIntervalDays(6);
            entityManager.flush();

            assertThat(saved.getVersion()).isEqualTo(2L);
        }

        @Test
        @DisplayName("GIVEN two detached instances of same CardProgress WHEN first updates and second attempts stale update THEN optimistic lock triggers (BE-CONC-001)")
        void testCardProgress_OptimisticLockingPreventsStaleWrite() {
            UserProfile profile = createTestUserProfile("srs_stale@example.com");

            CardProgress card = new CardProgress(profile, "VOCABULARY", 150L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now());
            CardProgress saved = entityManager.persistAndFlush(card);
            Long cardId = saved.getProgressId();
            assertThat(saved.getVersion()).isEqualTo(0L);

            entityManager.clear();

            // Load instance 1 and instance 2 independently
            CardProgress instance1 = entityManager.find(CardProgress.class, cardId);
            entityManager.detach(instance1);

            CardProgress instance2 = entityManager.find(CardProgress.class, cardId);

            // Instance 2 modifies and commits
            instance2.setRepetitions(2);
            instance2.setIntervalDays(6);
            entityManager.flush();
            assertThat(instance2.getVersion()).isEqualTo(1L);

            // Instance 1 (stale version 0) attempts to merge/save
            instance1.setRepetitions(3);
            instance1.setIntervalDays(10);

            assertThatThrownBy(() -> {
                entityManager.merge(instance1);
                entityManager.flush();
            }).isInstanceOf(PersistenceException.class);
        }
    }

    @Nested
    @DisplayName("ReviewLog Polymorphic Entity Tests")
    class ReviewLogTests {

        @Test
        @DisplayName("GIVEN ReviewLog for VOCABULARY and RADICAL WHEN persisted THEN review_time_seconds, rating, and scalars are exact")
        void testReviewLog_VocabularyAndRadicalPolymorphic() {
            UserProfile profile = createTestUserProfile("review_log@example.com");

            // Log 1: VOCABULARY
            ReviewLog logVocab = new ReviewLog();
            logVocab.setUser(profile);
            logVocab.setItemType("VOCABULARY");
            logVocab.setItemId(200L);
            logVocab.setRating(3);
            logVocab.setIntervalBefore(0);
            logVocab.setIntervalAfter(1);
            logVocab.setReviewTimeSeconds(12);

            ReviewLog savedVocab = entityManager.persistAndFlush(logVocab);
            Long idVocab = savedVocab.getLogId();

            // Log 2: RADICAL
            ReviewLog logRadical = new ReviewLog();
            logRadical.setUser(profile);
            logRadical.setItemType("RADICAL");
            logRadical.setItemId(15L);
            logRadical.setRating(4);
            logRadical.setIntervalBefore(1);
            logRadical.setIntervalAfter(3);
            logRadical.setReviewTimeSeconds(5);

            ReviewLog savedRadical = entityManager.persistAndFlush(logRadical);
            Long idRadical = savedRadical.getLogId();

            entityManager.clear();

            ReviewLog reloadedVocab = entityManager.find(ReviewLog.class, idVocab);
            assertThat(reloadedVocab).isNotNull();
            assertThat(reloadedVocab.getItemType()).isEqualTo("VOCABULARY");
            assertThat(reloadedVocab.getItemId()).isEqualTo(200L);
            assertThat(reloadedVocab.getRating()).isEqualTo((byte) 3);
            assertThat(reloadedVocab.getIntervalBefore()).isEqualTo(0);
            assertThat(reloadedVocab.getIntervalAfter()).isEqualTo(1);
            assertThat(reloadedVocab.getReviewTimeSeconds()).isEqualTo(12);
            assertThat(reloadedVocab.getReviewedAt()).isNotNull();

            ReviewLog reloadedRadical = entityManager.find(ReviewLog.class, idRadical);
            assertThat(reloadedRadical).isNotNull();
            assertThat(reloadedRadical.getItemType()).isEqualTo("RADICAL");
            assertThat(reloadedRadical.getItemId()).isEqualTo(15L);
            assertThat(reloadedRadical.getRating()).isEqualTo((byte) 4);
            assertThat(reloadedRadical.getIntervalBefore()).isEqualTo(1);
            assertThat(reloadedRadical.getIntervalAfter()).isEqualTo(3);
            assertThat(reloadedRadical.getReviewTimeSeconds()).isEqualTo(5);
            assertThat(reloadedRadical.getReviewedAt()).isNotNull();
        }
    }
}
