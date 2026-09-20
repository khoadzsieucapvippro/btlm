package com.elearning;

import com.elearning.dto.response.DueCardResponse;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Radical;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.CardProgressRepository;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.ReviewLogRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.SrsService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance and Query-Count Integration Tests for BE-PERF-001.
 * Verifies on real MySQL 8.4 Testcontainers that SrsServiceImpl.getDueCards()
 * eliminates N+1 item hydration queries and scales sublinearly / with O(1) bounded queries.
 */
@SpringBootTest
@DisplayName("BE-PERF-001: SRS getDueCards() Query-Count & Polymorphic Hydration Performance Integration Tests")
class SrsPerformanceIntegrationTests {

    @Autowired
    private SrsService srsService;

    @Autowired
    private CardProgressRepository cardProgressRepository;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    @Autowired
    private UserSrsSettingRepository userSrsSettingRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private RadicalRepository radicalRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private UserProfile testLearner;
    private String userEmail;
    private List<Vocabulary> createdVocabularies = new ArrayList<>();
    private List<Radical> createdRadicals = new ArrayList<>();
    private Statistics hibernateStats;

    @BeforeEach
    void setUp() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        hibernateStats = sessionFactory.getStatistics();
        hibernateStats.setStatisticsEnabled(true);
        hibernateStats.clear();

        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        userEmail = "learner_perf_" + uniqueSuffix + "@test.com";

        Account account = new Account();
        account.setEmailOrPhone(userEmail);
        account.setPasswordHash("passwordHash");
        account.setStatus("Active");
        if (learnerRole != null) {
            account.getRoles().add(learnerRole);
        }
        account = accountRepository.save(account);

        UserProfile profile = new UserProfile();
        profile.setAccount(account);
        profile.setFullName("Learner Perf " + uniqueSuffix);
        testLearner = userProfileRepository.save(profile);

        UserSrsSetting setting = new UserSrsSetting(testLearner, 50, 100);
        userSrsSettingRepository.save(setting);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userEmail, "password", List.of(new SimpleGrantedAuthority("ROLE_LEARNER")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (testLearner != null) {
            userSrsSettingRepository.findByUser(testLearner).ifPresent(userSrsSettingRepository::delete);
            reviewLogRepository.deleteAll(reviewLogRepository.findByUserOrderByReviewedAtDesc(testLearner));
            cardProgressRepository.deleteAll(cardProgressRepository.findByUserAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(testLearner, LocalDateTime.now().plusYears(10), org.springframework.data.domain.Pageable.unpaged()));
            userProfileRepository.delete(testLearner);
        }
        if (!createdVocabularies.isEmpty()) {
            vocabularyRepository.deleteAll(createdVocabularies);
            createdVocabularies.clear();
        }
        if (!createdRadicals.isEmpty()) {
            radicalRepository.deleteAll(createdRadicals);
            createdRadicals.clear();
        }
    }

    @Test
    @DisplayName("BE-PERF-001: GIVEN 10 mixed due cards (6 Vocabulary + 4 Radicals) WHEN getDueCards() executes THEN item hydration uses <= 2 bulk queries instead of 10 N+1 queries")
    void testGetDueCards_mixedPolymorphic10Cards_executesBoundedBulkQueries() {
        // 1. Seed 6 vocabularies and 4 radicals
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        List<CardProgress> progressList = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            Vocabulary v = vocabularyRepository.save(new Vocabulary("字" + suffix + i, "zì" + i, "zi" + suffix + i, "Tự " + i, "Chữ " + i));
            createdVocabularies.add(v);
            CardProgress cp = new CardProgress(testLearner, "VOCABULARY", v.getVocabId(), new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusMinutes(10 * i));
            progressList.add(cp);
        }

        // Use pre-existing radicals from seed (radicals 1 to 4)
        for (int i = 1; i <= 4; i++) {
            int radId = i;
            Radical r = radicalRepository.findById(radId).orElseGet(() -> {
                Radical newR = new Radical(radId, "部" + radId, "bù", "Bộ", "Bộ thủ " + radId);
                return radicalRepository.save(newR);
            });
            CardProgress cp = new CardProgress(testLearner, "RADICAL", r.getRadicalId().longValue(), new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusMinutes(50 + 10 * i));
            progressList.add(cp);
        }

        cardProgressRepository.saveAllAndFlush(progressList);

        // 2. Clear Hibernate statistics to measure only getDueCards() queries
        hibernateStats.clear();
        long statementsBefore = hibernateStats.getPrepareStatementCount();

        // 3. Execute getDueCards()
        List<DueCardResponse> results = srsService.getDueCards(null, 20);

        // 4. Measure executed statements
        long statementsAfter = hibernateStats.getPrepareStatementCount();
        long queryCount = statementsAfter - statementsBefore;

        // 5. Verify results correctness
        assertThat(results)
                .as("Must return all 10 due cards")
                .hasSize(10);

        long vocabCount = results.stream().filter(r -> "VOCABULARY".equals(r.getItemType())).count();
        long radicalCount = results.stream().filter(r -> "RADICAL".equals(r.getItemType())).count();
        assertThat(vocabCount).isEqualTo(6);
        assertThat(radicalCount).isEqualTo(4);

        // 6. Query Count Invariant:
        // Before optimization: 1 user + 1 setting + 1 review_count + 1 card_progress + 10 individual item queries = 14 queries.
        // After optimization: 1 user + 1 setting + 1 review_count + 1 card_progress + 1 vocab bulk + 1 radical bulk = 6 queries max.
        // Item hydration queries must be <= 2 (1 bulk for Vocabulary, 1 bulk for Radical).
        assertThat(queryCount)
                .as("Total database queries for 10 due cards must be bounded (<= 6 total, exactly 2 for item hydration, NOT 1 + 10 = 14)")
                .isLessThanOrEqualTo(6);
    }

    @Test
    @DisplayName("BE-PERF-001: Query count scales sublinearly / with O(1) item queries when due cards increase from 5 to 15")
    void testGetDueCards_queryCountScalesSublinearlyWithN() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);

        // --- PHASE A: Seed 5 cards ---
        List<CardProgress> list5 = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Vocabulary v = vocabularyRepository.save(new Vocabulary("甲" + suffix + i, "jiǎ", "jia" + suffix + i, "Giáp", "Giáp " + i));
            createdVocabularies.add(v);
            list5.add(new CardProgress(testLearner, "VOCABULARY", v.getVocabId(), new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusMinutes(i)));
        }
        for (int i = 1; i <= 2; i++) {
            list5.add(new CardProgress(testLearner, "RADICAL", (long) i, new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusMinutes(10 + i)));
        }
        cardProgressRepository.saveAllAndFlush(list5);

        hibernateStats.clear();
        List<DueCardResponse> resp5 = srsService.getDueCards(null, 5);
        long queriesFor5 = hibernateStats.getPrepareStatementCount();
        assertThat(resp5).hasSize(5);

        // --- PHASE B: Seed 10 more cards (total 15 cards) ---
        List<CardProgress> list10More = new ArrayList<>();
        for (int i = 4; i <= 10; i++) {
            Vocabulary v = vocabularyRepository.save(new Vocabulary("乙" + suffix + i, "yǐ", "yi" + suffix + i, "Ất", "Ất " + i));
            createdVocabularies.add(v);
            list10More.add(new CardProgress(testLearner, "VOCABULARY", v.getVocabId(), new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusMinutes(20 + i)));
        }
        for (int i = 3; i <= 5; i++) {
            list10More.add(new CardProgress(testLearner, "RADICAL", (long) i, new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusMinutes(40 + i)));
        }
        cardProgressRepository.saveAllAndFlush(list10More);

        hibernateStats.clear();
        List<DueCardResponse> resp15 = srsService.getDueCards(null, 15);
        long queriesFor15 = hibernateStats.getPrepareStatementCount();
        assertThat(resp15).hasSize(15);

        // Query count for 15 cards MUST NOT scale proportionally with N (15 != 5 * 3)
        // Both 5 cards and 15 cards must execute the same bounded number of item hydration queries (<= 6 total)
        assertThat(queriesFor15)
                .as("Query count for N=15 must be bounded and identical to N=5 (<= 6), proving O(1) query complexity for item hydration")
                .isEqualTo(queriesFor5)
                .isLessThanOrEqualTo(6);
    }

    @Test
    @DisplayName("BE-PERF-001: Preserves exact chronological nextReviewAt order when reconstructing polymorphic items")
    void testGetDueCards_preservesChronologicalOrder() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);

        Vocabulary v1 = vocabularyRepository.save(new Vocabulary("天" + suffix, "tiān", "tian" + suffix, "Thiên", "Trời"));
        Vocabulary v2 = vocabularyRepository.save(new Vocabulary("地" + suffix, "dì", "di" + suffix, "Địa", "Đất"));
        createdVocabularies.addAll(List.of(v1, v2));

        // Interleaved due times: V1 (oldest) -> R1 -> V2 -> R2 (newest)
        LocalDateTime t1 = LocalDateTime.now().minusMinutes(40);
        LocalDateTime t2 = LocalDateTime.now().minusMinutes(30);
        LocalDateTime t3 = LocalDateTime.now().minusMinutes(20);
        LocalDateTime t4 = LocalDateTime.now().minusMinutes(10);

        CardProgress cp1 = new CardProgress(testLearner, "VOCABULARY", v1.getVocabId(), new BigDecimal("2.50"), 1, 1, t1);
        CardProgress cp2 = new CardProgress(testLearner, "RADICAL", 1L, new BigDecimal("2.50"), 1, 1, t2);
        CardProgress cp3 = new CardProgress(testLearner, "VOCABULARY", v2.getVocabId(), new BigDecimal("2.50"), 1, 1, t3);
        CardProgress cp4 = new CardProgress(testLearner, "RADICAL", 2L, new BigDecimal("2.50"), 1, 1, t4);

        cardProgressRepository.saveAllAndFlush(List.of(cp1, cp2, cp3, cp4));

        List<DueCardResponse> results = srsService.getDueCards(null, 10);

        assertThat(results).hasSize(4);
        assertThat(results.get(0).getItemId()).isEqualTo(v1.getVocabId());
        assertThat(results.get(0).getItemType()).isEqualTo("VOCABULARY");

        assertThat(results.get(1).getItemId()).isEqualTo(1L);
        assertThat(results.get(1).getItemType()).isEqualTo("RADICAL");

        assertThat(results.get(2).getItemId()).isEqualTo(v2.getVocabId());
        assertThat(results.get(2).getItemType()).isEqualTo("VOCABULARY");

        assertThat(results.get(3).getItemId()).isEqualTo(2L);
        assertThat(results.get(3).getItemType()).isEqualTo("RADICAL");
    }

    @Test
    @DisplayName("BE-PERF-001: GIVEN Vocabulary-only due cards WHEN getDueCards() executes THEN exactly 1 item bulk query (0 for radicals)")
    void testGetDueCards_allVocabulary_executesSingleItemBulkQuery() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        List<CardProgress> list = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            Vocabulary v = vocabularyRepository.save(new Vocabulary("词" + suffix + i, "cí", "ci" + suffix + i, "Từ", "Từ " + i));
            createdVocabularies.add(v);
            list.add(new CardProgress(testLearner, "VOCABULARY", v.getVocabId(), new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusMinutes(i)));
        }
        cardProgressRepository.saveAllAndFlush(list);

        hibernateStats.clear();
        List<DueCardResponse> results = srsService.getDueCards(null, 10);
        long queries = hibernateStats.getPrepareStatementCount();

        assertThat(results).hasSize(5);
        assertThat(results).allMatch(r -> "VOCABULARY".equals(r.getItemType()));
        assertThat(queries).isLessThanOrEqualTo(5); // 1 user + 1 setting + 1 count + 1 cardProgress + 1 vocab bulk = 5
    }
}
