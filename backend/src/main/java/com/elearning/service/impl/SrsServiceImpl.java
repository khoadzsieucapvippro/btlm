package com.elearning.service.impl;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.dto.response.StudyStatsResponse;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Radical;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.CardProgressRepository;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.ReviewLogRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.SrsService;
import com.elearning.service.srs.ReviewRating;
import com.elearning.service.srs.SrsCalculationResult;
import com.elearning.service.srs.SrsCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.elearning.config.TimeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;

/**
 * Production implementation of {@link SrsService}.
 * Orchestrates Spaced Repetition reviews, card progress lifecycles, and study sessions
 * using pure mathematical calculation engine {@link SrsCalculator}.
 */
@Service
public class SrsServiceImpl implements SrsService {

    private static final Logger log = LoggerFactory.getLogger(SrsServiceImpl.class);

    private final CardProgressRepository cardProgressRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final UserSrsSettingRepository userSrsSettingRepository;
    private final VocabularyRepository vocabularyRepository;
    private final RadicalRepository radicalRepository;
    private final UserProfileRepository userProfileRepository;
    private final LessonRepository lessonRepository;
    private final LessonVocabularyRepository lessonVocabularyRepository;
    private final SrsCalculator srsCalculator;
    private final Clock businessClock;

    @Autowired
    public SrsServiceImpl(
            CardProgressRepository cardProgressRepository,
            ReviewLogRepository reviewLogRepository,
            UserSrsSettingRepository userSrsSettingRepository,
            VocabularyRepository vocabularyRepository,
            RadicalRepository radicalRepository,
            UserProfileRepository userProfileRepository,
            LessonRepository lessonRepository,
            LessonVocabularyRepository lessonVocabularyRepository,
            Clock businessClock) {
        this(cardProgressRepository, reviewLogRepository, userSrsSettingRepository,
                vocabularyRepository, radicalRepository, userProfileRepository,
                lessonRepository, lessonVocabularyRepository, new SrsCalculator(), businessClock);
    }

    public SrsServiceImpl(
            CardProgressRepository cardProgressRepository,
            ReviewLogRepository reviewLogRepository,
            UserSrsSettingRepository userSrsSettingRepository,
            VocabularyRepository vocabularyRepository,
            RadicalRepository radicalRepository,
            UserProfileRepository userProfileRepository,
            LessonRepository lessonRepository,
            LessonVocabularyRepository lessonVocabularyRepository,
            SrsCalculator srsCalculator,
            Clock businessClock) {
        this.cardProgressRepository = cardProgressRepository;
        this.reviewLogRepository = reviewLogRepository;
        this.userSrsSettingRepository = userSrsSettingRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.radicalRepository = radicalRepository;
        this.userProfileRepository = userProfileRepository;
        this.lessonRepository = lessonRepository;
        this.lessonVocabularyRepository = lessonVocabularyRepository;
        this.srsCalculator = srsCalculator != null ? srsCalculator : new SrsCalculator();
        this.businessClock = businessClock != null ? businessClock : Clock.system(ZoneId.of(TimeConfig.DEFAULT_BUSINESS_TIMEZONE));
    }

    public SrsServiceImpl(
            CardProgressRepository cardProgressRepository,
            ReviewLogRepository reviewLogRepository,
            UserSrsSettingRepository userSrsSettingRepository,
            VocabularyRepository vocabularyRepository,
            RadicalRepository radicalRepository,
            UserProfileRepository userProfileRepository,
            Clock businessClock) {
        this(cardProgressRepository, reviewLogRepository, userSrsSettingRepository,
                vocabularyRepository, radicalRepository, userProfileRepository,
                null, null, new SrsCalculator(), businessClock);
    }

    public SrsServiceImpl(
            CardProgressRepository cardProgressRepository,
            ReviewLogRepository reviewLogRepository,
            UserSrsSettingRepository userSrsSettingRepository,
            VocabularyRepository vocabularyRepository,
            RadicalRepository radicalRepository,
            UserProfileRepository userProfileRepository,
            SrsCalculator srsCalculator,
            Clock businessClock) {
        this(cardProgressRepository, reviewLogRepository, userSrsSettingRepository,
                vocabularyRepository, radicalRepository, userProfileRepository,
                null, null, srsCalculator, businessClock);
    }

    public SrsServiceImpl(
            CardProgressRepository cardProgressRepository,
            ReviewLogRepository reviewLogRepository,
            UserSrsSettingRepository userSrsSettingRepository,
            VocabularyRepository vocabularyRepository,
            RadicalRepository radicalRepository,
            UserProfileRepository userProfileRepository) {
        this(cardProgressRepository, reviewLogRepository, userSrsSettingRepository,
                vocabularyRepository, radicalRepository, userProfileRepository,
                null, null, new SrsCalculator(), Clock.system(ZoneId.of(TimeConfig.DEFAULT_BUSINESS_TIMEZONE)));
    }

    public SrsServiceImpl(
            CardProgressRepository cardProgressRepository,
            ReviewLogRepository reviewLogRepository,
            UserSrsSettingRepository userSrsSettingRepository,
            VocabularyRepository vocabularyRepository,
            RadicalRepository radicalRepository,
            UserProfileRepository userProfileRepository,
            SrsCalculator srsCalculator) {
        this(cardProgressRepository, reviewLogRepository, userSrsSettingRepository,
                vocabularyRepository, radicalRepository, userProfileRepository,
                null, null, srsCalculator, Clock.system(ZoneId.of(TimeConfig.DEFAULT_BUSINESS_TIMEZONE)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DueCardResponse> getDueCards(String itemTypeFilter, Integer limit) {
        UserProfile user = resolveCurrentUserProfile();

        // 1. Resolve daily limits
        UserSrsSetting setting = userSrsSettingRepository.findByUser(user).orElse(null);
        int maxReviewPerDay = setting != null ? setting.getMaxReviewPerDay() : 100;

        // 2. Count today's completed reviews
        LocalDateTime startOfDay = LocalDate.now(businessClock).atStartOfDay();
        long reviewsToday = reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(user, startOfDay);
        int remainingQuota = (int) Math.max(0, maxReviewPerDay - reviewsToday);

        if (remainingQuota == 0) {
            log.info("Learner user_id={} has reached daily review limit ({}/{})", user.getUserId(), reviewsToday, maxReviewPerDay);
            return Collections.emptyList();
        }

        // 3. Apply limit
        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, remainingQuota) : remainingQuota;
        Pageable pageable = PageRequest.of(0, effectiveLimit);
        LocalDateTime now = LocalDateTime.now(businessClock);

        // 4. Query due card progress
        List<CardProgress> dueProgressList;
        if (itemTypeFilter != null && !itemTypeFilter.trim().isEmpty()) {
            String typeUpper = itemTypeFilter.trim().toUpperCase();
            dueProgressList = cardProgressRepository.findByUserAndItemTypeAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(
                    user, typeUpper, now, pageable);
        } else {
            dueProgressList = cardProgressRepository.findByUserAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(
                    user, now, pageable);
        }

        // 5. Hydrate polymorphic content in bulk (BE-PERF-001: eliminate N+1 queries)
        if (dueProgressList.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> vocabIds = new HashSet<>();
        Set<Integer> radicalIds = new HashSet<>();

        for (CardProgress progress : dueProgressList) {
            if ("VOCABULARY".equalsIgnoreCase(progress.getItemType()) && progress.getItemId() != null) {
                vocabIds.add(progress.getItemId());
            } else if ("RADICAL".equalsIgnoreCase(progress.getItemType()) && progress.getItemId() != null) {
                radicalIds.add(progress.getItemId().intValue());
            }
        }

        Map<Long, Vocabulary> vocabMap = vocabIds.isEmpty()
                ? Collections.emptyMap()
                : vocabularyRepository.findAllById(vocabIds).stream()
                        .collect(Collectors.toMap(Vocabulary::getVocabId, Function.identity(), (a, b) -> a));

        Map<Integer, Radical> radicalMap = radicalIds.isEmpty()
                ? Collections.emptyMap()
                : radicalRepository.findAllById(radicalIds).stream()
                        .collect(Collectors.toMap(Radical::getRadicalId, Function.identity(), (a, b) -> a));

        List<DueCardResponse> results = new ArrayList<>(dueProgressList.size());
        for (CardProgress progress : dueProgressList) {
            if ("VOCABULARY".equalsIgnoreCase(progress.getItemType()) && progress.getItemId() != null) {
                Vocabulary v = vocabMap.get(progress.getItemId());
                if (v != null) {
                    results.add(DueCardResponse.fromVocabulary(v, progress));
                }
            } else if ("RADICAL".equalsIgnoreCase(progress.getItemType()) && progress.getItemId() != null) {
                Radical r = radicalMap.get(progress.getItemId().intValue());
                if (r != null) {
                    results.add(DueCardResponse.fromRadical(r, progress));
                }
            }
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DueCardResponse> getNewCardCandidates(Long lessonId, Integer limit) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID bài học không được để trống");
        }

        UserProfile user = resolveCurrentUserProfile();

        // 1. Validate lesson existence and approved status (R1-DEC-02, R1-DEC-03)
        if (lessonRepository != null) {
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));
            if (!"Approved".equalsIgnoreCase(lesson.getStatus())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId);
            }
        }

        // 2. Resolve daily new-card limits (R1-DEC-04)
        UserSrsSetting setting = userSrsSettingRepository.findByUser(user).orElse(null);
        int newCardsLimit = setting != null ? setting.getNewCardsPerDay() : 20;

        if (newCardsLimit <= 0) {
            log.info("Learner user_id={} has newCardsPerDay set to 0", user.getUserId());
            return Collections.emptyList();
        }

        // 3. Count unique new items introduced today (R1-DEC-04)
        LocalDateTime startOfDay = LocalDate.now(businessClock).atStartOfDay();
        long newCardsIntroducedToday = reviewLogRepository.countNewItemsIntroducedToday(user, "VOCABULARY", startOfDay);
        int remainingNewQuota = (int) Math.max(0, newCardsLimit - newCardsIntroducedToday);

        if (remainingNewQuota == 0) {
            log.info("Learner user_id={} has reached daily new-card limit ({}/{})", user.getUserId(), newCardsIntroducedToday, newCardsLimit);
            return Collections.emptyList();
        }

        // 4. Apply limit
        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, remainingNewQuota) : remainingNewQuota;
        Pageable pageable = PageRequest.of(0, effectiveLimit);

        // 5. Query candidate LessonVocabulary associations
        if (lessonVocabularyRepository == null) {
            return Collections.emptyList();
        }

        List<LessonVocabulary> candidateList = lessonVocabularyRepository.findNewCardCandidates(lessonId, user, pageable);
        if (candidateList == null || candidateList.isEmpty()) {
            return Collections.emptyList();
        }

        // 6. Map to DueCardResponse with isNew = true (preview only, creates zero CardProgress)
        List<DueCardResponse> results = new ArrayList<>(candidateList.size());
        for (LessonVocabulary lv : candidateList) {
            if (lv.getVocabulary() != null) {
                results.add(DueCardResponse.fromVocabulary(lv.getVocabulary(), null));
            }
        }

        return results;
    }

    @Override
    @Transactional
    public DueCardResponse reviewCard(ReviewCardRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Yêu cầu đánh giá thẻ không được để null");
        }

        // 1. Validate itemType
        if (request.getItemType() == null || request.getItemType().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Loại thẻ (itemType) không được để trống");
        }
        String itemTypeUpper = request.getItemType().trim().toUpperCase();
        if (!"VOCABULARY".equals(itemTypeUpper) && !"RADICAL".equals(itemTypeUpper)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Loại thẻ phải là VOCABULARY hoặc RADICAL");
        }

        // 2. Validate rating
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 4) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Điểm đánh giá phải từ 1 đến 4 (1=Again, 2=Hard, 3=Good, 4=Easy)");
        }

        // 3. Validate reviewTimeSeconds
        if (request.getReviewTimeSeconds() == null || request.getReviewTimeSeconds() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thời gian phản xạ không được âm");
        }

        // 4. Validate item existence in catalog (polymorphic referential integrity & concurrency serialization R3.1A)
        Vocabulary vocab = null;
        Radical radical = null;
        if ("VOCABULARY".equals(itemTypeUpper)) {
            vocab = vocabularyRepository.findByIdWithLock(request.getItemId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng với ID: " + request.getItemId()));
        } else {
            radical = radicalRepository.findById(request.getItemId().intValue())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bộ thủ với ID: " + request.getItemId()));
        }

        // 5. Resolve user profile with pessimistic lock to serialize quota-consuming reviews for this user (BE-CONC-002)
        UserProfile user = resolveCurrentUserProfileWithLock();

        // 6. Authoritative daily review quota enforcement (BE-CONC-002)
        UserSrsSetting setting = userSrsSettingRepository.findByUser(user).orElse(null);
        int maxReviewPerDay = setting != null ? setting.getMaxReviewPerDay() : 100;

        LocalDateTime startOfDay = LocalDate.now(businessClock).atStartOfDay();
        List<Long> todayLogIds = reviewLogRepository.findTodayLogIdsWithLock(user, startOfDay);
        int reviewsToday = todayLogIds.size();
        if (reviewsToday >= maxReviewPerDay) {
            log.warn("Learner user_id={} has reached daily review limit ({}/{})", user.getUserId(), reviewsToday, maxReviewPerDay);
            throw new BusinessException(ErrorCode.CONFLICT, "Bạn đã đạt giới hạn ôn tập tối đa trong ngày (" + maxReviewPerDay + " thẻ)");
        }

        // 7. Load existing progress or initialize new card progress (protected by @Version optimistic locking)
        Optional<CardProgress> optProgress = cardProgressRepository.findByUserAndItemTypeAndItemId(user, itemTypeUpper, request.getItemId());
        boolean isNewCard = optProgress.isEmpty();

        // Enforce Due-Only policy on existing cards for Standard SRS mode (R2 Review Policy)
        if (!isNewCard) {
            CardProgress existing = optProgress.get();
            LocalDateTime now = LocalDateTime.now(businessClock);
            if (existing.getIntervalDays() > 0 && existing.getNextReviewAt() != null && existing.getNextReviewAt().isAfter(now)) {
                log.warn("Card is not due yet for user_id={}, itemType={}, itemId={}, nextReviewAt={}",
                        user.getUserId(), itemTypeUpper, request.getItemId(), existing.getNextReviewAt());
                throw new BusinessException(ErrorCode.CONFLICT, "Thẻ chưa đến hạn ôn tập");
            }
        }

        // Authoritative new-card eligibility & daily quota enforcement on first-ever review (R1-DEC-01..04 & R2.1)
        if (isNewCard && "VOCABULARY".equals(itemTypeUpper)) {
            // R2.1: Invariant — A vocabulary may only be introduced as a new card if it belongs to at least one Approved lesson
            boolean hasApprovedLesson = lessonVocabularyRepository.existsApprovedLessonForVocabulary(request.getItemId());
            if (!hasApprovedLesson) {
                log.warn("Vocabulary item_id={} does not belong to any Approved lesson, rejected new card creation for user_id={}",
                        request.getItemId(), user.getUserId());
                throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "Từ vựng chưa thuộc bất kỳ bài học nào đã được phê duyệt để học mới");
            }

            int newCardsLimit = setting != null ? setting.getNewCardsPerDay() : 20;
            long newCardsIntroducedToday = countNewCardsIntroducedTodayWithLock(user, "VOCABULARY", startOfDay);
            if (newCardsIntroducedToday >= newCardsLimit) {
                log.warn("Learner user_id={} has reached daily new-card limit ({}/{})", user.getUserId(), newCardsIntroducedToday, newCardsLimit);
                throw new BusinessException(ErrorCode.CONFLICT, "Bạn đã đạt giới hạn học từ mới tối đa trong ngày (" + newCardsLimit + " từ)");
            }
        }

        CardProgress progress;
        int intervalBefore;
        BigDecimal currentEf;
        int currentRepetitions;

        if (!isNewCard) {
            progress = optProgress.get();
            intervalBefore = progress.getIntervalDays();
            currentEf = progress.getEaseFactor();
            currentRepetitions = progress.getRepetitions();
        } else {
            progress = new CardProgress();
            progress.setUser(user);
            progress.setItemType(itemTypeUpper);
            progress.setItemId(request.getItemId());
            progress.setEaseFactor(SrsCalculator.DEFAULT_EASE_FACTOR);
            progress.setIntervalDays(0);
            progress.setRepetitions(0);
            intervalBefore = 0;
            currentEf = SrsCalculator.DEFAULT_EASE_FACTOR;
            currentRepetitions = 0;
        }

        // 8. Delegate to pure mathematical SrsCalculator
        ReviewRating ratingEnum = ReviewRating.fromValue(request.getRating());
        SrsCalculationResult calcResult = srsCalculator.calculate(currentEf, intervalBefore, currentRepetitions, ratingEnum);

        // 9. Calculate next_review_at
        LocalDateTime reviewedAt = LocalDateTime.now(businessClock);
        if (calcResult.intervalDays() == 0) {
            progress.setNextReviewAt(reviewedAt); // due immediately for relearning today
        } else {
            progress.setNextReviewAt(reviewedAt.plusDays(calcResult.intervalDays()));
        }

        // 10. Update CardProgress state
        progress.setEaseFactor(calcResult.easeFactor());
        progress.setIntervalDays(calcResult.intervalDays());
        progress.setRepetitions(calcResult.repetitions());
        CardProgress savedProgress = cardProgressRepository.save(progress);

        // 11. Append immutable ReviewLog audit entry
        ReviewLog logEntry = new ReviewLog(
                user,
                itemTypeUpper,
                request.getItemId(),
                request.getRating().byteValue(),
                intervalBefore,
                calcResult.intervalDays(),
                request.getReviewTimeSeconds()
        );
        logEntry.setReviewedAt(reviewedAt);
        reviewLogRepository.save(logEntry);

        log.info("SRS review processed: user_id={}, itemType={}, itemId={}, rating={}, interval: {} -> {}, EF: {} -> {}, isNewCard={}",
                user.getUserId(), itemTypeUpper, request.getItemId(), request.getRating(),
                intervalBefore, calcResult.intervalDays(), currentEf, calcResult.easeFactor(), isNewCard);

        // 12. Return updated DueCardResponse
        if (vocab != null) {
            return DueCardResponse.fromVocabulary(vocab, savedProgress);
        } else {
            return DueCardResponse.fromRadical(radical, savedProgress);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StudyStatsResponse getStudyStats() {
        UserProfile user = resolveCurrentUserProfile();

        UserSrsSetting setting = userSrsSettingRepository.findByUser(user).orElse(null);
        int newLimit = setting != null ? setting.getNewCardsPerDay() : 20;
        int maxReviewLimit = setting != null ? setting.getMaxReviewPerDay() : 100;

        LocalDateTime now = LocalDateTime.now(businessClock);
        long cardsDue = cardProgressRepository.countByUserAndNextReviewAtLessThanEqual(user, now);

        LocalDateTime startOfDay = LocalDate.now(businessClock).atStartOfDay();
        long reviewsToday = reviewLogRepository.countByUserAndReviewedAtGreaterThanEqual(user, startOfDay);
        long newCardsToday = reviewLogRepository.countNewItemsIntroducedToday(user, "VOCABULARY", startOfDay);

        return new StudyStatsResponse(cardsDue, reviewsToday, newLimit, maxReviewLimit, newCardsToday);
    }

    private UserProfile resolveCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Chưa xác thực hoặc phiên đăng nhập đã hết hạn");
        }
        String emailOrPhone = authentication.getName();
        return userProfileRepository.findByAccountEmailOrPhone(emailOrPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Không tìm thấy thông tin người dùng cho tài khoản: " + emailOrPhone));
    }

    private UserProfile resolveCurrentUserProfileWithLock() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Chưa xác thực hoặc phiên đăng nhập đã hết hạn");
        }
        String emailOrPhone = authentication.getName();
        return userProfileRepository.findByAccountEmailOrPhoneWithLock(emailOrPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Không tìm thấy thông tin người dùng cho tài khoản: " + emailOrPhone));
    }

    private long countNewCardsIntroducedTodayWithLock(UserProfile user, String itemType, LocalDateTime startOfDay) {
        List<ReviewLog> todayLogs = reviewLogRepository.findTodayReviewLogsWithLock(user, startOfDay);
        if (todayLogs == null || todayLogs.isEmpty()) {
            return 0L;
        }
        Set<Long> distinctItemIds = new HashSet<>();
        for (ReviewLog r : todayLogs) {
            if (itemType.equalsIgnoreCase(r.getItemType()) && r.getItemId() != null) {
                distinctItemIds.add(r.getItemId());
            }
        }
        if (distinctItemIds.isEmpty()) {
            return 0L;
        }
        long count = 0;
        for (Long itemId : distinctItemIds) {
            boolean reviewedBeforeToday = reviewLogRepository.existsByUserAndItemTypeAndItemIdAndReviewedAtLessThan(user, itemType, itemId, startOfDay);
            if (!reviewedBeforeToday) {
                count++;
            }
        }
        return count;
    }
}
