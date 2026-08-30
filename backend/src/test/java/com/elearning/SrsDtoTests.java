package com.elearning;

import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.dto.response.StudyStatsResponse;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Radical;
import com.elearning.entity.Vocabulary;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 7B.1: SRS DTOs Validation & Mapping Tests")
class SrsDtoTests {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("ReviewCardRequest Validation Tests")
    class ReviewCardRequestValidationTests {

        @Test
        @DisplayName("Valid ReviewCardRequest passes validation")
        void testValidReviewCardRequest_passesValidation() {
            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 100L, 3, 4);
            Set<ConstraintViolation<ReviewCardRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Valid RADICAL ReviewCardRequest passes validation")
        void testValidRadicalReviewCardRequest_passesValidation() {
            ReviewCardRequest request = new ReviewCardRequest("RADICAL", 5L, 4, 0);
            Set<ConstraintViolation<ReviewCardRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Blank itemType fails validation")
        void testBlankItemType_failsValidation() {
            ReviewCardRequest request = new ReviewCardRequest("   ", 100L, 3, 4);
            Set<ConstraintViolation<ReviewCardRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("itemType"));
        }

        @Test
        @DisplayName("Invalid itemType pattern fails validation")
        void testInvalidItemTypePattern_failsValidation() {
            ReviewCardRequest request = new ReviewCardRequest("LESSON", 100L, 3, 4);
            Set<ConstraintViolation<ReviewCardRequest>> violations = validator.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("itemType"));
        }

        @Test
        @DisplayName("Null itemId or non-positive itemId fails validation")
        void testInvalidItemId_failsValidation() {
            ReviewCardRequest nullId = new ReviewCardRequest("VOCABULARY", null, 3, 4);
            assertThat(validator.validate(nullId)).anyMatch(v -> v.getPropertyPath().toString().equals("itemId"));

            ReviewCardRequest nonPositiveId = new ReviewCardRequest("VOCABULARY", 0L, 3, 4);
            assertThat(validator.validate(nonPositiveId)).anyMatch(v -> v.getPropertyPath().toString().equals("itemId"));
        }

        @Test
        @DisplayName("Rating outside 1..4 range fails validation")
        void testInvalidRating_failsValidation() {
            ReviewCardRequest zeroRating = new ReviewCardRequest("VOCABULARY", 100L, 0, 4);
            assertThat(validator.validate(zeroRating)).anyMatch(v -> v.getPropertyPath().toString().equals("rating"));

            ReviewCardRequest fiveRating = new ReviewCardRequest("VOCABULARY", 100L, 5, 4);
            assertThat(validator.validate(fiveRating)).anyMatch(v -> v.getPropertyPath().toString().equals("rating"));
        }

        @Test
        @DisplayName("Negative reviewTimeSeconds fails validation")
        void testNegativeReviewTimeSeconds_failsValidation() {
            ReviewCardRequest negativeTime = new ReviewCardRequest("VOCABULARY", 100L, 3, -1);
            assertThat(validator.validate(negativeTime)).anyMatch(v -> v.getPropertyPath().toString().equals("reviewTimeSeconds"));
        }
    }

    @Nested
    @DisplayName("DueCardResponse Mapping Tests")
    class DueCardResponseMappingTests {

        @Test
        @DisplayName("fromVocabulary mapping with existing progress")
        void testFromVocabulary_withProgress() {
            Vocabulary vocab = new Vocabulary("你好", "nǐ hǎo", "ni hao", "Nễ hảo", "Xin chào");
            vocab.setExampleSentence("你好吗？");
            vocab.setExampleTranslation("Bạn khỏe không?");
            vocab.setVocabId(42L);

            CardProgress progress = new CardProgress();
            progress.setProgressId(10L);
            progress.setItemType("VOCABULARY");
            progress.setItemId(42L);
            progress.setEaseFactor(new BigDecimal("2.50"));
            progress.setIntervalDays(6);
            progress.setRepetitions(2);
            progress.setNextReviewAt(LocalDateTime.now().plusDays(6));

            DueCardResponse response = DueCardResponse.fromVocabulary(vocab, progress);

            assertThat(response.getItemType()).isEqualTo("VOCABULARY");
            assertThat(response.getItemId()).isEqualTo(42L);
            assertThat(response.getHanzi()).isEqualTo("你好");
            assertThat(response.getPinyin()).isEqualTo("nǐ hǎo");
            assertThat(response.getMeaningVi()).isEqualTo("Xin chào");
            assertThat(response.getMeaningHanViet()).isEqualTo("Nễ hảo");
            assertThat(response.getExampleSentence()).isEqualTo("你好吗？");
            assertThat(response.getExampleTranslation()).isEqualTo("Bạn khỏe không?");
            assertThat(response.getStrokeCount()).isNull();
            assertThat(response.getRepetitions()).isEqualTo(2);
            assertThat(response.getIntervalDays()).isEqualTo(6);
            assertThat(response.getEaseFactor()).isEqualTo(new BigDecimal("2.50"));
            assertThat(response.getIsNew()).isFalse();
        }

        @Test
        @DisplayName("fromRadical mapping with null progress (new card)")
        void testFromRadical_nullProgress() {
            Radical radical = new Radical(1, "一", "yī", "Nhất", "Số một");

            DueCardResponse response = DueCardResponse.fromRadical(radical, null);

            assertThat(response.getItemType()).isEqualTo("RADICAL");
            assertThat(response.getItemId()).isEqualTo(1L);
            assertThat(response.getHanzi()).isEqualTo("一");
            assertThat(response.getPinyin()).isEqualTo("yī");
            assertThat(response.getMeaningVi()).isEqualTo("Số một");
            assertThat(response.getMeaningHanViet()).isEqualTo("Nhất");
            assertThat(response.getStrokeCount()).isNull();
            assertThat(response.getExampleSentence()).isNull();
            assertThat(response.getRepetitions()).isEqualTo(0);
            assertThat(response.getIntervalDays()).isEqualTo(0);
            assertThat(response.getEaseFactor()).isEqualTo(new BigDecimal("2.50"));
            assertThat(response.getIsNew()).isTrue();
        }
    }

    @Nested
    @DisplayName("StudyStatsResponse Tests")
    class StudyStatsResponseTests {

        @Test
        @DisplayName("StudyStatsResponse getters and setters")
        void testStudyStatsResponse() {
            StudyStatsResponse stats = new StudyStatsResponse(15L, 25L, 20, 100);

            assertThat(stats.getCardsDue()).isEqualTo(15L);
            assertThat(stats.getReviewsToday()).isEqualTo(25L);
            assertThat(stats.getNewCardsLimit()).isEqualTo(20);
            assertThat(stats.getMaxReviewLimit()).isEqualTo(100);
        }
    }
}
