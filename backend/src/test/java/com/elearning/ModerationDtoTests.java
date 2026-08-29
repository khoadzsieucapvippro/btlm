package com.elearning;

import com.elearning.dto.request.ApproveLessonRequest;
import com.elearning.dto.request.RejectLessonRequest;
import com.elearning.dto.response.ModerationQueueResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 6A.1: Moderation DTO Unit Tests")
class ModerationDtoTests {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("RejectLessonRequest Validation Tests")
    class RejectLessonRequestValidationTests {

        @Test
        @DisplayName("Valid RejectLessonRequest with reason and flaggedFields passes validation")
        void testValidRejectRequest_passes() {
            RejectLessonRequest request = new RejectLessonRequest("Nội dung không chuẩn HSK", "[\"pinyin\", \"meaningVi\"]");
            Set<ConstraintViolation<RejectLessonRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("RejectLessonRequest with null rejectionReason fails with @NotBlank")
        void testNullRejectionReason_fails() {
            RejectLessonRequest request = new RejectLessonRequest(null, null);
            Set<ConstraintViolation<RejectLessonRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("Lý do từ chối không được để trống");
        }

        @Test
        @DisplayName("RejectLessonRequest with blank rejectionReason fails with @NotBlank")
        void testBlankRejectionReason_fails() {
            RejectLessonRequest request = new RejectLessonRequest("   ", null);
            Set<ConstraintViolation<RejectLessonRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("Lý do từ chối không được để trống");
        }

        @Test
        @DisplayName("RejectLessonRequest with oversized rejectionReason (>500 chars) fails with @Size")
        void testOversizedRejectionReason_fails() {
            String longReason = "A".repeat(501);
            RejectLessonRequest request = new RejectLessonRequest(longReason, null);
            Set<ConstraintViolation<RejectLessonRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("Lý do từ chối không được vượt quá 500 ký tự");
        }

        @Test
        @DisplayName("RejectLessonRequest exactly 500 chars passes validation")
        void test500CharsRejectionReason_passes() {
            String exact500 = "B".repeat(500);
            RejectLessonRequest request = new RejectLessonRequest(exact500, null);
            Set<ConstraintViolation<RejectLessonRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("ApproveLessonRequest Validation Tests")
    class ApproveLessonRequestValidationTests {

        @Test
        @DisplayName("Empty ApproveLessonRequest passes validation")
        void testEmptyApproveRequest_passes() {
            ApproveLessonRequest request = new ApproveLessonRequest();
            Set<ConstraintViolation<ApproveLessonRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("ApproveLessonRequest with valid note passes validation")
        void testApproveRequestWithNote_passes() {
            ApproveLessonRequest request = new ApproveLessonRequest("Bài học đạt chuẩn HSK 1");
            Set<ConstraintViolation<ApproveLessonRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("ApproveLessonRequest with oversized note (>500 chars) fails with @Size")
        void testOversizedNote_fails() {
            ApproveLessonRequest request = new ApproveLessonRequest("C".repeat(501));
            Set<ConstraintViolation<ApproveLessonRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("Ghi chú phê duyệt không được vượt quá 500 ký tự");
        }
    }

    @Nested
    @DisplayName("ModerationQueueResponse Mapping & Serialization Tests")
    class ModerationQueueResponseTests {

        @Test
        @DisplayName("fromEntity maps Lesson to ModerationQueueResponse accurately")
        void testFromEntity_mapsCorrectly() {
            Account creator = new Account();
            creator.setAccountId(10L);
            creator.setEmailOrPhone("creator@example.com");

            Lesson lesson = new Lesson("Bài học HSK 1", creator);
            lesson.setLessonId(99L);
            lesson.setStatus("Pending");

            ModerationQueueResponse response = ModerationQueueResponse.fromEntity(lesson, 5);

            assertThat(response).isNotNull();
            assertThat(response.getLessonId()).isEqualTo(99L);
            assertThat(response.getTitle()).isEqualTo("Bài học HSK 1");
            assertThat(response.getStatus()).isEqualTo("Pending");
            assertThat(response.getCreatorId()).isEqualTo(10L);
            assertThat(response.getCreatorEmail()).isEqualTo("creator@example.com");
            assertThat(response.getVocabularyCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("fromEntity returns null when lesson is null")
        void testFromEntity_nullReturnsNull() {
            assertThat(ModerationQueueResponse.fromEntity(null)).isNull();
            assertThat(ModerationQueueResponse.fromEntity(null, 0)).isNull();
        }

        @Test
        @DisplayName("ModerationQueueResponse JSON serialization matches contract")
        void testJsonSerialization() throws Exception {
            LocalDateTime now = LocalDateTime.of(2026, 8, 29, 21, 30, 0);
            ModerationQueueResponse response = new ModerationQueueResponse(
                    101L, "Bài Học 1", "Pending", 5L, "creator@test.com", 8, now, now
            );

            String json = objectMapper.writeValueAsString(response);
            assertThat(json).contains("\"lessonId\":101");
            assertThat(json).contains("\"title\":\"Bài Học 1\"");
            assertThat(json).contains("\"status\":\"Pending\"");
            assertThat(json).contains("\"creatorId\":5");
            assertThat(json).contains("\"creatorEmail\":\"creator@test.com\"");
            assertThat(json).contains("\"vocabularyCount\":8");
        }
    }
}
