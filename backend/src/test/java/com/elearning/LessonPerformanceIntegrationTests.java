package com.elearning;

import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Role;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.CreatorLessonService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance and Query-Count Integration Tests for BE-PERF-002.
 * Verifies on real MySQL 8.4 Testcontainers that CreatorLessonServiceImpl.getMyLessons()
 * eliminates N+1 vocabulary count queries and operates with O(1) bounded queries.
 */
@SpringBootTest
@DisplayName("BE-PERF-002: Creator getMyLessons() Query-Count & Performance Integration Tests")
class LessonPerformanceIntegrationTests {

    @Autowired
    private CreatorLessonService creatorLessonService;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics hibernateStats;
    private List<Vocabulary> testVocabularies = new ArrayList<>();
    private List<Account> createdAccounts = new ArrayList<>();
    private List<Lesson> createdLessons = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        hibernateStats = sessionFactory.getStatistics();
        hibernateStats.setStatisticsEnabled(true);
        hibernateStats.clear();

        // Seed 5 vocabularies for association
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        testVocabularies.add(vocabularyRepository.save(new Vocabulary("字" + suffix, "zì", "zi", "Tự", "Chữ Hán 1")));
        testVocabularies.add(vocabularyRepository.save(new Vocabulary("学" + suffix, "xué", "xue", "Học", "Học tập 2")));
        testVocabularies.add(vocabularyRepository.save(new Vocabulary("校" + suffix, "xiào", "xiao", "Hiệu", "Trường học 3")));
        testVocabularies.add(vocabularyRepository.save(new Vocabulary("生" + suffix, "shēng", "sheng", "Sinh", "Học sinh 4")));
        testVocabularies.add(vocabularyRepository.save(new Vocabulary("师" + suffix, "shī", "shi", "Sư", "Thầy giáo 5")));
        vocabularyRepository.flush();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();

        // Clean up created entities in reverse dependency order
        for (Lesson lesson : createdLessons) {
            List<LessonVocabulary> lvs = lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(lesson.getLessonId());
            if (!lvs.isEmpty()) {
                lessonVocabularyRepository.deleteAll(lvs);
            }
        }
        lessonVocabularyRepository.flush();

        if (!createdLessons.isEmpty()) {
            lessonRepository.deleteAll(createdLessons);
            lessonRepository.flush();
            createdLessons.clear();
        }

        if (!createdAccounts.isEmpty()) {
            accountRepository.deleteAll(createdAccounts);
            accountRepository.flush();
            createdAccounts.clear();
        }

        if (!testVocabularies.isEmpty()) {
            vocabularyRepository.deleteAll(testVocabularies);
            vocabularyRepository.flush();
            testVocabularies.clear();
        }

        if (hibernateStats != null) {
            hibernateStats.clear();
        }
    }

    private Account createCreator(String emailPrefix) {
        String uniqueEmail = emailPrefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        Role creatorRole = roleRepository.findByRoleName("Creator").orElse(null);

        Account account = new Account();
        account.setEmailOrPhone(uniqueEmail);
        account.setPasswordHash("$2a$12$dummyhashforperfcreator1234567890123456");
        account.setStatus("Active");
        if (creatorRole != null) {
            account.getRoles().add(creatorRole);
        }
        account = accountRepository.saveAndFlush(account);
        createdAccounts.add(account);
        return account;
    }

    private void setAuthentication(String email) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, "N/A", List.of(new SimpleGrantedAuthority("ROLE_CREATOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GIVEN 10 lessons for creator WHEN getMyLessons THEN executes bounded queries (eliminated N+1) and accurately counts vocabularies")
    void test1_getMyLessons_boundedQueryCountFor10Lessons() {
        Account creator = createCreator("creator_perf_10");

        // Create 10 lessons with realistic distribution:
        // Lessons 1-4: 3 vocabularies each
        // Lessons 5-7: 1 vocabulary each
        // Lessons 8-10: 0 vocabularies each (Draft empty lessons)
        List<Lesson> lessons = new ArrayList<>();
        List<LessonVocabulary> allLvs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Lesson lesson = new Lesson("Bài học số " + i, creator);
            lesson.setStatus("Draft");
            lesson = lessonRepository.save(lesson);
            lessons.add(lesson);
            createdLessons.add(lesson);

            int vocabCount = (i <= 4) ? 3 : (i <= 7) ? 1 : 0;
            for (int v = 0; v < vocabCount; v++) {
                allLvs.add(new LessonVocabulary(lesson, testVocabularies.get(v), v + 1));
            }
        }
        lessonRepository.flush();
        if (!allLvs.isEmpty()) {
            lessonVocabularyRepository.saveAllAndFlush(allLvs);
        }

        setAuthentication(creator.getEmailOrPhone());

        // Reset query statistics before invoking target method
        hibernateStats.clear();
        long statementsBefore = hibernateStats.getPrepareStatementCount();

        Page<LessonSummaryResponse> page = creatorLessonService.getMyLessons(PageRequest.of(0, 20));

        long statementsAfter = hibernateStats.getPrepareStatementCount();
        long executedQueries = statementsAfter - statementsBefore;

        // Query count assertions:
        // In the legacy N+1 implementation: 1 account query + 1 lesson page query + 1 count query + 10 countByLesson queries = 13 queries.
        // In the optimized implementation: 1 account query + 1 lesson page query + 1 count query + 1 bulk count query = exactly 4 queries.
        assertThat(executedQueries)
                .as("Query count must be bounded to <= 4 (N+1 eliminated, was 13 queries in legacy implementation)")
                .isLessThanOrEqualTo(4);

        // Verification of data correctness and ordering
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getContent()).hasSize(10);

        for (int i = 0; i < 10; i++) {
            LessonSummaryResponse item = page.getContent().get(i);
            assertThat(item.getLessonId()).isEqualTo(lessons.get(i).getLessonId());
            assertThat(item.getTitle()).isEqualTo("Bài học số " + (i + 1));

            int expectedCount = (i < 4) ? 3 : (i < 7) ? 1 : 0;
            assertThat(item.getVocabularyCount())
                    .as("Vocabulary count for lesson " + item.getTitle())
                    .isEqualTo(expectedCount);
        }
    }

    @Test
    @DisplayName("GIVEN 5 vs 15 lessons WHEN getMyLessons THEN query count remains constant and scaling-invariant")
    void test2_getMyLessons_scalingInvarianceBetween5And15Lessons() {
        // Setup Creator 1 with 5 lessons
        Account creator5 = createCreator("creator_scale_5");
        List<LessonVocabulary> lvs5 = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Lesson lesson = lessonRepository.save(new Lesson("Bài 5_" + i, creator5));
            createdLessons.add(lesson);
            lvs5.add(new LessonVocabulary(lesson, testVocabularies.get(0), 1));
            lvs5.add(new LessonVocabulary(lesson, testVocabularies.get(1), 2));
        }
        lessonRepository.flush();
        lessonVocabularyRepository.saveAllAndFlush(lvs5);

        // Setup Creator 2 with 15 lessons
        Account creator15 = createCreator("creator_scale_15");
        List<LessonVocabulary> lvs15 = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Lesson lesson = lessonRepository.save(new Lesson("Bài 15_" + i, creator15));
            createdLessons.add(lesson);
            lvs15.add(new LessonVocabulary(lesson, testVocabularies.get(0), 1));
            lvs15.add(new LessonVocabulary(lesson, testVocabularies.get(1), 2));
        }
        lessonRepository.flush();
        lessonVocabularyRepository.saveAllAndFlush(lvs15);

        // Measure query count for 5 lessons
        setAuthentication(creator5.getEmailOrPhone());
        hibernateStats.clear();
        Page<LessonSummaryResponse> page5 = creatorLessonService.getMyLessons(PageRequest.of(0, 20));
        long queriesFor5 = hibernateStats.getPrepareStatementCount();

        // Measure query count for 15 lessons
        setAuthentication(creator15.getEmailOrPhone());
        hibernateStats.clear();
        Page<LessonSummaryResponse> page15 = creatorLessonService.getMyLessons(PageRequest.of(0, 20));
        long queriesFor15 = hibernateStats.getPrepareStatementCount();

        assertThat(page5.getContent()).hasSize(5);
        assertThat(page15.getContent()).hasSize(15);

        // Assert query count is invariant between 5 and 15 lessons
        assertThat(queriesFor15)
                .as("Query count for 15 lessons must equal query count for 5 lessons (O(1) query complexity)")
                .isEqualTo(queriesFor5);
        assertThat(queriesFor5).isLessThanOrEqualTo(4);
    }

    @Test
    @DisplayName("GIVEN creator with zero-count and mixed lessons WHEN getMyLessons THEN returns vocabularyCount = 0 for empty lessons")
    void test3_getMyLessons_zeroVocabularyCountDefensiveHandling() {
        Account creator = createCreator("creator_zero_test");

        Lesson emptyLesson = lessonRepository.save(new Lesson("Bài Trống", creator));
        Lesson populatedLesson = lessonRepository.save(new Lesson("Bài Có Từ Vựng", creator));
        createdLessons.add(emptyLesson);
        createdLessons.add(populatedLesson);
        lessonRepository.flush();

        List<LessonVocabulary> lvs = List.of(
                new LessonVocabulary(populatedLesson, testVocabularies.get(0), 1),
                new LessonVocabulary(populatedLesson, testVocabularies.get(1), 2)
        );
        lessonVocabularyRepository.saveAllAndFlush(lvs);

        setAuthentication(creator.getEmailOrPhone());
        hibernateStats.clear();

        Page<LessonSummaryResponse> page = creatorLessonService.getMyLessons(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        LessonSummaryResponse emptySummary = page.getContent().stream()
                .filter(l -> l.getLessonId().equals(emptyLesson.getLessonId()))
                .findFirst().orElseThrow();
        LessonSummaryResponse populatedSummary = page.getContent().stream()
                .filter(l -> l.getLessonId().equals(populatedLesson.getLessonId()))
                .findFirst().orElseThrow();

        assertThat(emptySummary.getVocabularyCount()).isEqualTo(0);
        assertThat(populatedSummary.getVocabularyCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("GIVEN multiple creators WHEN Creator A calls getMyLessons THEN only Creator A's lessons are returned and counted")
    void test4_getMyLessons_creatorOwnershipAndCountIsolation() {
        Account creatorA = createCreator("creator_isolation_a");
        Account creatorB = createCreator("creator_isolation_b");

        Lesson lessonA = lessonRepository.save(new Lesson("Bài của A", creatorA));
        Lesson lessonB = lessonRepository.save(new Lesson("Bài của B", creatorB));
        createdLessons.add(lessonA);
        createdLessons.add(lessonB);
        lessonRepository.flush();

        List<LessonVocabulary> lvs = List.of(
                new LessonVocabulary(lessonA, testVocabularies.get(0), 1),
                new LessonVocabulary(lessonB, testVocabularies.get(0), 1),
                new LessonVocabulary(lessonB, testVocabularies.get(1), 2),
                new LessonVocabulary(lessonB, testVocabularies.get(2), 3)
        );
        lessonVocabularyRepository.saveAllAndFlush(lvs);

        setAuthentication(creatorA.getEmailOrPhone());
        Page<LessonSummaryResponse> pageA = creatorLessonService.getMyLessons(PageRequest.of(0, 10));

        assertThat(pageA.getContent()).hasSize(1);
        assertThat(pageA.getContent().get(0).getLessonId()).isEqualTo(lessonA.getLessonId());
        assertThat(pageA.getContent().get(0).getTitle()).isEqualTo("Bài của A");
        assertThat(pageA.getContent().get(0).getVocabularyCount()).isEqualTo(1);

        setAuthentication(creatorB.getEmailOrPhone());
        Page<LessonSummaryResponse> pageB = creatorLessonService.getMyLessons(PageRequest.of(0, 10));

        assertThat(pageB.getContent()).hasSize(1);
        assertThat(pageB.getContent().get(0).getLessonId()).isEqualTo(lessonB.getLessonId());
        assertThat(pageB.getContent().get(0).getTitle()).isEqualTo("Bài của B");
        assertThat(pageB.getContent().get(0).getVocabularyCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("GIVEN 15 lessons WHEN paginating page 0 and page 1 (size 5) THEN preserves pagination metadata and bounded queries")
    void test5_getMyLessons_paginationPreservesPageSizeAndMetadata() {
        Account creator = createCreator("creator_pagination_test");

        List<LessonVocabulary> lvs = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Lesson lesson = lessonRepository.save(new Lesson("Bài Trang " + i, creator));
            createdLessons.add(lesson);
            lvs.add(new LessonVocabulary(lesson, testVocabularies.get(0), 1));
        }
        lessonRepository.flush();
        lessonVocabularyRepository.saveAllAndFlush(lvs);

        setAuthentication(creator.getEmailOrPhone());

        // Page 0 (size 5)
        hibernateStats.clear();
        Page<LessonSummaryResponse> page0 = creatorLessonService.getMyLessons(PageRequest.of(0, 5));
        long queriesPage0 = hibernateStats.getPrepareStatementCount();

        assertThat(page0.getNumber()).isEqualTo(0);
        assertThat(page0.getSize()).isEqualTo(5);
        assertThat(page0.getTotalElements()).isEqualTo(15);
        assertThat(page0.getTotalPages()).isEqualTo(3);
        assertThat(page0.getContent()).hasSize(5);
        assertThat(queriesPage0)
                .as("Page 0 query count must be bounded (<= 5 queries, eliminating per-lesson N+1)")
                .isLessThanOrEqualTo(5);

        // Page 1 (size 5)
        hibernateStats.clear();
        Page<LessonSummaryResponse> page1 = creatorLessonService.getMyLessons(PageRequest.of(1, 5));
        long queriesPage1 = hibernateStats.getPrepareStatementCount();

        assertThat(page1.getNumber()).isEqualTo(1);
        assertThat(page1.getContent()).hasSize(5);
        assertThat(queriesPage1)
                .as("Page 1 query count must be bounded (<= 5 queries, eliminating per-lesson N+1)")
                .isLessThanOrEqualTo(5);
        assertThat(queriesPage1).isEqualTo(queriesPage0);
    }

    @Test
    @DisplayName("GIVEN creator with 0 lessons WHEN getMyLessons THEN returns empty page without executing count aggregation")
    void test6_getMyLessons_emptyLessonsReturnsEmptyPageWithoutQueryAmplification() {
        Account emptyCreator = createCreator("creator_empty_test");
        setAuthentication(emptyCreator.getEmailOrPhone());

        hibernateStats.clear();
        Page<LessonSummaryResponse> page = creatorLessonService.getMyLessons(PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
        // Only account lookup (1) + lesson page query (1) + count query (1) = <= 3 queries, no bulk count query executed
        assertThat(hibernateStats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }
}
