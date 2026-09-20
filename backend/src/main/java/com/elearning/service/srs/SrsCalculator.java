package com.elearning.service.srs;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure Mathematical Calculation Engine for Spaced Repetition System (SRS).
 * Implements the project-adapted SuperMemo SM-2 algorithm.
 * <p>
 * This class is completely framework-independent, database-agnostic, stateless, deterministic,
 * and thread-safe. It does not access system time, database repositories, or Spring context.
 * </p>
 *
 * <h3>Rating to Quality Score Mapping (1..4 -> 0..5):</h3>
 * <ul>
 *   <li>1 = Again (Học lại / quên hoàn toàn) -> q = 0 (Failed recall, resets repetition sequence)</li>
 *   <li>2 = Hard (Khó / nhớ nhưng vất vả)   -> q = 3 (Successful recall with difficulty, decreases EF by 0.14)</li>
 *   <li>3 = Good (Tốt / nhớ chuẩn)           -> q = 4 (Normal successful recall, maintains EF)</li>
 *   <li>4 = Easy (Dễ / nhớ tức thì)          -> q = 5 (Effortless recall, increases EF by 0.10)</li>
 * </ul>
 *
 * <h3>Canonical SM-2 Ease Factor (EF) Formula:</h3>
 * <p>
 *   EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))<br>
 *   EF' = max(1.30, EF')
 * </p>
 *
 * <h3>Interval Progression:</h3>
 * <ul>
 *   <li>If rating is Again (q < 3): repetitions = 0, interval = 0 (reset for immediate relearning)</li>
 *   <li>If rating is Hard, Good, Easy (q >= 3):
 *     <ul>
 *       <li>Repetition 1 (first success): interval = 1 day</li>
 *       <li>Repetition 2 (second success): interval = 6 days</li>
 *       <li>Repetition n >= 3: interval = ceil(previous_interval * EF')</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class SrsCalculator {

    public static final BigDecimal DEFAULT_EASE_FACTOR = new BigDecimal("2.50");
    public static final BigDecimal MIN_EASE_FACTOR = new BigDecimal("1.30");
    public static final int FIRST_INTERVAL_DAYS = 1;
    public static final int SECOND_INTERVAL_DAYS = 6;
    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 4;

    /**
     * Calculates the next SRS state given current state and user recall rating.
     *
     * @param currentEaseFactor   current ease factor (must be > 0)
     * @param currentIntervalDays current interval in days (must be >= 0)
     * @param currentRepetitions  current consecutive successful repetition count (must be >= 0)
     * @param rating              user rating enum (AGAIN, HARD, GOOD, EASY)
     * @return immutable {@link SrsCalculationResult} with new easeFactor, intervalDays, and repetitions
     * @throws IllegalArgumentException if inputs are invalid or out of bounds
     */
    public SrsCalculationResult calculate(
            BigDecimal currentEaseFactor,
            int currentIntervalDays,
            int currentRepetitions,
            ReviewRating rating) {

        validateInputs(currentEaseFactor, currentIntervalDays, currentRepetitions, rating);

        int q = mapRatingToQuality(rating);
        BigDecimal newEaseFactor = calculateNewEaseFactor(currentEaseFactor, q);
        int newRepetitions = calculateNewRepetitions(currentRepetitions, q);
        int newIntervalDays = calculateNewIntervalDays(currentIntervalDays, currentRepetitions, newEaseFactor, q);

        return new SrsCalculationResult(newEaseFactor, newIntervalDays, newRepetitions);
    }

    /**
     * Overload supporting integer numeric rating value (1..4).
     */
    public SrsCalculationResult calculate(
            BigDecimal currentEaseFactor,
            int currentIntervalDays,
            int currentRepetitions,
            int ratingValue) {
        ReviewRating rating = ReviewRating.fromValue(ratingValue);
        return calculate(currentEaseFactor, currentIntervalDays, currentRepetitions, rating);
    }

    /**
     * Overload supporting double floating-point ease factor.
     */
    public SrsCalculationResult calculate(
            double currentEaseFactor,
            int currentIntervalDays,
            int currentRepetitions,
            int ratingValue) {
        if (currentEaseFactor <= 0.0 || Double.isNaN(currentEaseFactor) || Double.isInfinite(currentEaseFactor)) {
            throw new IllegalArgumentException("currentEaseFactor phải là số thực dương hợp lệ: " + currentEaseFactor);
        }
        BigDecimal ef = BigDecimal.valueOf(currentEaseFactor).setScale(2, RoundingMode.HALF_UP);
        return calculate(ef, currentIntervalDays, currentRepetitions, ratingValue);
    }

    /**
     * Convenience method for calculating initial review of a brand new card
     * (initial EF = 2.50, initial interval = 0, initial repetitions = 0).
     *
     * @param rating user recall evaluation rating
     * @return initial calculation result
     */
    public SrsCalculationResult calculateInitial(ReviewRating rating) {
        return calculate(DEFAULT_EASE_FACTOR, 0, 0, rating);
    }

    /**
     * Convenience method for calculating initial review of a brand new card with integer rating.
     */
    public SrsCalculationResult calculateInitial(int ratingValue) {
        return calculate(DEFAULT_EASE_FACTOR, 0, 0, ratingValue);
    }

    private void validateInputs(
            BigDecimal currentEaseFactor,
            int currentIntervalDays,
            int currentRepetitions,
            ReviewRating rating) {

        if (currentEaseFactor == null || currentEaseFactor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("currentEaseFactor không được null hoặc <= 0: " + currentEaseFactor);
        }
        if (currentIntervalDays < 0) {
            throw new IllegalArgumentException("currentIntervalDays không được âm: " + currentIntervalDays);
        }
        if (currentRepetitions < 0) {
            throw new IllegalArgumentException("currentRepetitions không được âm: " + currentRepetitions);
        }
        if (rating == null) {
            throw new IllegalArgumentException("rating không được để null");
        }
    }

    /**
     * Maps project 1..4 rating scale to canonical SM-2 0..5 quality score:
     * <ul>
     *   <li>AGAIN (1) -> 0 (Fail)</li>
     *   <li>HARD  (2) -> 3 (Pass with difficulty)</li>
     *   <li>GOOD  (3) -> 4 (Pass with normal effort)</li>
     *   <li>EASY  (4) -> 5 (Pass with perfect recall)</li>
     * </ul>
     */
    private int mapRatingToQuality(ReviewRating rating) {
        return switch (rating) {
            case AGAIN -> 0;
            case HARD -> 3;
            case GOOD -> 4;
            case EASY -> 5;
        };
    }

    /**
     * Computes updated Ease Factor (EF') using canonical SM-2 formula:
     * <p>
     * deltaEF = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)<br>
     * EF' = max(1.30, EF + deltaEF)
     * </p>
     */
    private BigDecimal calculateNewEaseFactor(BigDecimal currentEf, int q) {
        // deltaEF calculation
        // For q = 5 (EASY):  delta = +0.10
        // For q = 4 (GOOD):  delta = 0.00
        // For q = 3 (HARD):  delta = -0.14
        // For q = 0 (AGAIN): delta = -0.80
        double diff = 5.0 - q;
        double delta = 0.1 - (diff * (0.08 + diff * 0.02));

        BigDecimal calculatedEf = currentEf.add(BigDecimal.valueOf(delta)).setScale(2, RoundingMode.HALF_UP);

        if (calculatedEf.compareTo(MIN_EASE_FACTOR) < 0) {
            return MIN_EASE_FACTOR;
        }
        return calculatedEf;
    }

    /**
     * Determines updated repetition count:
     * <ul>
     *   <li>If q < 3 (Again) -> reset to 0</li>
     *   <li>If q >= 3 (Hard, Good, Easy) -> increment current repetitions by 1</li>
     * </ul>
     */
    private int calculateNewRepetitions(int currentRepetitions, int q) {
        if (q < 3) {
            return 0;
        }
        return currentRepetitions + 1;
    }

    /**
     * Determines next review interval in days:
     * <ul>
     *   <li>If q < 3 (Again) -> 0 days (relearning / due today)</li>
     *   <li>If q >= 3:
     *     <ul>
     *       <li>If currentRepetitions == 0 -> 1 day (FIRST_INTERVAL_DAYS)</li>
     *       <li>If currentRepetitions == 1 -> 6 days (SECOND_INTERVAL_DAYS)</li>
     *       <li>If currentRepetitions >= 2 -> ceil(currentIntervalDays * newEaseFactor)</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    private int calculateNewIntervalDays(
            int currentIntervalDays,
            int currentRepetitions,
            BigDecimal newEaseFactor,
            int q) {

        if (q < 3) {
            // Failed recall resets interval to 0 days for immediate relearning
            return 0;
        }

        if (currentRepetitions == 0) {
            return FIRST_INTERVAL_DAYS;
        }
        if (currentRepetitions == 1) {
            return SECOND_INTERVAL_DAYS;
        }

        // For n >= 3, I(n) = ceil(I(n-1) * EF')
        double multiplied = currentIntervalDays * newEaseFactor.doubleValue();
        double ceiled = Math.ceil(multiplied);

        if (ceiled > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) ceiled;
    }
}
