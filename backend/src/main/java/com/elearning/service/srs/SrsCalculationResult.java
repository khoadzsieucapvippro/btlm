package com.elearning.service.srs;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable value object (Java 21 record) representing the calculated output of the SM-2 algorithm.
 * Contains updated ease factor, next interval in days, and consecutive repetition count.
 * Pure mathematical domain model without dependencies on database entities or Spring context.
 *
 * @param easeFactor   calculated ease factor (normalized to 2 decimal places, floor >= 1.30)
 * @param intervalDays calculated interval in days until the next scheduled review
 * @param repetitions  updated count of consecutive successful repetitions
 */
public record SrsCalculationResult(
        BigDecimal easeFactor,
        int intervalDays,
        int repetitions
) {
    public SrsCalculationResult {
        if (easeFactor == null) {
            throw new IllegalArgumentException("easeFactor không được để null");
        }
        if (intervalDays < 0) {
            throw new IllegalArgumentException("intervalDays không được âm: " + intervalDays);
        }
        if (repetitions < 0) {
            throw new IllegalArgumentException("repetitions không được âm: " + repetitions);
        }
        // Normalize easeFactor to scale of 2
        easeFactor = easeFactor.setScale(2, RoundingMode.HALF_UP);
    }
}
