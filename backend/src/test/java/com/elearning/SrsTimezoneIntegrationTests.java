package com.elearning;

import com.elearning.config.TimeConfig;
import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.dto.response.StudyStatsResponse;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.ReviewLog;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification and Boundary Integration Tests for BE-SRS-001: Explicit Business Timezone.
 * Proves that SRS daily quota calculations, due card checks, and review logs are evaluated
 * strictly against the configured business timezone (Asia/Ho_Chi_Minh) and are independent
 * of the host/JVM system default timezone.
 */
@SpringBootTest
@DisplayName("Task 8D.4 / BE-SRS-001: Timezone Awareness & Daily Quota Boundary Integration Tests")
class SrsTimezoneIntegrationTests {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private Clock springInjectedClock;

    private UserProfile testLearner;
    private String userEmail;
    private Vocabulary testVocab;
    private Lesson testLesson;

    @BeforeEach
    void setUp() {
        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        userEmail = "learner_tz_" + uniqueSuffix + "@test.com";

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
        profile.setFullName("Learner TZ " + uniqueSuffix);
        testLearner = userProfileRepository.save(profile);

        // Daily limit of 1 review per day for boundary testing
        UserSrsSetting setting = new UserSrsSetting(testLearner, 10, 1);
        userSrsSettingRepository.save(setting);

        testVocab = vocabularyRepository.findByHanziAndPinyinRaw("字", "zi")
                .orElseGet(() -> {
                    Vocabulary v = new Vocabulary("字", "zì", "zi", "Tự", "Chữ");
                    return vocabularyRepository.save(v);
                });

        testLesson = new Lesson();
        testLesson.setTitle("TZ Lesson " + uniqueSuffix);
        testLesson.setStatus("Approved");
        testLesson.setCreatedBy(account);
        testLesson = lessonRepository.save(testLesson);

        if (!lessonVocabularyRepository.existsByLesson_LessonIdAndVocabulary_VocabId(testLesson.getLessonId(), testVocab.getVocabId())) {
            lessonVocabularyRepository.save(new LessonVocabulary(testLesson, testVocab, 1));
        }

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
    }

    @Test
    @DisplayName("BE-SRS-001: Spring injected Clock has the configured business timezone (Asia/Ho_Chi_Minh)")
    void testSpringInjectedClock_usesBusinessTimezone() {
        assertThat(springInjectedClock).isNotNull();
        assertThat(springInjectedClock.getZone()).isEqualTo(BUSINESS_ZONE);
    }

    @Test
    @DisplayName("BE-SRS-001: TimeConfig fails fast on invalid business timezone without silent fallback")
    void testTimeConfig_failsFastOnInvalidZone() {
        TimeConfig timeConfig = new TimeConfig();
        assertThatThrownBy(() -> timeConfig.businessClock("Invalid/NonExistent_Zone"))
                .isInstanceOf(ZoneRulesException.class);

        assertThatThrownBy(() -> timeConfig.businessClock(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("BE-SRS-001: GIVEN instant just before midnight (23:59:50) in Asia/Ho_Chi_Minh WHEN next day arrives (00:00:10) THEN daily review quota resets cleanly")
    void testBusinessDayBoundary_midnightQuotaReset() {
        org.springframework.transaction.support.TransactionTemplate txTemplate =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);

        // Instant A: 2026-08-30 23:59:50 in Asia/Ho_Chi_Minh (UTC: 2026-08-30 16:59:50)
        Instant instantBeforeMidnight = Instant.parse("2026-08-30T16:59:50Z");
        Clock clockDay1 = Clock.fixed(instantBeforeMidnight, BUSINESS_ZONE);

        SrsServiceImpl serviceDay1 = new SrsServiceImpl(
                cardProgressRepository,
                reviewLogRepository,
                userSrsSettingRepository,
                vocabularyRepository,
                radicalRepository,
                userProfileRepository,
                lessonRepository,
                lessonVocabularyRepository,
                new SrsCalculator(),
                clockDay1
        );

        // Review card on Day 1 (23:59:50)
        ReviewCardRequest req = new ReviewCardRequest();
        req.setItemType("VOCABULARY");
        req.setItemId(testVocab.getVocabId());
        req.setRating(3);
        req.setReviewTimeSeconds(5);

        DueCardResponse responseDay1 = txTemplate.execute(status -> serviceDay1.reviewCard(req));
        assertThat(responseDay1).isNotNull();

        // Second review on Day 1 (still 23:59:50) must be rejected because maxReviewPerDay=1
        assertThatThrownBy(() -> txTemplate.execute(status -> serviceDay1.reviewCard(req)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bạn đã đạt giới hạn ôn tập tối đa trong ngày");

        // Instant B: 2026-08-31 00:00:10 in Asia/Ho_Chi_Minh (UTC: 2026-08-30 17:00:10) - 20 seconds later, crossed midnight!
        Instant instantAfterMidnight = Instant.parse("2026-08-30T17:00:10Z");
        Clock clockDay2 = Clock.fixed(instantAfterMidnight, BUSINESS_ZONE);

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
                clockDay2
        );

        // On Day 2, getDueCards must see 0 reviews today (quota reset!)
        List<DueCardResponse> dueDay2 = txTemplate.execute(status -> serviceDay2.getDueCards(null, 10));
        // Quota is available (not empty due to quota)
        assertThat(dueDay2).isNotNull();

        StudyStatsResponse statsDay2 = txTemplate.execute(status -> serviceDay2.getStudyStats());
        assertThat(statsDay2.getReviewsToday()).isEqualTo(0);

        // Advance nextReviewAt so testVocab is due on Day 2
        txTemplate.execute(status -> {
            CardProgress cp = cardProgressRepository.findByUserAndItemTypeAndItemId(testLearner, "VOCABULARY", testVocab.getVocabId()).orElseThrow();
            cp.setNextReviewAt(LocalDateTime.of(2026, 8, 30, 0, 0, 0));
            cardProgressRepository.saveAndFlush(cp);
            return null;
        });

        // Day 2 review must succeed!
        req.setRating(4);
        DueCardResponse responseDay2 = txTemplate.execute(status -> serviceDay2.reviewCard(req));
        assertThat(responseDay2).isNotNull();

        // Now Day 2 quota is consumed (1/1)
        StudyStatsResponse statsDay2After = txTemplate.execute(status -> serviceDay2.getStudyStats());
        assertThat(statsDay2After.getReviewsToday()).isEqualTo(1);
    }

    @Test
    @DisplayName("BE-SRS-001: Evaluates business date correctly even when system default timezone is completely different (e.g. America/New_York or UTC)")
    void testBusinessDateEvaluation_independentOfSystemTimezone() {
        // At instant 2026-08-30 18:00:00 UTC:
        // In Asia/Ho_Chi_Minh (UTC+7): Date is 2026-08-31 01:00:00 (August 31st)
        // In America/New_York (UTC-4): Date is 2026-08-30 14:00:00 (August 30th)
        // In UTC: Date is 2026-08-30 18:00:00 (August 30th)
        Instant instant = Instant.parse("2026-08-30T18:00:00Z");
        Clock businessClock = Clock.fixed(instant, BUSINESS_ZONE);

        LocalDate businessDate = LocalDate.now(businessClock);
        LocalDateTime businessStartOfDay = businessDate.atStartOfDay();

        assertThat(businessDate).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(businessStartOfDay).isEqualTo(LocalDateTime.of(2026, 8, 31, 0, 0, 0));

        // When SrsServiceImpl runs with this businessClock:
        SrsServiceImpl service = new SrsServiceImpl(
                cardProgressRepository,
                reviewLogRepository,
                userSrsSettingRepository,
                vocabularyRepository,
                radicalRepository,
                userProfileRepository,
                new SrsCalculator(),
                businessClock
        );

        // Seed a ReviewLog from 2026-08-30 23:30:00 Asia/Ho_Chi_Minh
        ReviewLog oldLog = new ReviewLog(testLearner, "VOCABULARY", testVocab.getVocabId(), 3, 0, 1, 5);
        oldLog.setReviewedAt(LocalDateTime.of(2026, 8, 30, 23, 30, 0));
        reviewLogRepository.save(oldLog);

        // SrsService calculates reviewsToday for 2026-08-31 -> must be 0
        StudyStatsResponse stats = service.getStudyStats();
        assertThat(stats.getReviewsToday()).isEqualTo(0);
    }
}
