package com.elearning;

import com.elearning.dto.request.UpdateSrsSettingRequest;
import com.elearning.dto.response.UserSrsSettingResponse;
import com.elearning.entity.Account;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 8B.1: UserSrsSetting DTO Unit & Bean Validation Tests")
class UserSrsSettingDtoTests {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("1. UpdateSrsSettingRequest Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Valid positive integers (20, 100) pass validation")
        void testValidRequest_passes() {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(20, 100);
            Set<ConstraintViolation<UpdateSrsSettingRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Valid boundary value (1, 1) passes validation")
        void testMinimumPositiveValue_passes() {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(1, 1);
            Set<ConstraintViolation<UpdateSrsSettingRequest>> violations = validator.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Zero newCardsPerDay fails @Positive validation")
        void testZeroNewCards_fails() {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(0, 100);
            Set<ConstraintViolation<UpdateSrsSettingRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("newCardsPerDay");
        }

        @Test
        @DisplayName("Negative newCardsPerDay fails @Positive validation")
        void testNegativeNewCards_fails() {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(-5, 100);
            Set<ConstraintViolation<UpdateSrsSettingRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("Zero maxReviewPerDay fails @Positive validation")
        void testZeroMaxReview_fails() {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(20, 0);
            Set<ConstraintViolation<UpdateSrsSettingRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("maxReviewPerDay");
        }

        @Test
        @DisplayName("Negative maxReviewPerDay fails @Positive validation")
        void testNegativeMaxReview_fails() {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(20, -10);
            Set<ConstraintViolation<UpdateSrsSettingRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("Null fields fail @NotNull validation")
        void testNullFields_fail() {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(null, null);
            Set<ConstraintViolation<UpdateSrsSettingRequest>> violations = validator.validate(request);
            assertThat(violations).hasSize(2);
        }
    }

    @Nested
    @DisplayName("2. UserSrsSettingResponse Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("fromEntity maps all fields correctly from entity")
        void testFromEntity_mapsCorrectly() {
            Account acc = new Account();
            acc.setEmailOrPhone("user@test.com");
            acc.setPasswordHash("hash");
            acc.setStatus("Active");
            UserProfile user = new UserProfile();
            user.setUserId(1L);
            user.setAccount(acc);

            UserSrsSetting setting = new UserSrsSetting(user, 35, 150);
            setting.setSettingId(10L);

            UserSrsSettingResponse response = UserSrsSettingResponse.fromEntity(setting);

            assertThat(response.getSettingId()).isEqualTo(10L);
            assertThat(response.getNewCardsPerDay()).isEqualTo(35);
            assertThat(response.getMaxReviewPerDay()).isEqualTo(150);
        }

        @Test
        @DisplayName("fromEntity returns default (20, 100) when entity is null")
        void testFromEntity_nullEntity_returnsDefaults() {
            UserSrsSettingResponse response = UserSrsSettingResponse.fromEntity(null);
            assertThat(response.getSettingId()).isNull();
            assertThat(response.getNewCardsPerDay()).isEqualTo(20);
            assertThat(response.getMaxReviewPerDay()).isEqualTo(100);
        }
    }
}
