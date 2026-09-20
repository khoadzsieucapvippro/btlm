package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.dto.response.StudyStatsResponse;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Radical;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.Role;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 7B.1: SrsService Unit Tests")
class SrsServiceTests {

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

    private SrsCalculator srsCalculator;

    private SrsServiceImpl srsService;

    private UserProfile testUserProfile;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        srsCalculator = new SrsCalculator();
        srsService = new SrsServiceImpl(
                cardProgressRepository,
                reviewLogRepository,
                userSrsSettingRepository,
                vocabularyRepository,
                radicalRepository,
                userProfileRepository,
                lessonRepository,
                lessonVocabularyRepository,
                srsCalculator,
                null
        );

        testAccount = new Account();
        testAccount.setEmailOrPhone("learner@example.com");
        testAccount.setPasswordHash("passwordHash");
        testAccount.setStatus("Active");
        testAccount.setAccountId(1L);

        testUserProfile = new UserProfile();
        testUserProfile.setAccount(testAccount);
        testUserProfile.setFullName("Learner Test");
        testUserProfile.setUserId(10L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("learner@example.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_LEARNER")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUser() {
        org.mockito.Mockito.lenient().when(userProfileRepository.findByAccountEmailOrPhone("learner@example.com"))
                .thenReturn(Optional.of(testUserProfile));
        org.mockito.Mockito.lenient().when(userProfileRepository.findByAccountEmailOrPhoneWithLock("learner@example.com"))
                .thenReturn(Optional.of(testUserProfile));
        org.mockito.Mockito.lenient().when(reviewLogRepository.findTodayLogIdsWithLock(eq(testUserProfile), any(LocalDateTime.class)))
                .thenReturn(List.of());
        org.mockito.Mockito.lenient().when(lessonVocabularyRepository.existsApprovedLessonForVocabulary(any()))
                .thenReturn(true);
    }

    @Nested
    @DisplayName("getDueCards Tests")
    class GetDueCardsTests {

        @Test
        @DisplayName("Unauthenticated request throws UNAUTHORIZED")
        void testGetDueCards_unauthenticated_throwsException() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> srsService.getDueCards(null, 10))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        }

        @Test
        @DisplayName("When daily review limit is reached, returns empty list without querying progress")
        void testGetDueCards_dailyLimitReached_returnsEmpty() {
            mockCurrentUser();

            UserSrsSetting setting = new UserSrsSetting(testUserProfile, 20, 50);
            when(userSrsSettingRepository.findByUser(testUserProfile)).thenReturn(Optional.of(setting));
            when(reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(eq(testUserProfile), any(LocalDateTime.class)))
                    .thenReturn(50L); // 50/50 reached

            List<DueCardResponse> results = srsService.getDueCards(null, 10);

            assertThat(results).isEmpty();
            verify(cardProgressRepository, never()).findByUserAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(any(), any(), any());
        }

        @Test
        @DisplayName("Queries due cards and hydrates vocabulary and radical contents in bulk (BE-PERF-001)")
        void testGetDueCards_hydratesPolymorphicContent() {
            mockCurrentUser();

            UserSrsSetting setting = new UserSrsSetting(testUserProfile, 20, 100);
            when(userSrsSettingRepository.findByUser(testUserProfile)).thenReturn(Optional.of(setting));
            when(reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(eq(testUserProfile), any(LocalDateTime.class)))
                    .thenReturn(5L);

            CardProgress cpVocab = new CardProgress(testUserProfile, "VOCABULARY", 101L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(1));
            CardProgress cpRadical = new CardProgress(testUserProfile, "RADICAL", 5L, new BigDecimal("2.36"), 6, 2, LocalDateTime.now().minusHours(2));

            when(cardProgressRepository.findByUserAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(eq(testUserProfile), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of(cpVocab, cpRadical));

            Vocabulary vocab = new Vocabulary("水", "shuǐ", "shui", "Thủy", "Nước");
            vocab.setExampleSentence("喝水");
            vocab.setExampleTranslation("Uống nước");
            vocab.setVocabId(101L);
            when(vocabularyRepository.findAllById(any())).thenReturn(List.of(vocab));

            Radical radical = new Radical(5, "丿", "piě", "Phiệt", "Nét phẩy");
            when(radicalRepository.findAllById(any())).thenReturn(List.of(radical));

            List<DueCardResponse> results = srsService.getDueCards(null, 10);

            assertThat(results).hasSize(2);

            DueCardResponse item1 = results.get(0);
            assertThat(item1.getItemType()).isEqualTo("VOCABULARY");
            assertThat(item1.getItemId()).isEqualTo(101L);
            assertThat(item1.getHanzi()).isEqualTo("水");

            DueCardResponse item2 = results.get(1);
            assertThat(item2.getItemType()).isEqualTo("RADICAL");
            assertThat(item2.getItemId()).isEqualTo(5L);
            assertThat(item2.getHanzi()).isEqualTo("丿");

            // Verify bulk retrieval used, not individual findById
            verify(vocabularyRepository, never()).findById(any());
            verify(radicalRepository, never()).findById(any());
            verify(vocabularyRepository, times(1)).findAllById(any());
            verify(radicalRepository, times(1)).findAllById(any());
        }

        @Test
        @DisplayName("BE-PERF-001: Preserves exact original CardProgress order when hydrating mixed polymorphic items")
        void testGetDueCards_preservesOriginalCardProgressOrder() {
            mockCurrentUser();

            UserSrsSetting setting = new UserSrsSetting(testUserProfile, 20, 100);
            when(userSrsSettingRepository.findByUser(testUserProfile)).thenReturn(Optional.of(setting));
            when(reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(eq(testUserProfile), any(LocalDateTime.class)))
                    .thenReturn(0L);

            CardProgress cp1 = new CardProgress(testUserProfile, "VOCABULARY", 101L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(4));
            CardProgress cp2 = new CardProgress(testUserProfile, "RADICAL", 5L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(3));
            CardProgress cp3 = new CardProgress(testUserProfile, "VOCABULARY", 102L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(2));
            CardProgress cp4 = new CardProgress(testUserProfile, "RADICAL", 2L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(1));

            when(cardProgressRepository.findByUserAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(eq(testUserProfile), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of(cp1, cp2, cp3, cp4));

            Vocabulary v1 = new Vocabulary("水", "shuǐ", "shui", "Thủy", "Nước");
            v1.setVocabId(101L);
            Vocabulary v2 = new Vocabulary("火", "huǒ", "huo", "Hỏa", "Lửa");
            v2.setVocabId(102L);
            when(vocabularyRepository.findAllById(any())).thenReturn(List.of(v2, v1)); // repository returns in any order

            Radical r1 = new Radical(5, "丿", "piě", "Phiệt", "Nét phẩy");
            Radical r2 = new Radical(2, "丨", "gǔn", "Cổn", "Nét sổ");
            when(radicalRepository.findAllById(any())).thenReturn(List.of(r2, r1)); // repository returns in any order

            List<DueCardResponse> results = srsService.getDueCards(null, 10);

            assertThat(results).hasSize(4);
            assertThat(results.get(0).getItemId()).isEqualTo(101L);
            assertThat(results.get(0).getItemType()).isEqualTo("VOCABULARY");
            assertThat(results.get(1).getItemId()).isEqualTo(5L);
            assertThat(results.get(1).getItemType()).isEqualTo("RADICAL");
            assertThat(results.get(2).getItemId()).isEqualTo(102L);
            assertThat(results.get(2).getItemType()).isEqualTo("VOCABULARY");
            assertThat(results.get(3).getItemId()).isEqualTo(2L);
            assertThat(results.get(3).getItemType()).isEqualTo("RADICAL");
        }

        @Test
        @DisplayName("BE-PERF-001: Missing item entity is gracefully skipped without errors")
        void testGetDueCards_missingItem_gracefullySkipped() {
            mockCurrentUser();

            UserSrsSetting setting = new UserSrsSetting(testUserProfile, 20, 100);
            when(userSrsSettingRepository.findByUser(testUserProfile)).thenReturn(Optional.of(setting));
            when(reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(eq(testUserProfile), any(LocalDateTime.class)))
                    .thenReturn(0L);

            CardProgress cpExisting = new CardProgress(testUserProfile, "VOCABULARY", 101L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(2));
            CardProgress cpMissing = new CardProgress(testUserProfile, "VOCABULARY", 999L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now().minusHours(1));

            when(cardProgressRepository.findByUserAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(eq(testUserProfile), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of(cpExisting, cpMissing));

            Vocabulary v1 = new Vocabulary("水", "shuǐ", "shui", "Thủy", "Nước");
            v1.setVocabId(101L);
            when(vocabularyRepository.findAllById(any())).thenReturn(List.of(v1)); // 999 not returned

            List<DueCardResponse> results = srsService.getDueCards(null, 10);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getItemId()).isEqualTo(101L);
        }
    }

    @Nested
    @DisplayName("reviewCard Tests")
    class ReviewCardTests {

        @Test
        @DisplayName("First review of new vocabulary card with Rating 3 (Good) initializes progress and logs review")
        void testReviewCard_newVocabulary_good() {
            mockCurrentUser();

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 200L, 3, 4);

            Vocabulary vocab = new Vocabulary("火", "huǒ", "huo", "Hỏa", "Lửa");
            vocab.setExampleSentence("大火");
            vocab.setExampleTranslation("Lửa lớn");
            vocab.setVocabId(200L);
            when(vocabularyRepository.findByIdWithLock(200L)).thenReturn(Optional.of(vocab));

            when(cardProgressRepository.findByUserAndItemTypeAndItemId(testUserProfile, "VOCABULARY", 200L))
                    .thenReturn(Optional.empty()); // new card

            when(cardProgressRepository.save(any(CardProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DueCardResponse response = srsService.reviewCard(request);

            assertThat(response.getItemType()).isEqualTo("VOCABULARY");
            assertThat(response.getItemId()).isEqualTo(200L);
            assertThat(response.getEaseFactor()).isEqualTo(new BigDecimal("2.50"));
            assertThat(response.getIntervalDays()).isEqualTo(1);
            assertThat(response.getRepetitions()).isEqualTo(1);
            assertThat(response.getNextReviewAt()).isNotNull();

            ArgumentCaptor<CardProgress> cpCaptor = ArgumentCaptor.forClass(CardProgress.class);
            verify(cardProgressRepository).save(cpCaptor.capture());
            CardProgress savedCp = cpCaptor.getValue();
            assertThat(savedCp.getUser()).isEqualTo(testUserProfile);
            assertThat(savedCp.getItemType()).isEqualTo("VOCABULARY");
            assertThat(savedCp.getItemId()).isEqualTo(200L);
            assertThat(savedCp.getIntervalDays()).isEqualTo(1);
            assertThat(savedCp.getRepetitions()).isEqualTo(1);

            ArgumentCaptor<ReviewLog> logCaptor = ArgumentCaptor.forClass(ReviewLog.class);
            verify(reviewLogRepository).save(logCaptor.capture());
            ReviewLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getUser()).isEqualTo(testUserProfile);
            assertThat(savedLog.getItemType()).isEqualTo("VOCABULARY");
            assertThat(savedLog.getItemId()).isEqualTo(200L);
            assertThat(savedLog.getRating()).isEqualTo((byte) 3);
            assertThat(savedLog.getIntervalBefore()).isEqualTo(0);
            assertThat(savedLog.getIntervalAfter()).isEqualTo(1);
            assertThat(savedLog.getReviewTimeSeconds()).isEqualTo(4);
        }

        @Test
        @DisplayName("Review of existing radical card with Rating 2 (Hard) updates EF and interval (6 -> 15)")
        void testReviewCard_existingRadical_hard() {
            mockCurrentUser();

            ReviewCardRequest request = new ReviewCardRequest("RADICAL", 8L, 2, 5);

            Radical radical = new Radical(8, "人", "rén", "Nhân", "Người");
            when(radicalRepository.findById(8)).thenReturn(Optional.of(radical));

            CardProgress existingProgress = new CardProgress(testUserProfile, "RADICAL", 8L, new BigDecimal("2.50"), 6, 2, LocalDateTime.now().minusDays(1));
            when(cardProgressRepository.findByUserAndItemTypeAndItemId(testUserProfile, "RADICAL", 8L))
                    .thenReturn(Optional.of(existingProgress));

            when(cardProgressRepository.save(any(CardProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DueCardResponse response = srsService.reviewCard(request);

            assertThat(response.getItemType()).isEqualTo("RADICAL");
            assertThat(response.getEaseFactor()).isEqualTo(new BigDecimal("2.36"));
            assertThat(response.getIntervalDays()).isEqualTo(15); // ceil(6 * 2.36) = 15
            assertThat(response.getRepetitions()).isEqualTo(3);

            ArgumentCaptor<ReviewLog> logCaptor = ArgumentCaptor.forClass(ReviewLog.class);
            verify(reviewLogRepository).save(logCaptor.capture());
            ReviewLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getIntervalBefore()).isEqualTo(6);
            assertThat(savedLog.getIntervalAfter()).isEqualTo(15);
            assertThat(savedLog.getRating()).isEqualTo((byte) 2);
            assertThat(savedLog.getReviewTimeSeconds()).isEqualTo(5);
        }

        @Test
        @DisplayName("Review with Rating 1 (Again) resets repetitions and interval to 0")
        void testReviewCard_again_resets() {
            mockCurrentUser();

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 300L, 1, 6);

            Vocabulary vocab = new Vocabulary("木", "mù", "mu", "Mộc", "Cây gỗ");
            vocab.setExampleSentence("木头");
            vocab.setExampleTranslation("Khúc gỗ");
            vocab.setVocabId(300L);
            when(vocabularyRepository.findByIdWithLock(300L)).thenReturn(Optional.of(vocab));

            CardProgress existingProgress = new CardProgress(testUserProfile, "VOCABULARY", 300L, new BigDecimal("2.50"), 15, 3, LocalDateTime.now().minusDays(1));
            when(cardProgressRepository.findByUserAndItemTypeAndItemId(testUserProfile, "VOCABULARY", 300L))
                    .thenReturn(Optional.of(existingProgress));

            when(cardProgressRepository.save(any(CardProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DueCardResponse response = srsService.reviewCard(request);

            assertThat(response.getEaseFactor()).isEqualTo(new BigDecimal("1.70"));
            assertThat(response.getIntervalDays()).isEqualTo(0);
            assertThat(response.getRepetitions()).isEqualTo(0);

            ArgumentCaptor<ReviewLog> logCaptor = ArgumentCaptor.forClass(ReviewLog.class);
            verify(reviewLogRepository).save(logCaptor.capture());
            ReviewLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getIntervalBefore()).isEqualTo(15);
            assertThat(savedLog.getIntervalAfter()).isEqualTo(0);
        }

        @Test
        @DisplayName("Reviewing non-existent vocabulary throws NOT_FOUND")
        void testReviewCard_missingVocabulary_throwsNotFound() {
            mockCurrentUser();

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 9999L, 3, 2);
            when(vocabularyRepository.findByIdWithLock(9999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> srsService.reviewCard(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(cardProgressRepository, never()).save(any());
            verify(reviewLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Reviewing non-existent radical throws NOT_FOUND")
        void testReviewCard_missingRadical_throwsNotFound() {
            mockCurrentUser();

            ReviewCardRequest request = new ReviewCardRequest("RADICAL", 9999L, 3, 2);
            when(radicalRepository.findById(9999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> srsService.reviewCard(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("Review with invalid rating throws VALIDATION_ERROR")
        void testReviewCard_invalidRating_throwsValidationError() {
            mockCurrentUser();

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 100L, 5, 2);

            assertThatThrownBy(() -> srsService.reviewCard(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }

        @Test
        @DisplayName("When daily review quota is reached, reviewCard throws CONFLICT and does not update card or save log")
        void testReviewCard_dailyQuotaReached_throwsConflict() {
            mockCurrentUser();

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 100L, 3, 2);

            Vocabulary vocab = new Vocabulary("天", "tiān", "tian", "Thiên", "Trời");
            when(vocabularyRepository.findByIdWithLock(100L)).thenReturn(Optional.of(vocab));

            UserSrsSetting setting = new UserSrsSetting(testUserProfile, 20, 50);
            when(userSrsSettingRepository.findByUser(testUserProfile)).thenReturn(Optional.of(setting));
            List<Long> mockIds = Collections.nCopies(50, 1L);
            when(reviewLogRepository.findTodayLogIdsWithLock(eq(testUserProfile), any(LocalDateTime.class)))
                    .thenReturn(mockIds); // 50/50 reached

            assertThatThrownBy(() -> srsService.reviewCard(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(e.getMessage()).contains("đạt giới hạn ôn tập tối đa trong ngày");
                    });

            verify(cardProgressRepository, never()).save(any());
            verify(reviewLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Review of future scheduled card (nextReviewAt > now) throws CONFLICT (Thẻ chưa đến hạn ôn tập)")
        void testReviewCard_futureCard_throwsConflict() {
            mockCurrentUser();

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 100L, 3, 2);

            Vocabulary vocab = new Vocabulary("天", "tiān", "tian", "Thiên", "Trời");
            when(vocabularyRepository.findByIdWithLock(100L)).thenReturn(Optional.of(vocab));

            CardProgress futureProgress = new CardProgress(testUserProfile, "VOCABULARY", 100L, new BigDecimal("2.50"), 1, 1, LocalDateTime.now().plusDays(3));
            when(cardProgressRepository.findByUserAndItemTypeAndItemId(testUserProfile, "VOCABULARY", 100L))
                    .thenReturn(Optional.of(futureProgress));

            assertThatThrownBy(() -> srsService.reviewCard(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(e.getMessage()).contains("Thẻ chưa đến hạn ôn tập");
                    });

            verify(cardProgressRepository, never()).save(any());
            verify(reviewLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("R2.1: First review of new vocabulary not belonging to any Approved lesson throws UNPROCESSABLE_ENTITY")
        void testReviewCard_newVocabulary_noApprovedLesson_throwsUnprocessableEntity() {
            mockCurrentUser();

            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 500L, 3, 2);

            Vocabulary vocab = new Vocabulary("草", "cǎo", "cao", "Thảo", "Cỏ");
            when(vocabularyRepository.findByIdWithLock(500L)).thenReturn(Optional.of(vocab));
            when(cardProgressRepository.findByUserAndItemTypeAndItemId(testUserProfile, "VOCABULARY", 500L))
                    .thenReturn(Optional.empty()); // new card
            when(lessonVocabularyRepository.existsApprovedLessonForVocabulary(500L))
                    .thenReturn(false); // NO Approved lesson!

            assertThatThrownBy(() -> srsService.reviewCard(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);
                        assertThat(e.getMessage()).contains("chưa thuộc bất kỳ bài học nào đã được phê duyệt");
                    });

            verify(cardProgressRepository, never()).save(any());
            verify(reviewLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("R2.1: First review of new RADICAL bypasses lesson eligibility check")
        void testReviewCard_newRadical_bypassesLessonEligibility() {
            mockCurrentUser();

            ReviewCardRequest request = new ReviewCardRequest("RADICAL", 8L, 3, 2);

            Radical rad = new Radical(8, "亠", "tóu", "Đầu", "Không có nghĩa riêng");
            when(radicalRepository.findById(8)).thenReturn(Optional.of(rad));
            when(cardProgressRepository.findByUserAndItemTypeAndItemId(testUserProfile, "RADICAL", 8L))
                    .thenReturn(Optional.empty()); // new radical card
            when(cardProgressRepository.save(any(CardProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DueCardResponse response = srsService.reviewCard(request);

            assertThat(response).isNotNull();
            assertThat(response.getItemType()).isEqualTo("RADICAL");
            assertThat(response.getItemId()).isEqualTo(8L);
            verify(lessonVocabularyRepository, never()).existsApprovedLessonForVocabulary(any());
        }
    }

    @Nested
    @DisplayName("getStudyStats Tests")
    class GetStudyStatsTests {

        @Test
        @DisplayName("Calculates study statistics accurately from repositories and settings")
        void testGetStudyStats() {
            mockCurrentUser();

            UserSrsSetting setting = new UserSrsSetting(testUserProfile, 25, 120);
            when(userSrsSettingRepository.findByUser(testUserProfile)).thenReturn(Optional.of(setting));

            when(cardProgressRepository.countByUserAndNextReviewAtLessThanEqual(eq(testUserProfile), any(LocalDateTime.class)))
                    .thenReturn(18L);
            when(reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(eq(testUserProfile), any(LocalDateTime.class)))
                    .thenReturn(32L);

            StudyStatsResponse stats = srsService.getStudyStats();

            assertThat(stats.getCardsDue()).isEqualTo(18L);
            assertThat(stats.getReviewsToday()).isEqualTo(32L);
            assertThat(stats.getNewCardsLimit()).isEqualTo(25);
            assertThat(stats.getMaxReviewLimit()).isEqualTo(120);
        }
    }
}
