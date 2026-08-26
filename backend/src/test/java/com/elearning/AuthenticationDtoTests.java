package com.elearning;

import com.elearning.dto.request.LoginRequest;
import com.elearning.dto.request.RegisterRequest;
import com.elearning.dto.response.AuthResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Authentication DTOs & Validation Tests")
class AuthenticationDtoTests {

    private static Validator validator;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("RegisterRequest Validation Tests")
    class RegisterRequestValidationTests {

        @Test
        @DisplayName("GIVEN valid email, password, and fullName WHEN validated THEN no violations")
        void testValidRegisterRequestWithEmail() {
            RegisterRequest request = new RegisterRequest("learner@elearning.com", "validPass123", "Nguyen Van A");
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("GIVEN valid phone number, password, and fullName WHEN validated THEN no violations")
        void testValidRegisterRequestWithPhone() {
            RegisterRequest request = new RegisterRequest("0912345678", "validPass123", "Tran Thi B");
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("GIVEN null, empty, or blank emailOrPhone WHEN validated THEN fails with NotBlank violation")
        void testBlankEmailOrPhone() {
            RegisterRequest nullEmail = new RegisterRequest(null, "validPass123", "Nguyen Van A");
            RegisterRequest emptyEmail = new RegisterRequest("", "validPass123", "Nguyen Van A");
            RegisterRequest blankEmail = new RegisterRequest("   ", "validPass123", "Nguyen Van A");

            assertThat(validator.validate(nullEmail)).anyMatch(v -> v.getPropertyPath().toString().equals("emailOrPhone"));
            assertThat(validator.validate(emptyEmail)).anyMatch(v -> v.getPropertyPath().toString().equals("emailOrPhone"));
            assertThat(validator.validate(blankEmail)).anyMatch(v -> v.getPropertyPath().toString().equals("emailOrPhone"));
        }

        @Test
        @DisplayName("GIVEN emailOrPhone exceeding 191 characters WHEN validated THEN fails with Size violation")
        void testEmailOrPhoneExceedingMax() {
            String longEmail = "a".repeat(180) + "@elearning.com"; // > 191 chars
            RegisterRequest request = new RegisterRequest(longEmail, "validPass123", "Nguyen Van A");

            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("emailOrPhone"));
        }

        @Test
        @DisplayName("GIVEN null, empty, or blank password WHEN validated THEN fails with NotBlank violation")
        void testBlankPassword() {
            RegisterRequest nullPass = new RegisterRequest("user@elearning.com", null, "Nguyen Van A");
            RegisterRequest emptyPass = new RegisterRequest("user@elearning.com", "", "Nguyen Van A");
            RegisterRequest blankPass = new RegisterRequest("user@elearning.com", "   ", "Nguyen Van A");

            assertThat(validator.validate(nullPass)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
            assertThat(validator.validate(emptyPass)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
            assertThat(validator.validate(blankPass)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("GIVEN password boundaries WHEN validated THEN rejects < 6 or > 100, accepts 6 and 100")
        void testPasswordLengthBoundaries() {
            RegisterRequest tooShort = new RegisterRequest("user@elearning.com", "12345", "Nguyen Van A"); // 5
            RegisterRequest exactMin = new RegisterRequest("user@elearning.com", "123456", "Nguyen Van A"); // 6
            RegisterRequest exactMax = new RegisterRequest("user@elearning.com", "p".repeat(100), "Nguyen Van A"); // 100
            RegisterRequest tooLong = new RegisterRequest("user@elearning.com", "p".repeat(101), "Nguyen Van A"); // 101

            assertThat(validator.validate(tooShort)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
            assertThat(validator.validate(exactMin)).isEmpty();
            assertThat(validator.validate(exactMax)).isEmpty();
            assertThat(validator.validate(tooLong)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("GIVEN null, empty, blank or > 100 fullName WHEN validated THEN fails validation")
        void testFullNameConstraints() {
            RegisterRequest nullName = new RegisterRequest("user@elearning.com", "validPass", null);
            RegisterRequest emptyName = new RegisterRequest("user@elearning.com", "validPass", "");
            RegisterRequest blankName = new RegisterRequest("user@elearning.com", "validPass", "   ");
            RegisterRequest tooLongName = new RegisterRequest("user@elearning.com", "validPass", "a".repeat(101));

            assertThat(validator.validate(nullName)).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
            assertThat(validator.validate(emptyName)).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
            assertThat(validator.validate(blankName)).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
            assertThat(validator.validate(tooLongName)).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
        }
    }

    @Nested
    @DisplayName("LoginRequest Validation Tests")
    class LoginRequestValidationTests {

        @Test
        @DisplayName("GIVEN valid credentials WHEN validated THEN no violations")
        void testValidLoginRequest() {
            LoginRequest request = new LoginRequest("user@elearning.com", "password123");
            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("GIVEN null or blank emailOrPhone WHEN validated THEN fails with NotBlank violation")
        void testBlankLoginIdentifier() {
            LoginRequest nullId = new LoginRequest(null, "password123");
            LoginRequest emptyId = new LoginRequest("", "password123");
            LoginRequest blankId = new LoginRequest("   ", "password123");

            assertThat(validator.validate(nullId)).anyMatch(v -> v.getPropertyPath().toString().equals("emailOrPhone"));
            assertThat(validator.validate(emptyId)).anyMatch(v -> v.getPropertyPath().toString().equals("emailOrPhone"));
            assertThat(validator.validate(blankId)).anyMatch(v -> v.getPropertyPath().toString().equals("emailOrPhone"));
        }

        @Test
        @DisplayName("GIVEN null or blank password WHEN validated THEN fails with NotBlank violation")
        void testBlankLoginPassword() {
            LoginRequest nullPass = new LoginRequest("user@elearning.com", null);
            LoginRequest emptyPass = new LoginRequest("user@elearning.com", "");
            LoginRequest blankPass = new LoginRequest("user@elearning.com", "   ");

            assertThat(validator.validate(nullPass)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
            assertThat(validator.validate(emptyPass)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
            assertThat(validator.validate(blankPass)).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }
    }

    @Nested
    @DisplayName("JSON Serialization & Deserialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("GIVEN AuthResponse WHEN serialized to JSON THEN contains documented fields and NO sensitive/entity data")
        void testAuthResponseSerialization() throws Exception {
            AuthResponse response = new AuthResponse(
                    "header.payload.signature",
                    "Bearer",
                    42L,
                    "learner@elearning.com",
                    "Nguyen Van A",
                    List.of("Learner")
            );

            String json = objectMapper.writeValueAsString(response);
            JsonNode rootNode = objectMapper.readTree(json);

            // Required fields
            assertThat(rootNode.has("token")).isTrue();
            assertThat(rootNode.get("token").asText()).isEqualTo("header.payload.signature");
            assertThat(rootNode.has("type")).isTrue();
            assertThat(rootNode.get("type").asText()).isEqualTo("Bearer");
            assertThat(rootNode.has("accountId")).isTrue();
            assertThat(rootNode.get("accountId").asLong()).isEqualTo(42L);
            assertThat(rootNode.has("emailOrPhone")).isTrue();
            assertThat(rootNode.get("emailOrPhone").asText()).isEqualTo("learner@elearning.com");
            assertThat(rootNode.has("fullName")).isTrue();
            assertThat(rootNode.get("fullName").asText()).isEqualTo("Nguyen Van A");
            assertThat(rootNode.has("roles")).isTrue();
            assertThat(rootNode.get("roles").get(0).asText()).isEqualTo("Learner");

            // Absolute security isolation: NO password, passwordHash, secret, user entity
            assertThat(rootNode.has("password")).isFalse();
            assertThat(rootNode.has("passwordHash")).isFalse();
            assertThat(rootNode.has("secret")).isFalse();
            assertThat(rootNode.has("account")).isFalse();
            assertThat(rootNode.has("userProfile")).isFalse();
            assertThat(rootNode.has("expiresIn")).isFalse();
            assertThat(rootNode.has("refreshToken")).isFalse();
        }

        @Test
        @DisplayName("GIVEN JSON with accessToken/tokenType aliases WHEN deserialized into AuthResponse THEN maps correctly")
        void testAuthResponseDeserializationWithAliases() throws Exception {
            String jsonWithAliases = "{"
                    + "\"accessToken\": \"jwt.test.token\","
                    + "\"tokenType\": \"Bearer\","
                    + "\"accountId\": 10,"
                    + "\"emailOrPhone\": \"alias@elearning.com\","
                    + "\"fullName\": \"Alias User\","
                    + "\"roles\": [\"Admin\", \"Creator\"]"
                    + "}";

            AuthResponse response = objectMapper.readValue(jsonWithAliases, AuthResponse.class);

            assertThat(response.getToken()).isEqualTo("jwt.test.token");
            assertThat(response.getType()).isEqualTo("Bearer");
            assertThat(response.getAccountId()).isEqualTo(10L);
            assertThat(response.getEmailOrPhone()).isEqualTo("alias@elearning.com");
            assertThat(response.getFullName()).isEqualTo("Alias User");
            assertThat(response.getRoles()).containsExactly("Admin", "Creator");
        }

        @Test
        @DisplayName("GIVEN JSON input WHEN deserialized into RegisterRequest and LoginRequest THEN maps fields correctly")
        void testRequestDeserialization() throws Exception {
            String registerJson = "{"
                    + "\"emailOrPhone\": \"newuser@elearning.com\","
                    + "\"password\": \"securePassword123\","
                    + "\"fullName\": \"Hoang Thi C\""
                    + "}";

            RegisterRequest regReq = objectMapper.readValue(registerJson, RegisterRequest.class);
            assertThat(regReq.getEmailOrPhone()).isEqualTo("newuser@elearning.com");
            assertThat(regReq.getPassword()).isEqualTo("securePassword123");
            assertThat(regReq.getFullName()).isEqualTo("Hoang Thi C");

            String loginJson = "{"
                    + "\"emailOrPhone\": \"loginuser@elearning.com\","
                    + "\"password\": \"loginPass123\""
                    + "}";

            LoginRequest logReq = objectMapper.readValue(loginJson, LoginRequest.class);
            assertThat(logReq.getEmailOrPhone()).isEqualTo("loginuser@elearning.com");
            assertThat(logReq.getPassword()).isEqualTo("loginPass123");
        }

        @Test
        @DisplayName("GIVEN toString called on DTOs WHEN executed THEN credentials are masked and never printed")
        void testToStringMasksCredentials() {
            RegisterRequest reg = new RegisterRequest("user@test.com", "superSecretPass", "User Name");
            LoginRequest log = new LoginRequest("user@test.com", "superSecretPass");
            AuthResponse auth = new AuthResponse("secret.jwt.token", "Bearer", 1L, "user@test.com", "User Name", List.of("Learner"));

            assertThat(reg.toString()).contains("[PROTECTED]").doesNotContain("superSecretPass");
            assertThat(log.toString()).contains("[PROTECTED]").doesNotContain("superSecretPass");
            assertThat(auth.toString()).contains("[PROTECTED]").doesNotContain("secret.jwt.token");
        }
    }
}
