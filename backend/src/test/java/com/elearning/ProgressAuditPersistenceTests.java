package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.ModerationLog;
import com.elearning.entity.PersonalNote;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.entity.Vocabulary;
import jakarta.persistence.PersistenceException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("Progress, Audit & Personalization Persistence Tests (USER_SRS_SETTING, PERSONAL_NOTE, MODERATION_LOG)")
class ProgressAuditPersistenceTests {

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
        profile.setFullName("Nguyễn Văn A");
        profile.setAvatarUrl("https://storage.example.com/avatar.jpg");
        return entityManager.persistAndFlush(profile);
    }

    private Vocabulary createTestVocabulary(String hanzi, String pinyin, String pinyinRaw) {
        Vocabulary vocab = new Vocabulary(hanzi, pinyin, pinyinRaw, "Nghĩa Hán Việt", "Nghĩa Tiếng Việt");
        return entityManager.persistAndFlush(vocab);
    }

    private Lesson createTestLesson(String title, Account creator) {
        Lesson lesson = new Lesson(title, creator);
        lesson.setStatus("Draft");
        return entityManager.persistAndFlush(lesson);
    }

    @Nested
    @DisplayName("UserSrsSetting Entity Tests")
    class UserSrsSettingTests {

        @Test
        @DisplayName("GIVEN valid UserProfile WHEN persisting UserSrsSetting THEN 1:1 mapping and values are stored accurately")
        void testPersistAndReloadUserSrsSetting() {
            UserProfile profile = createTestUserProfile("srs_user@example.com");

            UserSrsSetting setting = new UserSrsSetting();
            setting.setUser(profile);
            setting.setNewCardsPerDay(25);
            setting.setMaxReviewPerDay(120);

            UserSrsSetting saved = entityManager.persistAndFlush(setting);
            Long settingId = saved.getSettingId();
            entityManager.clear();

            UserSrsSetting reloaded = entityManager.find(UserSrsSetting.class, settingId);
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getSettingId()).isEqualTo(settingId);
            assertThat(reloaded.getUser()).isNotNull();
            assertThat(reloaded.getUser().getUserId()).isEqualTo(profile.getUserId());
            assertThat(reloaded.getNewCardsPerDay()).isEqualTo(25);
            assertThat(reloaded.getMaxReviewPerDay()).isEqualTo(120);
        }
    }

    @Nested
    @DisplayName("PersonalNote Entity Tests")
    class PersonalNoteTests {

        @Test
        @DisplayName("GIVEN UserProfile and Vocabulary WHEN persisting PersonalNote THEN note is stored with correct associations")
        void testPersistAndReloadPersonalNote() {
            UserProfile profile = createTestUserProfile("note_user1@example.com");
            Vocabulary vocab = createTestVocabulary("猫", "māo", "mao");

            PersonalNote note = new PersonalNote();
            note.setUser(profile);
            note.setVocabulary(vocab);
            note.setContent("Từ này có bộ Khuyển (chó/thú) phía trước.");

            PersonalNote saved = entityManager.persistAndFlush(note);
            Long noteId = saved.getNoteId();
            entityManager.clear();

            PersonalNote reloaded = entityManager.find(PersonalNote.class, noteId);
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getNoteId()).isEqualTo(noteId);
            assertThat(reloaded.getUser().getUserId()).isEqualTo(profile.getUserId());
            assertThat(reloaded.getVocabulary().getVocabId()).isEqualTo(vocab.getVocabId());
            assertThat(reloaded.getContent()).isEqualTo("Từ này có bộ Khuyển (chó/thú) phía trước.");
            assertThat(reloaded.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("GIVEN note content of exactly 500 characters WHEN persisting THEN succeeds without truncation")
        void testPersonalNoteExact500Characters() {
            UserProfile profile = createTestUserProfile("note_user2@example.com");
            Vocabulary vocab = createTestVocabulary("狗", "gǒu", "gou");

            String content500 = "A".repeat(500);
            PersonalNote note = new PersonalNote(profile, vocab, content500);

            PersonalNote saved = entityManager.persistAndFlush(note);
            Long noteId = saved.getNoteId();
            entityManager.clear();

            PersonalNote reloaded = entityManager.find(PersonalNote.class, noteId);
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getContent()).hasSize(500);
            assertThat(reloaded.getContent()).isEqualTo(content500);
        }

        @Test
        @DisplayName("GIVEN one user and one vocabulary WHEN persisting 6 notes THEN all 6 notes persist without arbitrary limit")
        void testMultipleNotesPerVocabulary_NoFiveNoteLimit() {
            UserProfile profile = createTestUserProfile("note_user3@example.com");
            Vocabulary vocab = createTestVocabulary("水", "shuǐ", "shui");

            for (int i = 1; i <= 6; i++) {
                PersonalNote note = new PersonalNote(profile, vocab, "Ghi chú số " + i);
                entityManager.persist(note);
            }
            entityManager.flush();
            entityManager.clear();

            @SuppressWarnings("unchecked")
            List<PersonalNote> notes = entityManager.getEntityManager()
                    .createQuery("SELECT n FROM PersonalNote n WHERE n.user.userId = :uId AND n.vocabulary.vocabId = :vId")
                    .setParameter("uId", profile.getUserId())
                    .setParameter("vId", vocab.getVocabId())
                    .getResultList();

            assertThat(notes).hasSize(6);
        }

        @Test
        @DisplayName("GIVEN PersonalNote referencing Vocabulary WHEN note is deleted THEN Vocabulary is NOT cascade-deleted")
        void testDeletePersonalNoteDoesNotDeleteVocabulary() {
            UserProfile profile = createTestUserProfile("note_user4@example.com");
            Vocabulary vocab = createTestVocabulary("火", "huǒ", "huo");

            PersonalNote note = new PersonalNote(profile, vocab, "Bộ Hỏa liên quan đến lửa");
            PersonalNote saved = entityManager.persistAndFlush(note);
            Long noteId = saved.getNoteId();
            Long vocabId = vocab.getVocabId();
            entityManager.clear();

            PersonalNote toDelete = entityManager.find(PersonalNote.class, noteId);
            entityManager.remove(toDelete);
            entityManager.flush();
            entityManager.clear();

            assertThat(entityManager.find(PersonalNote.class, noteId)).isNull();
            assertThat(entityManager.find(Vocabulary.class, vocabId)).isNotNull();
        }
    }

    @Nested
    @DisplayName("ModerationLog Entity Tests")
    class ModerationLogTests {

        @Test
        @DisplayName("GIVEN Lesson and Moderator WHEN persisting ModerationLog THEN audit fields are stored immutably")
        void testPersistAndReloadModerationLog() {
            UserProfile creatorProfile = createTestUserProfile("creator_mod@example.com");
            Lesson lesson = createTestLesson("Bài 10: Du lịch", creatorProfile.getAccount());

            UserProfile modProfile = createTestUserProfile("moderator1@example.com");
            Account moderator = modProfile.getAccount();

            ModerationLog log = new ModerationLog();
            log.setLesson(lesson);
            log.setModerator(moderator);
            log.setAction("Reject");
            log.setRejectionReason("File Excel thiếu cột nghĩa tiếng Việt ở hàng số 5.");
            log.setFlaggedFields("[\"meaningVi\", \"exampleSentence\"]");

            ModerationLog saved = entityManager.persistAndFlush(log);
            Long logId = saved.getLogId();
            entityManager.clear();

            ModerationLog reloaded = entityManager.find(ModerationLog.class, logId);
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getLogId()).isEqualTo(logId);
            assertThat(reloaded.getLesson().getLessonId()).isEqualTo(lesson.getLessonId());
            assertThat(reloaded.getModerator().getAccountId()).isEqualTo(moderator.getAccountId());
            assertThat(reloaded.getAction()).isEqualTo("Reject");
            assertThat(reloaded.getRejectionReason()).isEqualTo("File Excel thiếu cột nghĩa tiếng Việt ở hàng số 5.");
            assertThat(reloaded.getFlaggedFields()).isEqualTo("[\"meaningVi\", \"exampleSentence\"]");
            assertThat(reloaded.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("GIVEN Lesson referenced by ModerationLog WHEN attempting to delete Lesson THEN DB RESTRICT constraint blocks deletion")
        void testAuditDeleteProtection_OnDeleteRestrict() {
            UserProfile creatorProfile = createTestUserProfile("creator_mod2@example.com");
            Lesson lesson = createTestLesson("Bài 11: Mua sắm", creatorProfile.getAccount());

            UserProfile modProfile = createTestUserProfile("moderator2@example.com");
            Account moderator = modProfile.getAccount();

            ModerationLog log = new ModerationLog(lesson, moderator, "Approve", null, null);
            entityManager.persistAndFlush(log);
            entityManager.clear();

            Lesson lessonToDelete = entityManager.find(Lesson.class, lesson.getLessonId());
            entityManager.remove(lessonToDelete);

            // DB constraint fk_moderation_log_lesson ON DELETE RESTRICT prevents deleting lesson
            assertThatThrownBy(() -> entityManager.flush())
                    .isInstanceOf(PersistenceException.class);
        }
    }
}
