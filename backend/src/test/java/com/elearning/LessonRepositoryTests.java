package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("Lesson Spring Data JPA Repository Tests (LessonRepository, LessonVocabularyRepository)")
class LessonRepositoryTests {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Account creatorA;
    private Account creatorB;
    private Vocabulary vocabA;
    private Vocabulary vocabB;
    private Vocabulary vocabC;
    private Lesson lesson1;
    private Lesson lesson2;
    private Lesson lesson3;

    @BeforeEach
    void setUp() {
        creatorA = new Account();
        creatorA.setEmailOrPhone("creator_a@example.com");
        creatorA.setPasswordHash("$2a$12$hashA");
        creatorA.setStatus("Active");

        creatorB = new Account();
        creatorB.setEmailOrPhone("creator_b@example.com");
        creatorB.setPasswordHash("$2a$12$hashB");
        creatorB.setStatus("Active");

        entityManager.persist(creatorA);
        entityManager.persist(creatorB);

        vocabA = new Vocabulary("书", "shū", "shu", "Thư", "Sách");
        vocabB = new Vocabulary("笔", "bǐ", "bi", "Bút", "Bút");
        vocabC = new Vocabulary("纸", "zhǐ", "zhi", "Chỉ", "Giấy");
        entityManager.persist(vocabA);
        entityManager.persist(vocabB);
        entityManager.persist(vocabC);

        // Lesson 1: owned by creatorA, status "Draft"
        // Words added out of order: A (orderIndex=2), B (orderIndex=1), C (orderIndex=3)
        lesson1 = new Lesson("Bài học 1 - Đồ dùng học tập", creatorA);
        lesson1.setStatus("Draft");
        lesson1.addVocabulary(vocabA, 2);
        lesson1.addVocabulary(vocabB, 1);
        lesson1.addVocabulary(vocabC, 3);
        entityManager.persist(lesson1);

        // Lesson 2: owned by creatorA, status "Approved", no vocabularies
        lesson2 = new Lesson("Bài học 2 - Nâng cao", creatorA);
        lesson2.setStatus("Approved");
        entityManager.persist(lesson2);

        // Lesson 3: owned by creatorB, status "Pending", shares vocabA (orderIndex=1)
        lesson3 = new Lesson("Bài học 3 - Nhập môn", creatorB);
        lesson3.setStatus("Pending");
        lesson3.addVocabulary(vocabA, 1);
        entityManager.persist(lesson3);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("LessonRepository Tests")
    class LessonRepositoryQueryTests {

        @Test
        @DisplayName("GIVEN lessons by multiple creators WHEN findByCreatedBy THEN only lessons belonging to target creator are returned")
        void testFindByCreatedBy_isolation() {
            Account reloadedA = entityManager.find(Account.class, creatorA.getAccountId());
            Account reloadedB = entityManager.find(Account.class, creatorB.getAccountId());

            Page<Lesson> pageA = lessonRepository.findByCreatedBy(reloadedA, PageRequest.of(0, 10));
            assertThat(pageA).isNotNull();
            assertThat(pageA.getTotalElements()).isEqualTo(2);
            assertThat(pageA.getContent()).extracting(Lesson::getTitle)
                    .containsExactlyInAnyOrder("Bài học 1 - Đồ dùng học tập", "Bài học 2 - Nâng cao");

            Page<Lesson> pageB = lessonRepository.findByCreatedBy(reloadedB, PageRequest.of(0, 10));
            assertThat(pageB).isNotNull();
            assertThat(pageB.getTotalElements()).isEqualTo(1);
            assertThat(pageB.getContent().get(0).getTitle()).isEqualTo("Bài học 3 - Nhập môn");
        }

        @Test
        @DisplayName("GIVEN lessons with Draft/Approved/Pending statuses WHEN findByStatus THEN only matching status is returned")
        void testFindByStatus_isolation() {
            Page<Lesson> draftPage = lessonRepository.findByStatus("Draft", PageRequest.of(0, 10));
            assertThat(draftPage.getTotalElements()).isEqualTo(1);
            assertThat(draftPage.getContent().get(0).getTitle()).isEqualTo("Bài học 1 - Đồ dùng học tập");

            Page<Lesson> approvedPage = lessonRepository.findByStatus("Approved", PageRequest.of(0, 10));
            assertThat(approvedPage.getTotalElements()).isEqualTo(1);
            assertThat(approvedPage.getContent().get(0).getTitle()).isEqualTo("Bài học 2 - Nâng cao");

            Page<Lesson> pendingPage = lessonRepository.findByStatus("Pending", PageRequest.of(0, 10));
            assertThat(pendingPage.getTotalElements()).isEqualTo(1);
            assertThat(pendingPage.getContent().get(0).getTitle()).isEqualTo("Bài học 3 - Nhập môn");
        }

        @Test
        @DisplayName("GIVEN creator and status filters WHEN findByCreatedByAndStatus THEN returns precise intersection")
        void testFindByCreatedByAndStatus() {
            Account reloadedA = entityManager.find(Account.class, creatorA.getAccountId());

            Page<Lesson> draftPage = lessonRepository.findByCreatedByAndStatus(reloadedA, "Draft", PageRequest.of(0, 10));
            assertThat(draftPage.getTotalElements()).isEqualTo(1);
            assertThat(draftPage.getContent().get(0).getTitle()).isEqualTo("Bài học 1 - Đồ dùng học tập");

            Page<Lesson> approvedPage = lessonRepository.findByCreatedByAndStatus(reloadedA, "Approved", PageRequest.of(0, 10));
            assertThat(approvedPage.getTotalElements()).isEqualTo(1);
            assertThat(approvedPage.getContent().get(0).getTitle()).isEqualTo("Bài học 2 - Nâng cao");

            Page<Lesson> pendingPage = lessonRepository.findByCreatedByAndStatus(reloadedA, "Pending", PageRequest.of(0, 10));
            assertThat(pendingPage.getTotalElements()).isZero();
            assertThat(pendingPage.getContent()).isEmpty();
        }

        @Test
        @DisplayName("GIVEN unmatching query criteria WHEN querying THEN returns empty page, not null")
        void testEmptyResults() {
            Account nonCreator = new Account();
            nonCreator.setEmailOrPhone("non_creator@example.com");
            nonCreator.setPasswordHash("$2a$12$hashNon");
            nonCreator.setStatus("Active");
            entityManager.persistAndFlush(nonCreator);
            entityManager.clear();

            Page<Lesson> emptyByOwner = lessonRepository.findByCreatedBy(nonCreator, PageRequest.of(0, 10));
            assertThat(emptyByOwner).isNotNull();
            assertThat(emptyByOwner.getTotalElements()).isZero();
            assertThat(emptyByOwner.getContent()).isEmpty();

            Page<Lesson> emptyByStatus = lessonRepository.findByStatus("Rejected", PageRequest.of(0, 10));
            assertThat(emptyByStatus).isNotNull();
            assertThat(emptyByStatus.getTotalElements()).isZero();
            assertThat(emptyByStatus.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("LessonVocabularyRepository Tests")
    class LessonVocabularyRepositoryQueryTests {

        @Test
        @DisplayName("GIVEN Lesson with items added out of order WHEN findByLessonOrderByOrderIndexAsc THEN strictly sorted 1, 2, 3")
        void testLessonVocabulary_OrderByOrderIndexAsc() {
            Lesson reloadedLesson1 = entityManager.find(Lesson.class, lesson1.getLessonId());

            List<LessonVocabulary> byEntity = lessonVocabularyRepository.findByLessonOrderByOrderIndexAsc(reloadedLesson1);
            assertThat(byEntity).hasSize(3);
            assertThat(byEntity.get(0).getOrderIndex()).isEqualTo(1);
            assertThat(byEntity.get(0).getVocabulary().getHanzi()).isEqualTo("笔");

            assertThat(byEntity.get(1).getOrderIndex()).isEqualTo(2);
            assertThat(byEntity.get(1).getVocabulary().getHanzi()).isEqualTo("书");

            assertThat(byEntity.get(2).getOrderIndex()).isEqualTo(3);
            assertThat(byEntity.get(2).getVocabulary().getHanzi()).isEqualTo("纸");

            // Also verify query by lessonId navigation
            List<LessonVocabulary> byId = lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(lesson1.getLessonId());
            assertThat(byId).hasSize(3);
            assertThat(byId.get(0).getOrderIndex()).isEqualTo(1);
            assertThat(byId.get(0).getVocabulary().getHanzi()).isEqualTo("笔");
            assertThat(byId.get(1).getOrderIndex()).isEqualTo(2);
            assertThat(byId.get(1).getVocabulary().getHanzi()).isEqualTo("书");
            assertThat(byId.get(2).getOrderIndex()).isEqualTo(3);
            assertThat(byId.get(2).getVocabulary().getHanzi()).isEqualTo("纸");
        }

        @Test
        @DisplayName("GIVEN shared Vocabulary across lessons WHEN querying each lesson THEN isolation is strictly preserved")
        void testLessonVocabulary_IsolationAndSharedVocabulary() {
            Lesson reloadedLesson1 = entityManager.find(Lesson.class, lesson1.getLessonId());
            Lesson reloadedLesson3 = entityManager.find(Lesson.class, lesson3.getLessonId());

            List<LessonVocabulary> list1 = lessonVocabularyRepository.findByLessonOrderByOrderIndexAsc(reloadedLesson1);
            List<LessonVocabulary> list3 = lessonVocabularyRepository.findByLessonOrderByOrderIndexAsc(reloadedLesson3);

            assertThat(list1).hasSize(3);
            assertThat(list3).hasSize(1);

            // Lesson 3 only has vocabA ("书") with orderIndex = 1
            assertThat(list3.get(0).getVocabulary().getHanzi()).isEqualTo("书");
            assertThat(list3.get(0).getOrderIndex()).isEqualTo(1);

            // Verify both point to the same shared vocabulary ID
            assertThat(list1.get(1).getVocabulary().getVocabId()).isEqualTo(vocabA.getVocabId());
            assertThat(list3.get(0).getVocabulary().getVocabId()).isEqualTo(vocabA.getVocabId());
        }

        @Test
        @DisplayName("GIVEN lesson with no vocabulary WHEN querying THEN returns empty list, not null")
        void testLessonVocabulary_EmptyResults() {
            Lesson reloadedLesson2 = entityManager.find(Lesson.class, lesson2.getLessonId());

            List<LessonVocabulary> emptyList = lessonVocabularyRepository.findByLessonOrderByOrderIndexAsc(reloadedLesson2);
            assertThat(emptyList).isNotNull();
            assertThat(emptyList).isEmpty();

            List<LessonVocabulary> emptyById = lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(999999L);
            assertThat(emptyById).isNotNull();
            assertThat(emptyById).isEmpty();
        }
    }
}
