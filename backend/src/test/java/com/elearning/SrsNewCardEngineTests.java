package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.config.TimeConfig;
import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.dto.response.StudyStatsResponse;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.CardProgressRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.ReviewLogRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.impl.SrsServiceImpl;
import com.elearning.service.srs.SrsCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("R1 — SRS New Card Engine Unit Tests")
class SrsNewCardEngineTests {

    @Mock
    private CardProgressRepository cardProgressRepository;

    @Mock
    private ReviewLogRepository reviewLogRepository;

    @Mock
    private UserSrsSettingRepository userSrsSettingRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private RadicalRepository radicalRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonVocabularyRepository lessonVocabularyRepository;

    private SrsServiceImpl srsService;
    private Clock fixedClock;
    private UserProfile testUser;
    private Lesson approvedLesson;
    private Lesson draftLesson;
    private Vocabulary vocab1;
    private Vocabulary vocab2;
    private UserSrsSetting userSetting;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-08-31T10:00:00Z"), ZoneId.of(TimeConfig.DEFAULT_BUSINESS_TIMEZONE));
        srsService = new SrsServiceImpl(
                cardProgressRepository,
                reviewLogRepository,
                userSrsSettingRepository,
                vocabularyRepository,
                radicalRepository,
                userProfileRepository,
                lessonRepository,
                lessonVocabularyRepository,
                new SrsCalculator(),
                fixedClock
        );

        Account account = new Account();
        account.setAccountId(1L);
        account.setEmailOrPhone("learner@test.com");

        testUser = new UserProfile();
        testUser.setUserId(10L);
        testUser.setAccount(account);
        testUser.setFullName("Test Learner");

        userSetting = new UserSrsSetting();
        userSetting.setUser(testUser);
        userSetting.setNewCardsPerDay(10);
        userSetting.setMaxReviewPerDay(100);

        approvedLesson = new Lesson("Lesson 1: Greetings", account);
        approvedLesson.setLessonId(100L);
        approvedLesson.setStatus("Approved");

        draftLesson = new Lesson("Lesson 2: Draft", account);
        draftLesson.setLessonId(200L);
        draftLesson.setStatus("Draft");

        vocab1 = new Vocabulary("你好", "nǐ hǎo", "ni hao", "Nhĩ hảo", "Xin chào");
        vocab1.setVocabId(1L);

        vocab2 = new Vocabulary("再见", "zài jiàn", "zai jian", "Tái kiến", "Tạm biệt");
        vocab2.setVocabId(2L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("learner@test.com", "credentials", Collections.emptyList())
        );
    }

    @Nested
    @DisplayName("Candidate Retrieval (getNewCardCandidates) Tests")
    class CandidateRetrievalTests {

        @Test
        @DisplayName("GIVEN approved lesson and available new quota WHEN getNewCardCandidates THEN returns ordered candidate cards with isNew=true")
        void testGetNewCardCandidatesSuccess() {
            when(userProfileRepository.findByAccountEmailOrPhone("learner@test.com")).thenReturn(Optional.of(testUser));
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(approvedLesson));
            when(userSrsSettingRepository.findByUser(testUser)).thenReturn(Optional.of(userSetting));
            when(reviewLogRepository.countNewItemsIntroducedToday(eq(testUser), eq("VOCABULARY"), any(LocalDateTime.class))).thenReturn(0L);

            LessonVocabulary lv1 = new LessonVocabulary(approvedLesson, vocab1, 1);
            LessonVocabulary lv2 = new LessonVocabulary(approvedLesson, vocab2, 2);
            when(lessonVocabularyRepository.findNewCardCandidates(eq(100L), eq(testUser), any(Pageable.class)))
                    .thenReturn(List.of(lv1, lv2));

            List<DueCardResponse> candidates = srsService.getNewCardCandidates(100L, null);

            assertThat(candidates).hasSize(2);
            assertThat(candidates.get(0).getItemId()).isEqualTo(1L);
            assertThat(candidates.get(0).getHanzi()).isEqualTo("你好");
            assertThat(candidates.get(0).getIsNew()).isTrue();
            assertThat(candidates.get(0).getRepetitions()).isZero();
            assertThat(candidates.get(0).getIntervalDays()).isZero();
            assertThat(candidates.get(0).getNextReviewAt()).isNull();

            assertThat(candidates.get(1).getItemId()).isEqualTo(2L);
            assertThat(candidates.get(1).getHanzi()).isEqualTo("再见");
            assertThat(candidates.get(1).getIsNew()).isTrue();

            // Candidate retrieval MUST NOT save or create any CardProgress (Preview vs Introduction separation)
            verify(cardProgressRepository, never()).save(any());
            verify(reviewLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("GIVEN non-approved (Draft) lesson WHEN getNewCardCandidates THEN throws NOT_FOUND 404")
        void testGetNewCardCandidatesDraftLessonThrowsNotFound() {
            when(userProfileRepository.findByAccountEmailOrPhone("learner@test.com")).thenReturn(Optional.of(testUser));
            when(lessonRepository.findById(200L)).thenReturn(Optional.of(draftLesson));

            assertThatThrownBy(() -> srsService.getNewCardCandidates(200L, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(lessonVocabularyRepository, never()).findNewCardCandidates(any(), any(), any());
        }

        @Test
        @DisplayName("GIVEN nonexistent lesson ID WHEN getNewCardCandidates THEN throws NOT_FOUND 404")
        void testGetNewCardCandidatesNonexistentLessonThrowsNotFound() {
            when(userProfileRepository.findByAccountEmailOrPhone("learner@test.com")).thenReturn(Optional.of(testUser));
            when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> srsService.getNewCardCandidates(999L, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("GIVEN null lesson ID WHEN getNewCardCandidates THEN throws VALIDATION_ERROR 400")
        void testGetNewCardCandidatesNullLessonIdThrowsValidationError() {
            assertThatThrownBy(() -> srsService.getNewCardCandidates(null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }

        @Test
        @DisplayName("GIVEN newCardsPerDay=0 WHEN getNewCardCandidates THEN returns empty list immediately")
        void testGetNewCardCandidatesZeroQuotaReturnsEmptyList() {
            userSetting.setNewCardsPerDay(0);
            when(userProfileRepository.findByAccountEmailOrPhone("learner@test.com")).thenReturn(Optional.of(testUser));
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(approvedLesson));
            when(userSrsSettingRepository.findByUser(testUser)).thenReturn(Optional.of(userSetting));

            List<DueCardResponse> candidates = srsService.getNewCardCandidates(100L, null);

            assertThat(candidates).isEmpty();
            verify(lessonVocabularyRepository, never()).findNewCardCandidates(any(), any(), any());
        }

        @Test
        @DisplayName("GIVEN daily new quota exhausted (10/10 introduced) WHEN getNewCardCandidates THEN returns empty list")
        void testGetNewCardCandidatesQuotaExhaustedReturnsEmptyList() {
            when(userProfileRepository.findByAccountEmailOrPhone("learner@test.com")).thenReturn(Optional.of(testUser));
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(approvedLesson));
            when(userSrsSettingRepository.findByUser(testUser)).thenReturn(Optional.of(userSetting));
            when(reviewLogRepository.countNewItemsIntroducedToday(eq(testUser), eq("VOCABULARY"), any(LocalDateTime.class))).thenReturn(10L);

            List<DueCardResponse> candidates = srsService.getNewCardCandidates(100L, null);

            assertThat(candidates).isEmpty();
            verify(lessonVocabularyRepository, never()).findNewCardCandidates(any(), any(), any());
        }

        @Test
        @DisplayName("GIVEN remaining quota = 3 and client limit = 10 WHEN getNewCardCandidates THEN effective limit is clamped to 3")
        void testGetNewCardCandidatesEffectiveLimitClampedToRemainingQuota() {
            when(userProfileRepository.findByAccountEmailOrPhone("learner@test.com")).thenReturn(Optional.of(testUser));
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(approvedLesson));
            when(userSrsSettingRepository.findByUser(testUser)).thenReturn(Optional.of(userSetting));
            when(reviewLogRepository.countNewItemsIntroducedToday(eq(testUser), eq("VOCABULARY"), any(LocalDateTime.class))).thenReturn(7L); // 10 - 7 = 3 left

            when(lessonVocabularyRepository.findNewCardCandidates(eq(100L), eq(testUser), any(Pageable.class)))
                    .thenAnswer(invocation -> {
                        Pageable pageable = invocation.getArgument(2);
                        assertThat(pageable.getPageSize()).isEqualTo(3);
                        return Collections.emptyList();
                    });

            List<DueCardResponse> candidates = srsService.getNewCardCandidates(100L, 10);
            assertThat(candidates).isEmpty();
        }
    }

    @Nested
    @DisplayName("First-Ever Review & New Card Quota Enforcement Tests")
    class NewCardReviewQuotaTests {

        @Test
        @DisplayName("GIVEN unreviewed card AND remaining new quota available WHEN reviewCard THEN initializes CardProgress and records ReviewLog")
        void testFirstReviewSuccess() {
            when(userProfileRepository.findByAccountEmailOrPhoneWithLock("learner@test.com")).thenReturn(Optional.of(testUser));
            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(vocab1));
            when(lessonVocabularyRepository.existsApprovedLessonForVocabulary(1L)).thenReturn(true);
            when(userSrsSettingRepository.findByUser(testUser)).thenReturn(Optional.of(userSetting));
            when(reviewLogRepository.findTodayLogIdsWithLock(eq(testUser), any(LocalDateTime.class))).thenReturn(Collections.emptyList());
            when(cardProgressRepository.findByUserAndItemTypeAndItemId(testUser, "VOCABULARY", 1L)).thenReturn(Optional.empty());
            when(reviewLogRepository.findTodayReviewLogsWithLock(eq(testUser), any(LocalDateTime.class))).thenReturn(Collections.emptyList());

            when(cardProgressRepository.save(any(CardProgress.class))).thenAnswer(invocation -> {
                CardProgress cp = invocation.getArgument(0);
                cp.setProgressId(101L);
                return cp;
            });

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 1L, 3, 4); // rating 3 = Good
            DueCardResponse response = srsService.reviewCard(request);

            assertThat(response).isNotNull();
            assertThat(response.getItemId()).isEqualTo(1L);
            assertThat(response.getRepetitions()).isEqualTo(1);
            assertThat(response.getIntervalDays()).isEqualTo(1);
            assertThat(response.getEaseFactor()).isEqualByComparingTo(new BigDecimal("2.50"));

            verify(cardProgressRepository).save(any(CardProgress.class));
            verify(reviewLogRepository).save(any());
        }

        @Test
        @DisplayName("GIVEN unreviewed card AND newCardsPerDay reached (10/10) WHEN reviewCard THEN rejected with CONFLICT 409")
        void testFirstReviewQuotaExhaustedThrowsConflict() {
            when(userProfileRepository.findByAccountEmailOrPhoneWithLock("learner@test.com")).thenReturn(Optional.of(testUser));
            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(vocab1));
            when(lessonVocabularyRepository.existsApprovedLessonForVocabulary(1L)).thenReturn(true);
            when(userSrsSettingRepository.findByUser(testUser)).thenReturn(Optional.of(userSetting));
            when(reviewLogRepository.findTodayLogIdsWithLock(eq(testUser), any(LocalDateTime.class))).thenReturn(List.of(1L, 2L, 3L));
            when(cardProgressRepository.findByUserAndItemTypeAndItemId(testUser, "VOCABULARY", 1L)).thenReturn(Optional.empty());

            // Build 10 distinct review logs for today
            List<ReviewLog> logs = new ArrayList<>();
            for (long i = 1; i <= 10; i++) {
                ReviewLog l = new ReviewLog(testUser, "VOCABULARY", 1000L + i, (byte) 3, 0, 1, 2);
                logs.add(l);
            }
            when(reviewLogRepository.findTodayReviewLogsWithLock(eq(testUser), any(LocalDateTime.class))).thenReturn(logs);
            when(reviewLogRepository.existsByUserAndItemTypeAndItemIdAndReviewedAtLessThan(eq(testUser), eq("VOCABULARY"), any(Long.class), any(LocalDateTime.class))).thenReturn(false);

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 1L, 3, 4);

            assertThatThrownBy(() -> srsService.reviewCard(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Bạn đã đạt giới hạn học từ mới tối đa trong ngày (10 từ)");
                    });

            verify(cardProgressRepository, never()).save(any());
        }

        @Test
        @DisplayName("GIVEN existing CardProgress (re-review / Again) WHEN reviewCard THEN newCardsPerDay quota is NOT checked")
        void testExistingCardReviewBypassesNewCardQuotaCheck() {
            CardProgress existingProgress = new CardProgress(testUser, "VOCABULARY", 1L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now(fixedClock));
            existingProgress.setProgressId(101L);

            when(userProfileRepository.findByAccountEmailOrPhoneWithLock("learner@test.com")).thenReturn(Optional.of(testUser));
            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(vocab1));
            when(userSrsSettingRepository.findByUser(testUser)).thenReturn(Optional.of(userSetting));
            when(reviewLogRepository.findTodayLogIdsWithLock(eq(testUser), any(LocalDateTime.class))).thenReturn(List.of(1L, 2L));
            when(cardProgressRepository.findByUserAndItemTypeAndItemId(testUser, "VOCABULARY", 1L)).thenReturn(Optional.of(existingProgress));
            when(cardProgressRepository.save(any(CardProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 1L, 4, 3); // Easy
            DueCardResponse response = srsService.reviewCard(request);

            assertThat(response).isNotNull();
            assertThat(response.getRepetitions()).isEqualTo(2);

            // findTodayReviewLogsWithLock should NOT be called for existing cards
            verify(reviewLogRepository, never()).findTodayReviewLogsWithLock(any(), any());
        }
    }

    @Nested
    @DisplayName("Study Statistics (getStudyStats) Tests")
    class StudyStatsTests {

        @Test
        @DisplayName("GIVEN learner with study history WHEN getStudyStats THEN returns accurate cardsDue, reviewsToday, and newCardsToday")
        void testGetStudyStatsAccurate() {
            when(userProfileRepository.findByAccountEmailOrPhone("learner@test.com")).thenReturn(Optional.of(testUser));
            when(userSrsSettingRepository.findByUser(testUser)).thenReturn(Optional.of(userSetting));
            when(cardProgressRepository.countByUserAndNextReviewAtLessThanEqual(eq(testUser), any(LocalDateTime.class))).thenReturn(5L);
            when(reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(eq(testUser), any(LocalDateTime.class))).thenReturn(12L);
            when(reviewLogRepository.countNewItemsIntroducedToday(eq(testUser), eq("VOCABULARY"), any(LocalDateTime.class))).thenReturn(4L);

            StudyStatsResponse stats = srsService.getStudyStats();

            assertThat(stats.getCardsDue()).isEqualTo(5L);
            assertThat(stats.getReviewsToday()).isEqualTo(12L);
            assertThat(stats.getNewCardsLimit()).isEqualTo(10);
            assertThat(stats.getMaxReviewLimit()).isEqualTo(100);
            assertThat(stats.getNewCardsToday()).isEqualTo(4L);
        }
    }
}
