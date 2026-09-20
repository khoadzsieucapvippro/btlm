package com.elearning;

import com.elearning.config.TimeConfig;
import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.dto.response.StudyStatsResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.CardProgressRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.ReviewLogRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.SrsService;
import com.elearning.service.impl.SrsServiceImpl;
import com.elearning.service.srs.SrsCalculator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test suite on real MySQL Testcontainers for R1: Lesson-Driven New Card Engine.
 * Tests candidate retrieval, ordering, daily new-card quota enforcement, cross-lesson deduplication,
 * and date-boundary rollover on actual database state.
 */
@SpringBootTest
@DisplayName("R1 — Lesson-Driven New Card Engine Integration Tests")
class SrsNewCardIntegrationTests {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of(TimeConfig.DEFAULT_BUSINESS_TIMEZONE);

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
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;
    private UserProfile testLearner;
    private Lesson approvedLesson1;
    private Lesson approvedLesson2;
    private Lesson draftLesson;
    private Vocabulary vocabA;
    private Vocabulary vocabB;
    private Vocabulary vocabC;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);

        txTemplate.execute(status -> {
            Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            Account account = new Account();
            account.setEmailOrPhone("learner_r1_" + suffix + "@test.com");
            account.setPasswordHash("hash123");
            account.setStatus("Active");
            if (learnerRole != null) {
                account.getRoles().add(learnerRole);
            }
            account = accountRepository.save(account);

            testLearner = new UserProfile();
            testLearner.setAccount(account);
            testLearner.setFullName("Learner R1 " + suffix);
            testLearner = userProfileRepository.save(testLearner);

            UserSrsSetting setting = new UserSrsSetting();
            setting.setUser(testLearner);
            setting.setNewCardsPerDay(2); // limit 2 new cards per day
            setting.setMaxReviewPerDay(100);
            userSrsSettingRepository.save(setting);

            // Create test vocabularies with unique suffix to avoid cross-test collisions
            vocabA = vocabularyRepository.save(new Vocabulary("你" + suffix, "nǐ", "ni_" + suffix, "Nhĩ", "Bạn"));
            vocabB = vocabularyRepository.save(new Vocabulary("好" + suffix, "hǎo", "hao_" + suffix, "Hảo", "Tốt"));
            vocabC = vocabularyRepository.save(new Vocabulary("学" + suffix, "xué", "xue_" + suffix, "Học", "Học tập"));

            // Create Approved Lesson 1 containing vocabA (order 1) and vocabB (order 2)
            approvedLesson1 = new Lesson("Approved Lesson 1 " + suffix, account);
            approvedLesson1.setStatus("Approved");
            approvedLesson1 = lessonRepository.save(approvedLesson1);
            approvedLesson1.addVocabulary(vocabA, 1);
            approvedLesson1.addVocabulary(vocabB, 2);
            approvedLesson1 = lessonRepository.save(approvedLesson1);

            // Create Approved Lesson 2 containing vocabB (order 1) and vocabC (order 2)
            approvedLesson2 = new Lesson("Approved Lesson 2 " + suffix, account);
            approvedLesson2.setStatus("Approved");
            approvedLesson2 = lessonRepository.save(approvedLesson2);
            approvedLesson2.addVocabulary(vocabB, 1);
            approvedLesson2.addVocabulary(vocabC, 2);
            approvedLesson2 = lessonRepository.save(approvedLesson2);

            // Create Draft Lesson containing vocabC
            draftLesson = new Lesson("Draft Lesson " + suffix, account);
            draftLesson.setStatus("Draft");
            draftLesson = lessonRepository.save(draftLesson);
            draftLesson.addVocabulary(vocabC, 1);
            draftLesson = lessonRepository.save(draftLesson);

            return null;
        });

        authenticateAs(testLearner.getAccount().getEmailOrPhone());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        txTemplate.execute(status -> {
            if (approvedLesson1 != null) lessonRepository.delete(approvedLesson1);
            if (approvedLesson2 != null) lessonRepository.delete(approvedLesson2);
            if (draftLesson != null) lessonRepository.delete(draftLesson);
            if (testLearner != null) {
                reviewLogRepository.deleteAll(reviewLogRepository.findByUserOrderByReviewedAtDesc(testLearner));
                cardProgressRepository.deleteAll(cardProgressRepository.findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(testLearner, "VOCABULARY", java.time.LocalDateTime.now().plusYears(10)));
                userSrsSettingRepository.findByUser(testLearner).ifPresent(userSrsSettingRepository::delete);
                userProfileRepository.delete(testLearner);
                if (testLearner.getAccount() != null) {
                    accountRepository.delete(testLearner.getAccount());
                }
            }
            if (vocabA != null) vocabularyRepository.delete(vocabA);
            if (vocabB != null) vocabularyRepository.delete(vocabB);
            if (vocabC != null) vocabularyRepository.delete(vocabC);
            return null;
        });
    }

    private void authenticateAs(String emailOrPhone) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                emailOrPhone, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GIVEN approved lesson WHEN getNewCardCandidates THEN returns unstudied vocabulary ordered strictly by orderIndex ASC")
    void testGetNewCardCandidatesReturnsOrderedList() {
        List<DueCardResponse> candidates = srsService.getNewCardCandidates(approvedLesson1.getLessonId(), null);

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0).getItemId()).isEqualTo(vocabA.getVocabId());
        assertThat(candidates.get(0).getIsNew()).isTrue();
        assertThat(candidates.get(0).getRepetitions()).isZero();
        assertThat(candidates.get(0).getIntervalDays()).isZero();

        assertThat(candidates.get(1).getItemId()).isEqualTo(vocabB.getVocabId());
        assertThat(candidates.get(1).getIsNew()).isTrue();

        // Proves candidate retrieval did NOT persist any CardProgress (Preview vs Introduction separation)
        long progressCount = cardProgressRepository.countByUserAndNextReviewAtLessThanEqual(testLearner, java.time.LocalDateTime.now().plusYears(10));
        assertThat(progressCount).isZero();
    }

    @Test
    @DisplayName("GIVEN draft lesson WHEN getNewCardCandidates THEN throws NOT_FOUND 404")
    void testGetNewCardCandidatesDraftLessonThrowsNotFound() {
        assertThatThrownBy(() -> srsService.getNewCardCandidates(draftLesson.getLessonId(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy bài học");
    }

    @Test
    @DisplayName("GIVEN vocabulary studied in Lesson 1 WHEN Lesson 2 candidates queried THEN already-studied vocabulary is automatically excluded")
    void testCrossLessonDeduplication() {
        // 1. Learner reviews vocabB from Lesson 1
        ReviewCardRequest req = new ReviewCardRequest("VOCABULARY", vocabB.getVocabId(), 3, 2);
        DueCardResponse resp = srsService.reviewCard(req);
        assertThat(resp.getItemId()).isEqualTo(vocabB.getVocabId());

        // 2. Query Lesson 2 candidates (which contains vocabB at order 1 and vocabC at order 2)
        List<DueCardResponse> candidatesLesson2 = srsService.getNewCardCandidates(approvedLesson2.getLessonId(), null);

        // vocabB must be excluded, only vocabC remains
        assertThat(candidatesLesson2).hasSize(1);
        assertThat(candidatesLesson2.get(0).getItemId()).isEqualTo(vocabC.getVocabId());
    }

    @Test
    @DisplayName("GIVEN newCardsPerDay=2 WHEN learner introduces 2 new cards and attempts 3rd THEN 3rd is rejected with CONFLICT 409")
    void testDailyNewCardQuotaEnforcement() {
        // 1. Review 1st new card (vocabA) -> success
        srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabA.getVocabId(), 3, 2));

        // 2. Review 2nd new card (vocabB) -> success
        srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabB.getVocabId(), 3, 2));

        // 3. Verify candidates list is now empty (quota exhausted for today)
        List<DueCardResponse> candidates = srsService.getNewCardCandidates(approvedLesson2.getLessonId(), null);
        assertThat(candidates).isEmpty();

        // 4. Attempt 3rd new card (vocabC) -> throws CONFLICT
        assertThatThrownBy(() -> srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabC.getVocabId(), 3, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn đã đạt giới hạn học từ mới tối đa trong ngày (2 từ)");
    }

    @Test
    @DisplayName("GIVEN new card introduced with Again rating WHEN re-reviewed on same day THEN does NOT consume additional new-card quota")
    void testSameDayAgainDoesNotConsumeNewQuota() {
        // 1. First review of vocabA with rating 1 (Again) -> consumes 1 new quota
        srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabA.getVocabId(), 1, 2));

        // 2. Same-day re-review of vocabA with rating 1 (Again) -> re-review, NOT new card
        srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabA.getVocabId(), 1, 3));

        // 3. Same-day re-review of vocabA with rating 3 (Good) -> re-review, NOT new card
        srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabA.getVocabId(), 3, 4));

        // 4. Learner still has 1 remaining new-card quota (2 - 1 = 1) -> can introduce vocabB!
        DueCardResponse respB = srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabB.getVocabId(), 3, 2));
        assertThat(respB.getItemId()).isEqualTo(vocabB.getVocabId());

        // 5. Total unique new items today is exactly 2
        StudyStatsResponse stats = srsService.getStudyStats();
        assertThat(stats.getNewCardsToday()).isEqualTo(2L);
        assertThat(stats.getReviewsToday()).isEqualTo(4L); // 3 reviews on vocabA + 1 review on vocabB
    }

    @Test
    @DisplayName("GIVEN day 1 quota exhausted WHEN midnight rolls over to day 2 THEN new card quota resets to 2")
    void testDateBoundaryRollover() {
        // Day 1: introduce 2 new cards using spring service
        srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabA.getVocabId(), 3, 2));
        srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabB.getVocabId(), 3, 2));

        // Attempting vocabC on Day 1 fails
        assertThatThrownBy(() -> srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabC.getVocabId(), 3, 2)))
                .isInstanceOf(BusinessException.class);

        // Day 2 Clock: 1 day later
        Clock day2Clock = Clock.fixed(Instant.now().plusSeconds(86400 * 2), BUSINESS_ZONE);

        SrsServiceImpl serviceDay2 = new SrsServiceImpl(
                cardProgressRepository,
                reviewLogRepository,
                userSrsSettingRepository,
                vocabularyRepository,
                radicalRepository,
                userProfileRepository,
                lessonRepository,
                lessonVocabularyRepository,
                new SrsCalculator(),
                day2Clock
        );

        // On Day 2, learner can introduce vocabC within transaction!
        DueCardResponse respC = txTemplate.execute(status ->
                serviceDay2.reviewCard(new ReviewCardRequest("VOCABULARY", vocabC.getVocabId(), 3, 2))
        );
        assertThat(respC.getItemId()).isEqualTo(vocabC.getVocabId());

        StudyStatsResponse statsDay2 = txTemplate.execute(status -> serviceDay2.getStudyStats());
        assertThat(statsDay2.getNewCardsToday()).isEqualTo(1L); // only vocabC was introduced on Day 2
    }
}
