package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.response.ApiResponse;
import com.elearning.exception.BusinessException;
import com.elearning.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler Contract & Behavior Tests")
class GlobalExceptionHandlerTests {

    private GlobalExceptionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        objectMapper = new ObjectMapper();
    }

    // Dummy method for constructing MethodArgumentNotValidException
    public void dummyMethod(String param) {}

    private MethodParameter getDummyParameter() {
        try {
            Method method = this.getClass().getMethod("dummyMethod", String.class);
            return new MethodParameter(method, 0);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException Tests")
    class ValidationExceptionTests {

        @Test
        @DisplayName("GIVEN single field validation failure WHEN handled THEN returns HTTP 400 and VALIDATION_ERROR with field message")
        void testSingleFieldValidationError() throws Exception {
            // GIVEN
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "email", "Email must not be blank"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(getDummyParameter(), bindingResult);

            // WHEN
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
            assertThat(body.getMessage()).isEqualTo(ErrorCode.VALIDATION_ERROR.getDefaultMessage());
            assertThat(body.getErrors()).containsExactly("email: Email must not be blank");
            assertThat(body.getData()).isNull();

            // JSON serialization assertion
            String json = objectMapper.writeValueAsString(body);
            JsonNode root = objectMapper.readTree(json);
            assertThat(root.get("code").asText()).isEqualTo("VALIDATION_ERROR");
            assertThat(root.get("errors").isArray()).isTrue();
            assertThat(root.get("errors").get(0).asText()).isEqualTo("email: Email must not be blank");
        }

        @Test
        @DisplayName("GIVEN multiple field validation failures WHEN handled THEN returns HTTP 400 and all error messages in errors array")
        void testMultipleFieldValidationErrors() {
            // GIVEN
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "email", "Email must not be blank"));
            bindingResult.addError(new FieldError("request", "password", "Password must be at least 6 characters"));
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(getDummyParameter(), bindingResult);

            // WHEN
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getErrors()).containsExactlyInAnyOrder(
                    "email: Email must not be blank",
                    "password: Password must be at least 6 characters"
            );
        }

        @Test
        @DisplayName("GIVEN empty validation errors WHEN handled THEN returns empty errors array without throwing secondary exceptions")
        void testEmptyValidationErrors() {
            // GIVEN
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(getDummyParameter(), bindingResult);

            // WHEN
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getErrors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("BusinessException Tests")
    class BusinessExceptionTests {

        @Test
        @DisplayName("GIVEN BusinessException with NOT_FOUND WHEN handled THEN returns HTTP 404 with NOT_FOUND code")
        void testBusinessExceptionNotFound() throws Exception {
            // GIVEN
            BusinessException ex = new BusinessException(ErrorCode.NOT_FOUND);

            // WHEN
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getCode()).isEqualTo("NOT_FOUND");
            assertThat(body.getMessage()).isEqualTo(ErrorCode.NOT_FOUND.getDefaultMessage());
            assertThat(body.getErrors()).isEmpty();
            assertThat(body.getData()).isNull();

            String json = objectMapper.writeValueAsString(body);
            JsonNode root = objectMapper.readTree(json);
            assertThat(root.get("code").asText()).isEqualTo("NOT_FOUND");
            assertThat(root.get("message").asText()).isEqualTo(ErrorCode.NOT_FOUND.getDefaultMessage());
        }

        @Test
        @DisplayName("GIVEN BusinessException with custom message and CONFLICT WHEN handled THEN preserves custom message and returns HTTP 409")
        void testBusinessExceptionConflictWithCustomMessage() {
            // GIVEN
            BusinessException ex = new BusinessException(ErrorCode.CONFLICT, "Email đã được sử dụng");

            // WHEN
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getCode()).isEqualTo("CONFLICT");
            assertThat(body.getMessage()).isEqualTo("Email đã được sử dụng");
        }

        @Test
        @DisplayName("GIVEN BusinessException with UNPROCESSABLE_ENTITY WHEN handled THEN returns HTTP 422")
        void testBusinessExceptionUnprocessableEntity() {
            // GIVEN
            BusinessException ex = new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "Bài học không có từ vựng");

            // WHEN
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody().getCode()).isEqualTo("UNPROCESSABLE_ENTITY");
        }

        @Test
        @DisplayName("GIVEN BusinessException with UNAUTHORIZED WHEN handled THEN returns HTTP 401")
        void testBusinessExceptionUnauthorized() {
            // GIVEN
            BusinessException ex = new BusinessException(ErrorCode.UNAUTHORIZED);

            // WHEN
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().getCode()).isEqualTo("UNAUTHORIZED");
        }

        @Test
        @DisplayName("GIVEN BusinessException with FORBIDDEN WHEN handled THEN returns HTTP 403")
        void testBusinessExceptionForbidden() {
            // GIVEN
            BusinessException ex = new BusinessException(ErrorCode.FORBIDDEN);

            // WHEN
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().getCode()).isEqualTo("FORBIDDEN");
        }
    }

    @Nested
    @DisplayName("Generic Exception Fallback Tests")
    class GenericFallbackTests {

        @Test
        @DisplayName("GIVEN unexpected RuntimeException WHEN handled THEN returns HTTP 500 and hides internal exception details")
        void testGenericExceptionFallback() throws Exception {
            // GIVEN: Exception containing sensitive internal information (e.g. database query, filesystem path)
            RuntimeException sensitiveEx = new RuntimeException("SQLSyntaxErrorException: SELECT password_hash FROM secret_table WHERE path=/etc/shadow");

            // WHEN
            ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(sensitiveEx);

            // THEN
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
            assertThat(body.getMessage()).isEqualTo(ErrorCode.INTERNAL_ERROR.getDefaultMessage());
            assertThat(body.getErrors()).isEmpty();
            assertThat(body.getData()).isNull();

            // Client response MUST NOT contain any sensitive keyword
            String json = objectMapper.writeValueAsString(body);
            assertThat(json).doesNotContain("SQLSyntaxErrorException");
            assertThat(json).doesNotContain("password_hash");
            assertThat(json).doesNotContain("/etc/shadow");
        }
    }

    @Nested
    @DisplayName("Spring MVC Standalone Resolution & Precedence Tests")
    class PrecedenceAndMvcIntegrationTests {

        // Test DTO for validation test fixture
        static class TestDto {
            @NotBlank(message = "Title is required")
            @Size(min = 2, max = 50, message = "Title must be between 2 and 50 characters")
            private String title;

            public String getTitle() { return title; }
            public void setTitle(String title) { this.title = title; }
        }

        // Test controller fixture strictly inside test class
        @RestController
        static class DummyTestController {
            @PostMapping("/test/validate")
            public String validateEndpoint(@Valid @RequestBody TestDto dto) {
                return "OK";
            }

            @GetMapping("/test/business-error")
            public String businessErrorEndpoint() {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Item not found in test");
            }

            @GetMapping("/test/unexpected-error")
            public String unexpectedErrorEndpoint() {
                throw new NullPointerException("Simulated null pointer");
            }

            @GetMapping("/test/illegal-argument")
            public String illegalArgumentEndpoint() {
                throw new IllegalArgumentException("Internal parameter invalid");
            }
        }

        private MockMvc mockMvc;

        @BeforeEach
        void setupMockMvc() {
            mockMvc = MockMvcBuilders.standaloneSetup(new DummyTestController())
                    .setControllerAdvice(handler)
                    .build();
        }

        @Test
        @DisplayName("GIVEN invalid request body WHEN dispatched through MockMvc THEN validation handler triggers returning 400")
        void testMvcValidationPrecedence() throws Exception {
            mockMvc.perform(post("/test/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.VALIDATION_ERROR.getDefaultMessage()))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("GIVEN business exception thrown WHEN dispatched through MockMvc THEN business handler triggers returning 404")
        void testMvcBusinessExceptionPrecedence() throws Exception {
            mockMvc.perform(get("/test/business-error"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Item not found in test"))
                    .andExpect(jsonPath("$.errors").isEmpty());
        }

        @Test
        @DisplayName("GIVEN unhandled runtime exception thrown WHEN dispatched through MockMvc THEN fallback handler triggers returning 500")
        void testMvcFallbackPrecedence() throws Exception {
            mockMvc.perform(get("/test/unexpected-error"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getDefaultMessage()))
                    .andExpect(jsonPath("$.errors").isEmpty());
        }

        @Test
        @DisplayName("GIVEN IllegalArgumentException thrown WHEN handled THEN returns HTTP 400 with BAD_REQUEST without leaking details")
        void testIllegalArgumentExceptionHandling() {
            IllegalArgumentException ex = new IllegalArgumentException("Internal DB column id must not be null");
            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgumentException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
            assertThat(response.getBody().getMessage()).isEqualTo("Tham số yêu cầu không hợp lệ");
            assertThat(response.getBody().getMessage()).doesNotContain("Internal DB column");
        }

        @Test
        @DisplayName("GIVEN IllegalArgumentException in MVC pipeline THEN returns HTTP 400 BAD_REQUEST")
        void testMvcIllegalArgumentPrecedence() throws Exception {
            mockMvc.perform(get("/test/illegal-argument"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value("Tham số yêu cầu không hợp lệ"));
        }
    }
}
