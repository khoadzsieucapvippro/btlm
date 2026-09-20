package com.elearning;

import com.elearning.service.srs.ReviewRating;
import com.elearning.service.srs.SrsCalculationResult;
import com.elearning.service.srs.SrsCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive Mathematical Unit Tests for SrsCalculator (Task 7A.2 & Checkpoint 7A).
 * <p>
 * This test suite verifies the mathematical precision and invariants of the project's SM-2
 * Spaced Repetition calculation engine independently of Spring Boot, JPA persistence, or system time.
 * </p>
 *
 * <h3>Mathematical Oracles & Reference Formula:</h3>
 * <ul>
 *   <li>Quality score mapping: Again(1)->q=0, Hard(2)->q=3, Good(3)->q=4, Easy(4)->q=5</li>
 *   <li>EF update: EF' = max(1.30, EF + 0.1 - (5-q)*(0.08 + (5-q)*0.02))</li>
 *   <li>Interval progression: q<3 -> I=0, R=0; q>=3: R=0->I=1, R=1->I=6, R>=2->I=ceil(I*EF')</li>
 * </ul>
 */
@DisplayName("Task 7A.2: Comprehensive Mathematical Unit Tests for SrsCalculator")
class SrsCalculatorTests {

    private SrsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SrsCalculator();
    }

    // =========================================================================
    // GROUP 1: Constants & Specification Verification
    // =========================================================================
    @Nested
    @DisplayName("Group 1: Constants & Default Values")
    class Group1ConstantsTests {

        @Test
        @DisplayName("Specification constants match project requirements and SM-2 baseline")
        void testSpecificationConstants() {
            assertThat(SrsCalculator.DEFAULT_EASE_FACTOR).isEqualTo(new BigDecimal("2.50"));
            assertThat(SrsCalculator.MIN_EASE_FACTOR).isEqualTo(new BigDecimal("1.30"));
            assertThat(SrsCalculator.FIRST_INTERVAL_DAYS).isEqualTo(1);
            assertThat(SrsCalculator.SECOND_INTERVAL_DAYS).isEqualTo(6);
            assertThat(SrsCalculator.MIN_RATING).isEqualTo(1);
            assertThat(SrsCalculator.MAX_RATING).isEqualTo(4);
        }

        @Test
        @DisplayName("ReviewRating enum values match project 1..4 scale specification")
        void testReviewRatingEnumContract() {
            assertThat(ReviewRating.AGAIN.getValue()).isEqualTo(1);
            assertThat(ReviewRating.HARD.getValue()).isEqualTo(2);
            assertThat(ReviewRating.GOOD.getValue()).isEqualTo(3);
            assertThat(ReviewRating.EASY.getValue()).isEqualTo(4);

            assertThat(ReviewRating.fromValue(1)).isSameAs(ReviewRating.AGAIN);
            assertThat(ReviewRating.fromValue(2)).isSameAs(ReviewRating.HARD);
            assertThat(ReviewRating.fromValue(3)).isSameAs(ReviewRating.GOOD);
            assertThat(ReviewRating.fromValue(4)).isSameAs(ReviewRating.EASY);
        }
    }

    // =========================================================================
    // GROUP 2: Rating Validation & Out-of-Bounds Handling
    // =========================================================================
    @Nested
    @DisplayName("Group 2: Rating Validation & Exception Safety")
    class Group2RatingValidationTests {

        @ParameterizedTest(name = "Invalid rating {0} must throw IllegalArgumentException")
        @ValueSource(ints = {-999, -1, 0, 5, 6, 10, 100})
        @DisplayName("Out-of-bounds integer rating values throw IllegalArgumentException")
        void testInvalidRatingValues_throwException(int invalidRating) {
            assertThatThrownBy(() -> calculator.calculate(new BigDecimal("2.50"), 1, 1, invalidRating))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Giá trị rating không hợp lệ");

            assertThatThrownBy(() -> calculator.calculateInitial(invalidRating))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Null ReviewRating throws IllegalArgumentException")
        void testNullRatingEnum_throwsException() {
            assertThatThrownBy(() -> calculator.calculate(new BigDecimal("2.50"), 1, 1, (ReviewRating) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rating không được để null");
        }

        @ParameterizedTest(name = "Invalid EF {0} must throw IllegalArgumentException")
        @ValueSource(strings = {"0.00", "-0.01", "-1.00", "-2.50"})
        @DisplayName("Non-positive Ease Factor throws IllegalArgumentException")
        void testInvalidEaseFactor_throwsException(String invalidEf) {
            assertThatThrownBy(() -> calculator.calculate(new BigDecimal(invalidEf), 1, 1, ReviewRating.GOOD))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Null Ease Factor throws IllegalArgumentException")
        void testNullEaseFactor_throwsException() {
            assertThatThrownBy(() -> calculator.calculate((BigDecimal) null, 1, 1, ReviewRating.GOOD))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "Invalid double EF {0} must throw IllegalArgumentException")
        @ValueSource(doubles = {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        @DisplayName("Invalid double Ease Factor values throw IllegalArgumentException")
        void testInvalidDoubleEf_throwsException(double invalidDoubleEf) {
            assertThatThrownBy(() -> calculator.calculate(invalidDoubleEf, 1, 1, 3))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Negative intervalDays throws IllegalArgumentException")
        void testNegativeInterval_throwsException() {
            assertThatThrownBy(() -> calculator.calculate(new BigDecimal("2.50"), -1, 1, ReviewRating.GOOD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currentIntervalDays không được âm");
        }

        @Test
        @DisplayName("Negative repetitions throws IllegalArgumentException")
        void testNegativeRepetitions_throwsException() {
            assertThatThrownBy(() -> calculator.calculate(new BigDecimal("2.50"), 1, -1, ReviewRating.GOOD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currentRepetitions không được âm");
        }
    }

    // =========================================================================
    // GROUP 3: Rating Semantics & Individual Delta EF
    // =========================================================================
    @Nested
    @DisplayName("Group 3: Rating Semantics & Delta EF Precision")
    class Group3RatingSemanticsTests {

        @Test
        @DisplayName("Again (Rating 1, q=0) -> Delta EF = -0.80, resets repetitions & interval to 0")
        void testAgainSemantics() {
            SrsCalculationResult result = calculator.calculate(new BigDecimal("2.50"), 15, 3, ReviewRating.AGAIN);

            // 2.50 - 0.80 = 1.70
            assertThat(result.easeFactor()).isEqualTo(new BigDecimal("1.70"));
            assertThat(result.intervalDays()).isEqualTo(0);
            assertThat(result.repetitions()).isEqualTo(0);
        }

        @Test
        @DisplayName("Hard (Rating 2, q=3) -> Delta EF = -0.14, increments repetitions, progresses interval")
        void testHardSemantics() {
            SrsCalculationResult result = calculator.calculate(new BigDecimal("2.50"), 6, 2, ReviewRating.HARD);

            // 2.50 - 0.14 = 2.36, interval = ceil(6 * 2.36) = ceil(14.16) = 15
            assertThat(result.easeFactor()).isEqualTo(new BigDecimal("2.36"));
            assertThat(result.intervalDays()).isEqualTo(15);
            assertThat(result.repetitions()).isEqualTo(3);
        }

        @Test
        @DisplayName("Good (Rating 3, q=4) -> Delta EF = 0.00 (Neutral), increments repetitions, progresses interval")
        void testGoodSemantics() {
            SrsCalculationResult result = calculator.calculate(new BigDecimal("2.50"), 6, 2, ReviewRating.GOOD);

            // 2.50 + 0.00 = 2.50, interval = ceil(6 * 2.50) = 15
            assertThat(result.easeFactor()).isEqualTo(new BigDecimal("2.50"));
            assertThat(result.intervalDays()).isEqualTo(15);
            assertThat(result.repetitions()).isEqualTo(3);
        }

        @Test
        @DisplayName("Easy (Rating 4, q=5) -> Delta EF = +0.10, increments repetitions, progresses interval")
        void testEasySemantics() {
            SrsCalculationResult result = calculator.calculate(new BigDecimal("2.50"), 6, 2, ReviewRating.EASY);

            // 2.50 + 0.10 = 2.60, interval = ceil(6 * 2.60) = ceil(15.60) = 16
            assertThat(result.easeFactor()).isEqualTo(new BigDecimal("2.60"));
            assertThat(result.intervalDays()).isEqualTo(16);
            assertThat(result.repetitions()).isEqualTo(3);
        }
    }

    // =========================================================================
    // GROUP 4: Initial Card Calculations (CARD_PROGRESS Initial State)
    // =========================================================================
    @Nested
    @DisplayName("Group 4: Initial Card State Transitions (R=0, I=0, EF=2.50)")
    class Group4InitialCardTests {

        @ParameterizedTest(name = "Rating {0} -> Expected EF={1}, Interval={2}, Repetitions={3}")
        @CsvSource({
                "1, 1.70, 0, 0",
                "2, 2.36, 1, 1",
                "3, 2.50, 1, 1",
                "4, 2.60, 1, 1"
        })
        @DisplayName("Initial review vectors match exact independent mathematical oracle")
        void testInitialReviewVectors(int ratingValue, String expectedEf, int expectedInterval, int expectedRepetitions) {
            SrsCalculationResult result = calculator.calculateInitial(ratingValue);

            assertThat(result.easeFactor()).isEqualTo(new BigDecimal(expectedEf));
            assertThat(result.intervalDays()).isEqualTo(expectedInterval);
            assertThat(result.repetitions()).isEqualTo(expectedRepetitions);
        }
    }

    // =========================================================================
    // GROUP 5: First & Second Repetitions Fixed Progression
    // =========================================================================
    @Nested
    @DisplayName("Group 5: Fixed Interval Progression for Repetition 1 (1 day) & Repetition 2 (6 days)")
    class Group5FixedIntervalProgressionTests {

        @ParameterizedTest(name = "Passing rating {0} from R=0 must produce I=1 day")
        @EnumSource(value = ReviewRating.class, names = {"HARD", "GOOD", "EASY"})
        @DisplayName("First successful repetition always produces interval = 1 day, independent of starting EF")
        void testFirstRepetition_alwaysOneDay(ReviewRating rating) {
            SrsCalculationResult result1 = calculator.calculate(new BigDecimal("2.10"), 0, 0, rating);
            SrsCalculationResult result2 = calculator.calculate(new BigDecimal("2.80"), 0, 0, rating);

            assertThat(result1.intervalDays()).isEqualTo(1);
            assertThat(result1.repetitions()).isEqualTo(1);
            assertThat(result2.intervalDays()).isEqualTo(1);
            assertThat(result2.repetitions()).isEqualTo(1);
        }

        @ParameterizedTest(name = "Passing rating {0} from R=1, I=1 must produce I=6 days")
        @EnumSource(value = ReviewRating.class, names = {"HARD", "GOOD", "EASY"})
        @DisplayName("Second successful repetition always produces interval = 6 days, independent of starting EF")
        void testSecondRepetition_alwaysSixDays(ReviewRating rating) {
            SrsCalculationResult result1 = calculator.calculate(new BigDecimal("2.10"), 1, 1, rating);
            SrsCalculationResult result2 = calculator.calculate(new BigDecimal("2.80"), 1, 1, rating);

            assertThat(result1.intervalDays()).isEqualTo(6);
            assertThat(result1.repetitions()).isEqualTo(2);
            assertThat(result2.intervalDays()).isEqualTo(6);
            assertThat(result2.repetitions()).isEqualTo(2);
        }
    }

    // =========================================================================
    // GROUP 6: Third+ Repetitions & Math.ceil Fractional Rounding
    // =========================================================================
    @Nested
    @DisplayName("Group 6: Third+ Repetition Calculations & Fractional Ceil Rounding")
    class Group6ThirdPlusRepetitionsTests {

        static Stream<Arguments> thirdPlusRepetitionOracleVectors() {
            return Stream.of(
                    // currentEf, currentInterval, currentRepetitions, rating, expectedEf, expectedInterval, expectedRepetitions
                    // R=2 -> R'=3, I=6 * EF'
                    Arguments.of(new BigDecimal("2.50"), 6, 2, ReviewRating.GOOD, new BigDecimal("2.50"), 15, 3), // 6 * 2.50 = 15.0 -> 15
                    Arguments.of(new BigDecimal("2.50"), 6, 2, ReviewRating.HARD, new BigDecimal("2.36"), 15, 3), // 6 * 2.36 = 14.16 -> ceil = 15
                    Arguments.of(new BigDecimal("2.50"), 6, 2, ReviewRating.EASY, new BigDecimal("2.60"), 16, 3), // 6 * 2.60 = 15.60 -> ceil = 16

                    // R=3 -> R'=4
                    Arguments.of(new BigDecimal("2.50"), 15, 3, ReviewRating.GOOD, new BigDecimal("2.50"), 38, 4), // 15 * 2.50 = 37.50 -> ceil = 38
                    Arguments.of(new BigDecimal("2.36"), 15, 3, ReviewRating.GOOD, new BigDecimal("2.36"), 36, 4), // 15 * 2.36 = 35.40 -> ceil = 36
                    Arguments.of(new BigDecimal("2.60"), 16, 3, ReviewRating.EASY, new BigDecimal("2.70"), 44, 4), // 16 * 2.70 = 43.20 -> ceil = 44
                    Arguments.of(new BigDecimal("2.36"), 15, 3, ReviewRating.HARD, new BigDecimal("2.22"), 34, 4), // 15 * 2.22 = 33.30 -> ceil = 34

                    // Exact integral multiplication (no accidental +1 rounding)
                    Arguments.of(new BigDecimal("2.00"), 10, 3, ReviewRating.GOOD, new BigDecimal("2.00"), 20, 4), // 10 * 2.00 = 20.0 -> 20
                    Arguments.of(new BigDecimal("2.50"), 10, 3, ReviewRating.GOOD, new BigDecimal("2.50"), 25, 4)  // 10 * 2.50 = 25.0 -> 25
            );
        }

        @ParameterizedTest(name = "{index} => EF={0}, I={1}, R={2}, rating={3} -> Expected EF={4}, I={5}, R={6}")
        @MethodSource("thirdPlusRepetitionOracleVectors")
        @DisplayName("Third+ repetition vectors verify exact formula and ceiling behavior")
        void testThirdPlusRepetitions(
                BigDecimal currentEf,
                int currentInterval,
                int currentRepetitions,
                ReviewRating rating,
                BigDecimal expectedEf,
                int expectedInterval,
                int expectedRepetitions) {

            SrsCalculationResult result = calculator.calculate(currentEf, currentInterval, currentRepetitions, rating);

            assertThat(result.easeFactor()).isEqualTo(expectedEf);
            assertThat(result.intervalDays()).isEqualTo(expectedInterval);
            assertThat(result.repetitions()).isEqualTo(expectedRepetitions);
        }
    }

    // =========================================================================
    // GROUP 7: Ease Factor Floor Clamping (EF >= 1.30)
    // =========================================================================
    @Nested
    @DisplayName("Group 7: Ease Factor Floor & Boundaries (EF >= 1.30)")
    class Group7EaseFactorFloorTests {

        @ParameterizedTest(name = "Starting EF={0}, rating={1} -> EF must not drop below 1.30 floor")
        @CsvSource({
                "1.30, 1, 1.30", // 1.30 - 0.80 = 0.50 -> clamped to 1.30
                "1.35, 2, 1.30", // 1.35 - 0.14 = 1.21 -> clamped to 1.30
                "1.40, 1, 1.30", // 1.40 - 0.80 = 0.60 -> clamped to 1.30
                "1.43, 2, 1.30", // 1.43 - 0.14 = 1.29 -> clamped to 1.30
                "1.30, 2, 1.30"  // 1.30 - 0.14 = 1.16 -> clamped to 1.30
        })
        @DisplayName("Calculated EF strictly respects 1.30 lower bound floor")
        void testEfFloor_clamping(String initialEf, int ratingValue, String expectedEf) {
            SrsCalculationResult result = calculator.calculate(new BigDecimal(initialEf), 10, 3, ratingValue);

            assertThat(result.easeFactor()).isEqualTo(new BigDecimal(expectedEf));
            assertThat(result.easeFactor()).isGreaterThanOrEqualTo(new BigDecimal("1.30"));
        }

        @Test
        @DisplayName("EF at floor 1.30 can increase when rating is Easy (not permanently locked at floor)")
        void testEfFloor_canRecoverOnEasy() {
            SrsCalculationResult result = calculator.calculate(new BigDecimal("1.30"), 10, 3, ReviewRating.EASY);

            // 1.30 + 0.10 = 1.40
            assertThat(result.easeFactor()).isEqualTo(new BigDecimal("1.40"));
            assertThat(result.intervalDays()).isEqualTo((int) Math.ceil(10 * 1.40)); // 14
            assertThat(result.repetitions()).isEqualTo(4);
        }

        @Test
        @DisplayName("EF at floor 1.30 remains 1.30 when rating is Good")
        void testEfFloor_remainsOnGood() {
            SrsCalculationResult result = calculator.calculate(new BigDecimal("1.30"), 10, 3, ReviewRating.GOOD);

            // 1.30 + 0.00 = 1.30
            assertThat(result.easeFactor()).isEqualTo(new BigDecimal("1.30"));
            assertThat(result.intervalDays()).isEqualTo((int) Math.ceil(10 * 1.30)); // 13
            assertThat(result.repetitions()).isEqualTo(4);
        }
    }

    // =========================================================================
    // GROUP 8: Multi-Step Review Lifecycle Sequences
    // =========================================================================
    @Nested
    @DisplayName("Group 8: Multi-Step Deterministic Review Sequences")
    class Group8MultiStepSequenceTests {

        @Test
        @DisplayName("Standard learning sequence (Initial -> Good -> Good -> Good -> Hard -> Easy)")
        void testStandardLearningSequence() {
            // Step 0: Brand new card
            SrsCalculationResult r1 = calculator.calculateInitial(ReviewRating.GOOD);
            assertThat(r1.easeFactor()).isEqualTo(new BigDecimal("2.50"));
            assertThat(r1.intervalDays()).isEqualTo(1);
            assertThat(r1.repetitions()).isEqualTo(1);

            // Step 1: Second review with Good
            SrsCalculationResult r2 = calculator.calculate(r1.easeFactor(), r1.intervalDays(), r1.repetitions(), ReviewRating.GOOD);
            assertThat(r2.easeFactor()).isEqualTo(new BigDecimal("2.50"));
            assertThat(r2.intervalDays()).isEqualTo(6);
            assertThat(r2.repetitions()).isEqualTo(2);

            // Step 2: Third review with Good
            SrsCalculationResult r3 = calculator.calculate(r2.easeFactor(), r2.intervalDays(), r2.repetitions(), ReviewRating.GOOD);
            assertThat(r3.easeFactor()).isEqualTo(new BigDecimal("2.50"));
            assertThat(r3.intervalDays()).isEqualTo(15); // ceil(6 * 2.50) = 15
            assertThat(r3.repetitions()).isEqualTo(3);

            // Step 3: Fourth review with Hard
            SrsCalculationResult r4 = calculator.calculate(r3.easeFactor(), r3.intervalDays(), r3.repetitions(), ReviewRating.HARD);
            assertThat(r4.easeFactor()).isEqualTo(new BigDecimal("2.36"));
            assertThat(r4.intervalDays()).isEqualTo(36); // ceil(15 * 2.36) = ceil(35.40) = 36
            assertThat(r4.repetitions()).isEqualTo(4);

            // Step 4: Fifth review with Easy
            SrsCalculationResult r5 = calculator.calculate(r4.easeFactor(), r4.intervalDays(), r4.repetitions(), ReviewRating.EASY);
            assertThat(r5.easeFactor()).isEqualTo(new BigDecimal("2.46"));
            assertThat(r5.intervalDays()).isEqualTo(89); // ceil(36 * 2.46) = ceil(88.56) = 89
            assertThat(r5.repetitions()).isEqualTo(5);
        }

        @Test
        @DisplayName("Failure, Reset and Recovery sequence (Good -> Good -> Again -> Good -> Easy)")
        void testFailureAndRecoverySequence() {
            // Establish a card with 2 repetitions
            SrsCalculationResult r1 = calculator.calculate(new BigDecimal("2.50"), 1, 1, ReviewRating.GOOD);
            assertThat(r1.intervalDays()).isEqualTo(6);
            assertThat(r1.repetitions()).isEqualTo(2);

            // Step 2: User forgets card -> Again
            SrsCalculationResult r2 = calculator.calculate(r1.easeFactor(), r1.intervalDays(), r1.repetitions(), ReviewRating.AGAIN);
            assertThat(r2.easeFactor()).isEqualTo(new BigDecimal("1.70"));
            assertThat(r2.intervalDays()).isEqualTo(0);
            assertThat(r2.repetitions()).isEqualTo(0);

            // Step 3: Immediate relearn review -> Good
            SrsCalculationResult r3 = calculator.calculate(r2.easeFactor(), r2.intervalDays(), r2.repetitions(), ReviewRating.GOOD);
            assertThat(r3.easeFactor()).isEqualTo(new BigDecimal("1.70"));
            assertThat(r3.intervalDays()).isEqualTo(1);
            assertThat(r3.repetitions()).isEqualTo(1);

            // Step 4: Second review after relearn -> Easy
            SrsCalculationResult r4 = calculator.calculate(r3.easeFactor(), r3.intervalDays(), r3.repetitions(), ReviewRating.EASY);
            assertThat(r4.easeFactor()).isEqualTo(new BigDecimal("1.80")); // 1.70 + 0.10
            assertThat(r4.intervalDays()).isEqualTo(6);
            assertThat(r4.repetitions()).isEqualTo(2);

            // Step 5: Third review after relearn -> Good
            SrsCalculationResult r5 = calculator.calculate(r4.easeFactor(), r4.intervalDays(), r4.repetitions(), ReviewRating.GOOD);
            assertThat(r5.easeFactor()).isEqualTo(new BigDecimal("1.80"));
            assertThat(r5.intervalDays()).isEqualTo(11); // ceil(6 * 1.80) = ceil(10.80) = 11
            assertThat(r5.repetitions()).isEqualTo(3);
        }
    }

    // =========================================================================
    // GROUP 9: Determinism, Statelessness & Overload Consistency
    // =========================================================================
    @Nested
    @DisplayName("Group 9: Determinism & Method Overload Parity")
    class Group9DeterminismAndOverloadTests {

        @Test
        @DisplayName("Pure function determinism: repeated invocations with same parameters produce identical results")
        void testDeterminism_repeatedCalls() {
            SrsCalculationResult resA1 = calculator.calculate(new BigDecimal("2.36"), 15, 3, ReviewRating.EASY);
            SrsCalculationResult resB = calculator.calculate(new BigDecimal("1.80"), 6, 2, ReviewRating.HARD);
            SrsCalculationResult resA2 = calculator.calculate(new BigDecimal("2.36"), 15, 3, ReviewRating.EASY);

            assertThat(resA1).isEqualTo(resA2);
            assertThat(resA1.hashCode()).isEqualTo(resA2.hashCode());
            assertThat(resA1).isNotEqualTo(resB);
        }

        @Test
        @DisplayName("Method overloads (enum, integer rating, double EF) produce identical results")
        void testMethodOverloads_produceIdenticalResults() {
            SrsCalculationResult resEnum = calculator.calculate(new BigDecimal("2.50"), 6, 2, ReviewRating.GOOD);
            SrsCalculationResult resInt = calculator.calculate(new BigDecimal("2.50"), 6, 2, 3);
            SrsCalculationResult resDouble = calculator.calculate(2.50, 6, 2, 3);

            assertThat(resEnum).isEqualTo(resInt);
            assertThat(resEnum).isEqualTo(resDouble);
        }

        @Test
        @DisplayName("Initial overloads (enum vs int) produce identical results")
        void testInitialOverloads_produceIdenticalResults() {
            SrsCalculationResult resEnum = calculator.calculateInitial(ReviewRating.HARD);
            SrsCalculationResult resInt = calculator.calculateInitial(2);

            assertThat(resEnum).isEqualTo(resInt);
        }
    }

    // =========================================================================
    // GROUP 10: Boundary & Overflow Protections
    // =========================================================================
    @Nested
    @DisplayName("Group 10: Large Value & Overflow Boundary Safety")
    class Group10LargeValueTests {

        @Test
        @DisplayName("Large valid intervals do not produce negative numbers or arithmetic overflow")
        void testLargeInterval_safeCalculation() {
            SrsCalculationResult result = calculator.calculate(new BigDecimal("2.50"), 100_000, 10, ReviewRating.GOOD);

            assertThat(result.easeFactor()).isEqualTo(new BigDecimal("2.50"));
            assertThat(result.intervalDays()).isEqualTo(250_000);
            assertThat(result.repetitions()).isEqualTo(11);
        }

        @Test
        @DisplayName("Extremely large interval calculation clamps to Integer.MAX_VALUE without overflow")
        void testExtremelyLargeInterval_clampsToMaxInteger() {
            SrsCalculationResult result = calculator.calculate(new BigDecimal("3.00"), 1_000_000_000, 20, ReviewRating.EASY);

            assertThat(result.easeFactor()).isEqualTo(new BigDecimal("3.10"));
            assertThat(result.intervalDays()).isEqualTo(Integer.MAX_VALUE);
            assertThat(result.repetitions()).isEqualTo(21);
        }
    }
}
