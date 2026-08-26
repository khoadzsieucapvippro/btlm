package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Vocabulary;
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
@DisplayName("Lesson Persistence Mapping Tests (LESSON, LESSON_VOCABULARY)")
class LessonPersistenceTests {

    @Autowired
    private TestEntityManager entityManager;

    private Account createTestAccount(String email) {
        Account account = new Account();
        account.setEmailOrPhone(email);
        account.setPasswordHash("$2a$12$creatorPasswordHash123456789");
        account.setStatus("Active");
        return entityManager.persistAndFlush(account);
    }

    private Vocabulary createTestVocabulary(String hanzi, String pinyin, String pinyinRaw) {
        Vocabulary vocab = new Vocabulary(hanzi, pinyin, pinyinRaw, "Nghĩa Hán Việt", "Nghĩa Tiếng Việt");
        return entityManager.persistAndFlush(vocab);
    }

    @Nested
    @DisplayName("Lesson Entity Persistence Tests")
    class LessonEntityTests {

        @Test
        @DisplayName("GIVEN valid Account WHEN Lesson references Account as createdBy THEN persists correct FK and attributes")
        void testPersistLessonWithCreatedByAccount() {
            Account creator = createTestAccount("creator_lesson1@example.com");

            Lesson lesson = new Lesson();
            lesson.setTitle("Bài 1: Chào hỏi tiếng Trung cơ bản");
            lesson.setExcelFileUrl("https://storage.example.com/lessons/lesson1.xlsx");
            lesson.setCreatedBy(creator);
            lesson.setStatus("Draft");

            Lesson saved = entityManager.persistAndFlush(lesson);
            Long lessonId = saved.getLessonId();
            entityManager.clear();

            Lesson reloaded = entityManager.find(Lesson.class, lessonId);
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getLessonId()).isEqualTo(lessonId);
            assertThat(reloaded.getTitle()).isEqualTo("Bài 1: Chào hỏi tiếng Trung cơ bản");
            assertThat(reloaded.getExcelFileUrl()).isEqualTo("https://storage.example.com/lessons/lesson1.xlsx");
            assertThat(reloaded.getStatus()).isEqualTo("Draft");
            assertThat(reloaded.getCreatedBy()).isNotNull();
            assertThat(reloaded.getCreatedBy().getAccountId()).isEqualTo(creator.getAccountId());
            assertThat(reloaded.getCreatedAt()).isNotNull();
            assertThat(reloaded.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("LessonVocabulary Association and Ordering Tests")
    class LessonVocabularyTests {

        @Test
        @DisplayName("GIVEN Lesson and Vocabulary WHEN LessonVocabulary is persisted THEN composite PK and order_index are stored")
        void testPersistLessonVocabularyWithCompositeKey() {
            Account creator = createTestAccount("creator_lesson2@example.com");
            Lesson lesson = new Lesson("Bài 2: Gia đình", creator);
            Vocabulary vocab = createTestVocabulary("爸爸", "bàba", "baba");

            lesson.addVocabulary(vocab, 1);
            Lesson saved = entityManager.persistAndFlush(lesson);
            Long lessonId = saved.getLessonId();
            entityManager.clear();

            Lesson reloaded = entityManager.find(Lesson.class, lessonId);
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getLessonVocabularies()).hasSize(1);

            LessonVocabulary lv = reloaded.getLessonVocabularies().get(0);
            assertThat(lv.getOrderIndex()).isEqualTo(1);
            assertThat(lv.getVocabulary().getHanzi()).isEqualTo("爸爸");
            assertThat(lv.getLesson().getLessonId()).isEqualTo(lessonId);

            // Verify physical table columns via native query
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.getEntityManager()
                    .createNativeQuery("SELECT lesson_id, vocab_id, order_index FROM lesson_vocabulary WHERE lesson_id = :lId")
                    .setParameter("lId", lessonId)
                    .getResultList();

            assertThat(rows).hasSize(1);
            Object[] row = rows.get(0);
            assertThat(((Number) row[0]).longValue()).isEqualTo(lessonId);
            assertThat(((Number) row[1]).longValue()).isEqualTo(vocab.getVocabId());
            assertThat(((Number) row[2]).intValue()).isEqualTo(1);
        }

        @Test
        @DisplayName("GIVEN multiple LessonVocabulary items added out of order WHEN reloaded THEN collection is sorted by order_index ASC")
        void testOrderingByOrderIndex() {
            Account creator = createTestAccount("creator_lesson3@example.com");
            Lesson lesson = new Lesson("Bài 3: Hoa quả", creator);

            Vocabulary vApple = createTestVocabulary("苹果", "píngguǒ", "pingguo");
            Vocabulary vBanana = createTestVocabulary("香蕉", "xiāngjiāo", "xiangjiao");
            Vocabulary vOrange = createTestVocabulary("橙子", "chéngzi", "chengzi");

            // Add out of sequential order: orderIndex 2, then 3, then 1
            lesson.addVocabulary(vBanana, 2);
            lesson.addVocabulary(vOrange, 3);
            lesson.addVocabulary(vApple, 1);

            Lesson saved = entityManager.persistAndFlush(lesson);
            Long lessonId = saved.getLessonId();
            entityManager.clear();

            Lesson reloaded = entityManager.find(Lesson.class, lessonId);
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getLessonVocabularies()).hasSize(3);

            // Assert strict ascending sequence by order_index
            assertThat(reloaded.getLessonVocabularies().get(0).getOrderIndex()).isEqualTo(1);
            assertThat(reloaded.getLessonVocabularies().get(0).getVocabulary().getHanzi()).isEqualTo("苹果");

            assertThat(reloaded.getLessonVocabularies().get(1).getOrderIndex()).isEqualTo(2);
            assertThat(reloaded.getLessonVocabularies().get(1).getVocabulary().getHanzi()).isEqualTo("香蕉");

            assertThat(reloaded.getLessonVocabularies().get(2).getOrderIndex()).isEqualTo(3);
            assertThat(reloaded.getLessonVocabularies().get(2).getVocabulary().getHanzi()).isEqualTo("橙子");
        }

        @Test
        @DisplayName("GIVEN Vocabulary X in Lesson A and Lesson B THEN both associations persist independently without duplicating Vocabulary")
        void testSharedVocabularyAcrossMultipleLessons() {
            Account creator = createTestAccount("creator_lesson4@example.com");
            Vocabulary sharedVocab = createTestVocabulary("学习", "xuéxí", "xuexi");

            Lesson lessonA = new Lesson("Bài 4A", creator);
            lessonA.addVocabulary(sharedVocab, 1);
            entityManager.persist(lessonA);

            Lesson lessonB = new Lesson("Bài 4B", creator);
            lessonB.addVocabulary(sharedVocab, 1);
            entityManager.persist(lessonB);

            entityManager.flush();
            Long idA = lessonA.getLessonId();
            Long idB = lessonB.getLessonId();
            entityManager.clear();

            Lesson reloadedA = entityManager.find(Lesson.class, idA);
            Lesson reloadedB = entityManager.find(Lesson.class, idB);

            assertThat(reloadedA.getLessonVocabularies()).hasSize(1);
            assertThat(reloadedB.getLessonVocabularies()).hasSize(1);
            assertThat(reloadedA.getLessonVocabularies().get(0).getVocabulary().getVocabId())
                    .isEqualTo(sharedVocab.getVocabId());
            assertThat(reloadedB.getLessonVocabularies().get(0).getVocabulary().getVocabId())
                    .isEqualTo(sharedVocab.getVocabId());

            // Check total vocabulary count is still 1
            Number vocabCount = (Number) entityManager.getEntityManager()
                    .createNativeQuery("SELECT count(*) FROM vocabulary WHERE vocab_id = :vId")
                    .setParameter("vId", sharedVocab.getVocabId())
                    .getSingleResult();
            assertThat(vocabCount.intValue()).isEqualTo(1);
        }

        @Test
        @DisplayName("GIVEN Lesson with LessonVocabulary WHEN Lesson is deleted THEN Account and Vocabulary are NOT deleted")
        void testDeleteLessonDoesNotDeleteAccountOrVocabulary() {
            Account creator = createTestAccount("creator_lesson5@example.com");
            Vocabulary vocab = createTestVocabulary("你好", "nǐhǎo", "nihao");

            Lesson lesson = new Lesson("Bài 5", creator);
            lesson.addVocabulary(vocab, 1);
            Lesson saved = entityManager.persistAndFlush(lesson);
            Long lessonId = saved.getLessonId();
            Long creatorId = creator.getAccountId();
            Long vocabId = vocab.getVocabId();
            entityManager.clear();

            Lesson toDelete = entityManager.find(Lesson.class, lessonId);
            entityManager.remove(toDelete);
            entityManager.flush();
            entityManager.clear();

            // Lesson is deleted
            assertThat(entityManager.find(Lesson.class, lessonId)).isNull();

            // Account still exists
            assertThat(entityManager.find(Account.class, creatorId)).isNotNull();

            // Vocabulary still exists
            assertThat(entityManager.find(Vocabulary.class, vocabId)).isNotNull();

            // Junction row is deleted
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.getEntityManager()
                    .createNativeQuery("SELECT lesson_id FROM lesson_vocabulary WHERE lesson_id = :lId")
                    .setParameter("lId", lessonId)
                    .getResultList();
            assertThat(rows).isEmpty();
        }
    }
}
