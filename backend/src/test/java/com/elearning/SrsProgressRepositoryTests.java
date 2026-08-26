package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Lesson;
import com.elearning.entity.ModerationLog;
import com.elearning.entity.PersonalNote;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.CardProgressRepository;
import com.elearning.repository.ModerationLogRepository;
import com.elearning.repository.PersonalNoteRepository;
import com.elearning.repository.ReviewLogRepository;
import com.elearning.repository.UserSrsSettingRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("SRS, Personal Note & Moderation Spring Data JPA Repository Tests")
class SrsProgressRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CardProgressRepository cardProgressRepository;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    @Autowired
    private PersonalNoteRepository personalNoteRepository;

    @Autowired
    private ModerationLogRepository moderationLogRepository;

    @Autowired
    private UserSrsSettingRepository userSrsSettingRepository;

    private UserProfile createTestUserProfile(String email, String fullName) {
        Account account = new Account();
        account.setEmailOrPhone(email);
        account.setPasswordHash("$2a$12$hash123456789");
        account.setStatus("Active");
        entityManager.persist(account);

        UserProfile profile = new UserProfile();
        profile.setAccount(account);
        profile.setFullName(fullName);
        profile.setAvatarUrl("https://storage.example.com/avatar.jpg");
        return entityManager.persistAndFlush(profile);
    }

    private Vocabulary createTestVocabulary(String hanzi, String pinyin, String pinyinRaw) {
        Vocabulary vocab = new Vocabulary(hanzi, pinyin, pinyinRaw, "Nghĩa Hán Việt", "Nghĩa Tiếng Việt");
        return entityManager.persistAndFlush(vocab);
    }

    private Lesson createTestLesson(String title, Account creator) {
        Lesson lesson = new Lesson(title, creator);
        lesson.setStatus("Pending");
        return entityManager.persistAndFlush(lesson);
    }

    @Nested
    @DisplayName("CardProgressRepository Tests")
    class CardProgressRepoTests {

        @Test
        @DisplayName("GIVEN multiple users and due/future cards WHEN querying due cards at reference time T THEN user isolation and boundary nextReviewAt <= T are strictly enforced")
        void testFindByUserAndNextReviewAtLessThanEqual_DueBoundaryAndUserIsolation() {
            UserProfile userA = createTestUserProfile("srs_repo_a@example.com", "User A");
            UserProfile userB = createTestUserProfile("srs_repo_b@example.com", "User B");

            LocalDateTime referenceNow = LocalDateTime.of(2026, 8, 26, 12, 0, 0);

            // Card 1 (Past -> due): userA, VOCABULARY
            CardProgress card1 = new CardProgress(userA, "VOCABULARY", 101L, new BigDecimal("2.50"), 1, 1, referenceNow.minusHours(2));
            entityManager.persist(card1);

            // Card 2 (Exact now -> due): userA, RADICAL
            CardProgress card2 = new CardProgress(userA, "RADICAL", 55L, new BigDecimal("2.60"), 2, 2, referenceNow);
            entityManager.persist(card2);

            // Card 3 (Future -> not due): userA, VOCABULARY
            CardProgress card3 = new CardProgress(userA, "VOCABULARY", 102L, new BigDecimal("2.50"), 3, 2, referenceNow.plusHours(1));
            entityManager.persist(card3);

            // Card 4 (Past -> due, but userB): userB, VOCABULARY
            CardProgress card4 = new CardProgress(userB, "VOCABULARY", 101L, new BigDecimal("2.50"), 1, 1, referenceNow.minusHours(1));
            entityManager.persist(card4);

            entityManager.flush();
            entityManager.clear();

            // Query due cards for userA at referenceNow
            List<CardProgress> dueCardsA = cardProgressRepository
                    .findByUserAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(userA, referenceNow);

            assertThat(dueCardsA).hasSize(2);
            assertThat(dueCardsA.get(0).getProgressId()).isEqualTo(card1.getProgressId());
            assertThat(dueCardsA.get(0).getItemType()).isEqualTo("VOCABULARY");
            assertThat(dueCardsA.get(1).getProgressId()).isEqualTo(card2.getProgressId());
            assertThat(dueCardsA.get(1).getItemType()).isEqualTo("RADICAL");

            // Count due cards
            long countA = cardProgressRepository.countByUserAndNextReviewAtLessThanEqual(userA, referenceNow);
            assertThat(countA).isEqualTo(2);

            // User B due cards count
            long countB = cardProgressRepository.countByUserAndNextReviewAtLessThanEqual(userB, referenceNow);
            assertThat(countB).isEqualTo(1);
        }

        @Test
        @DisplayName("GIVEN existing card WHEN querying by (user, item_type, item_id) THEN exact card is returned")
        void testFindByUserAndItemTypeAndItemId() {
            UserProfile user = createTestUserProfile("srs_lookup@example.com", "Lookup User");
            CardProgress card = new CardProgress(user, "VOCABULARY", 205L, new BigDecimal("2.50"), 0, 0, LocalDateTime.now());
            entityManager.persistAndFlush(card);
            entityManager.clear();

            Optional<CardProgress> found = cardProgressRepository.findByUserAndItemTypeAndItemId(user, "VOCABULARY", 205L);
            assertThat(found).isPresent();
            assertThat(found.get().getProgressId()).isEqualTo(card.getProgressId());

            Optional<CardProgress> notFound = cardProgressRepository.findByUserAndItemTypeAndItemId(user, "RADICAL", 205L);
            assertThat(notFound).isEmpty();
        }
    }

    @Nested
    @DisplayName("ReviewLogRepository Tests")
    class ReviewLogRepoTests {

        @Test
        @DisplayName("GIVEN multiple review logs for a user WHEN querying history THEN returned in reviewedAt DESC order")
        void testFindByUserOrderByReviewedAtDesc() {
            UserProfile userA = createTestUserProfile("rev_repo_a@example.com", "User Rev A");
            UserProfile userB = createTestUserProfile("rev_repo_b@example.com", "User Rev B");

            LocalDateTime t1 = LocalDateTime.of(2026, 8, 26, 10, 0, 0);
            LocalDateTime t2 = LocalDateTime.of(2026, 8, 26, 11, 0, 0);
            LocalDateTime t3 = LocalDateTime.of(2026, 8, 26, 12, 0, 0);

            ReviewLog log1 = new ReviewLog(userA, "VOCABULARY", 301L, 3, 0, 1, 10);
            log1.setReviewedAt(t1);
            entityManager.persist(log1);

            ReviewLog log2 = new ReviewLog(userA, "VOCABULARY", 301L, 4, 1, 3, 8);
            log2.setReviewedAt(t2);
            entityManager.persist(log2);

            ReviewLog log3 = new ReviewLog(userA, "RADICAL", 12L, 4, 0, 2, 5);
            log3.setReviewedAt(t3);
            entityManager.persist(log3);

            // User B log
            ReviewLog logB = new ReviewLog(userB, "VOCABULARY", 301L, 2, 0, 0, 15);
            entityManager.persist(logB);

            entityManager.flush();
            entityManager.clear();

            List<ReviewLog> logsA = reviewLogRepository.findByUserOrderByReviewedAtDesc(userA);
            assertThat(logsA).hasSize(3);
            assertThat(logsA.get(0).getLogId()).isEqualTo(log3.getLogId());
            assertThat(logsA.get(1).getLogId()).isEqualTo(log2.getLogId());
            assertThat(logsA.get(2).getLogId()).isEqualTo(log1.getLogId());

            // By specific card
            List<ReviewLog> cardLogs = reviewLogRepository
                    .findByUserAndItemTypeAndItemIdOrderByReviewedAtDesc(userA, "VOCABULARY", 301L);
            assertThat(cardLogs).hasSize(2);
            assertThat(cardLogs.get(0).getLogId()).isEqualTo(log2.getLogId());
            assertThat(cardLogs.get(1).getLogId()).isEqualTo(log1.getLogId());
        }
    }

    @Nested
    @DisplayName("PersonalNoteRepository Tests")
    class PersonalNoteRepoTests {

        @Test
        @DisplayName("GIVEN 6 notes for one user and vocabulary WHEN querying THEN all 6 notes are returned in createdAt DESC order without cap")
        void testFindByUserAndVocabularyOrderByCreatedAtDesc_NoFiveNoteLimit() {
            UserProfile user = createTestUserProfile("note_repo_user@example.com", "Note User");
            Vocabulary vocab = createTestVocabulary("木", "mù", "mu");

            for (int i = 1; i <= 6; i++) {
                PersonalNote note = new PersonalNote(user, vocab, "Ghi chú phân tích " + i);
                entityManager.persist(note);
            }
            entityManager.flush();
            entityManager.clear();

            List<PersonalNote> notes = personalNoteRepository
                    .findByUserAndVocabularyOrderByCreatedAtDesc(user, vocab);

            assertThat(notes).hasSize(6);
        }

        @Test
        @DisplayName("GIVEN note owned by User A WHEN User B queries by noteId and user THEN returns Optional.empty()")
        void testFindByNoteIdAndUser_OwnershipIsolation() {
            UserProfile userA = createTestUserProfile("note_owner_a@example.com", "Owner A");
            UserProfile userB = createTestUserProfile("note_owner_b@example.com", "Owner B");
            Vocabulary vocab = createTestVocabulary("人", "rén", "ren");

            PersonalNote noteA = new PersonalNote(userA, vocab, "Ghi chú của user A");
            entityManager.persistAndFlush(noteA);
            Long noteId = noteA.getNoteId();
            entityManager.clear();

            Optional<PersonalNote> foundA = personalNoteRepository.findByNoteIdAndUser(noteId, userA);
            assertThat(foundA).isPresent();
            assertThat(foundA.get().getContent()).isEqualTo("Ghi chú của user A");

            Optional<PersonalNote> foundB = personalNoteRepository.findByNoteIdAndUser(noteId, userB);
            assertThat(foundB).isEmpty();
        }
    }

    @Nested
    @DisplayName("ModerationLogRepository Tests")
    class ModerationLogRepoTests {

        @Test
        @DisplayName("GIVEN moderation logs for multiple lessons WHEN querying by lesson THEN only that lesson's audit trail is returned newest first")
        void testFindByLessonOrderByCreatedAtDesc() {
            UserProfile creator = createTestUserProfile("creator_mod_repo@example.com", "Creator");
            UserProfile modProfile = createTestUserProfile("mod_repo@example.com", "Moderator");
            Account moderator = modProfile.getAccount();

            Lesson lesson1 = createTestLesson("Lesson 1: Chào hỏi", creator.getAccount());
            Lesson lesson2 = createTestLesson("Lesson 2: Mua sắm", creator.getAccount());

            ModerationLog log1 = new ModerationLog(lesson1, moderator, "Reject", "Lỗi chính tả", "[\"pinyin\"]");
            log1.setCreatedAt(LocalDateTime.of(2026, 8, 26, 10, 0, 0));
            entityManager.persist(log1);

            ModerationLog log2 = new ModerationLog(lesson1, moderator, "Approve", null, null);
            log2.setCreatedAt(LocalDateTime.of(2026, 8, 26, 11, 0, 0));
            entityManager.persist(log2);

            ModerationLog log3 = new ModerationLog(lesson2, moderator, "Reject", "Chưa hoàn thiện", null);
            entityManager.persist(log3);

            entityManager.flush();
            entityManager.clear();

            List<ModerationLog> history = moderationLogRepository.findByLessonOrderByCreatedAtDesc(lesson1);
            assertThat(history).hasSize(2);
            assertThat(history.get(0).getLogId()).isEqualTo(log2.getLogId());
            assertThat(history.get(0).getAction()).isEqualTo("Approve");
            assertThat(history.get(1).getLogId()).isEqualTo(log1.getLogId());
            assertThat(history.get(1).getAction()).isEqualTo("Reject");

            // By lesson ID
            List<ModerationLog> historyById = moderationLogRepository
                    .findByLesson_LessonIdOrderByCreatedAtDesc(lesson1.getLessonId());
            assertThat(historyById).hasSize(2);
        }
    }

    @Nested
    @DisplayName("UserSrsSettingRepository Tests")
    class UserSrsSettingRepoTests {

        @Test
        @DisplayName("GIVEN user with custom SRS settings WHEN querying by user THEN returns setting; missing user returns empty Optional")
        void testFindByUser() {
            UserProfile userA = createTestUserProfile("srs_setting_userA@example.com", "User Srs A");
            UserProfile userB = createTestUserProfile("srs_setting_userB@example.com", "User Srs B");

            UserSrsSetting settingA = new UserSrsSetting(userA, 30, 150);
            entityManager.persistAndFlush(settingA);
            entityManager.clear();

            Optional<UserSrsSetting> foundA = userSrsSettingRepository.findByUser(userA);
            assertThat(foundA).isPresent();
            assertThat(foundA.get().getNewCardsPerDay()).isEqualTo(30);
            assertThat(foundA.get().getMaxReviewPerDay()).isEqualTo(150);

            Optional<UserSrsSetting> foundB = userSrsSettingRepository.findByUser(userB);
            assertThat(foundB).isEmpty();

            assertThat(userSrsSettingRepository.existsByUser(userA)).isTrue();
            assertThat(userSrsSettingRepository.existsByUser(userB)).isFalse();
        }
    }
}
