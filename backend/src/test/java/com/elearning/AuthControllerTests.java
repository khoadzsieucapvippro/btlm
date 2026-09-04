package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.controller.AuthController;
import com.elearning.dto.request.LoginRequest;
import com.elearning.dto.request.RegisterRequest;
import com.elearning.dto.response.AuthResponse;
import com.elearning.exception.BusinessException;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController MockMvc Contract Tests")
class AuthControllerTests {

    @Mock
    private AuthService authService;

    @Mock
    private com.elearning.security.LoginRateLimiter loginRateLimiter;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(loginRateLimiter.tryAcquire(any())).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Registration Endpoint Tests (POST /api/v1/auth/register)")
    class RegisterEndpointTests {

        @Test
        @DisplayName("GIVEN valid RegisterRequest WHEN POST /register THEN returns 201 Created with ApiResponse<AuthResponse>")
        void testRegisterSuccess() throws Exception {
            RegisterRequest request = new RegisterRequest("learner@elearning.com", "securePassword123", "Tran Van A");
            AuthResponse response = new AuthResponse(
                    "jwt.mock.token", "Bearer", 1L, "learner@elearning.com", "Tran Van A", List.of("Learner")
            );

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value(containsString("thành công")))
                    .andExpect(jsonPath("$.data.token").value("jwt.mock.token"))
                    .andExpect(jsonPath("$.data.type").value("Bearer"))
                    .andExpect(jsonPath("$.data.emailOrPhone").value("learner@elearning.com"))
                    .andExpect(jsonPath("$.data.fullName").value("Tran Van A"))
                    .andExpect(jsonPath("$.data.roles[0]").value("Learner"));
        }

        @Test
        @DisplayName("GIVEN invalid RegisterRequest (blank fields) WHEN POST /register THEN returns 400 Bad Request with VALIDATION_ERROR")
        void testRegisterValidationFailure() throws Exception {
            RegisterRequest invalidRequest = new RegisterRequest("", "123", "");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("GIVEN duplicate emailOrPhone WHEN POST /register THEN returns 409 Conflict with CONFLICT code")
        void testRegisterDuplicateConflict() throws Exception {
            RegisterRequest duplicateRequest = new RegisterRequest("existing@elearning.com", "pass123456", "Existing User");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "Email hoặc số điện thoại đã được đăng ký trong hệ thống"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.message").value(containsString("đã được đăng ký")));
        }
    }

    @Nested
    @DisplayName("Login Endpoint Tests (POST /api/v1/auth/login)")
    class LoginEndpointTests {

        @Test
        @DisplayName("GIVEN valid credentials WHEN POST /login THEN returns 200 OK with ApiResponse<AuthResponse>")
        void testLoginSuccess() throws Exception {
            LoginRequest request = new LoginRequest("learner@elearning.com", "securePassword123");
            AuthResponse response = new AuthResponse(
                    "jwt.mock.token", "Bearer", 1L, "learner@elearning.com", "Tran Van A", List.of("Learner")
            );

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.token").value("jwt.mock.token"))
                    .andExpect(jsonPath("$.data.type").value("Bearer"))
                    .andExpect(jsonPath("$.data.emailOrPhone").value("learner@elearning.com"));
        }

        @Test
        @DisplayName("GIVEN invalid LoginRequest (blank password) WHEN POST /login THEN returns 400 Bad Request with VALIDATION_ERROR")
        void testLoginValidationFailure() throws Exception {
            LoginRequest invalidRequest = new LoginRequest("user@elearning.com", "");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GIVEN wrong credentials WHEN POST /login THEN returns 401 Unauthorized with UNAUTHORIZED code")
        void testLoginBadCredentials() throws Exception {
            LoginRequest request = new LoginRequest("user@elearning.com", "wrongPassword");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED, "Email/số điện thoại hoặc mật khẩu không chính xác"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value(containsString("không chính xác")));
        }
    }
}
