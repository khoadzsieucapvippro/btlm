package com.elearning;

import com.elearning.controller.AuthController;
import com.elearning.dto.request.LoginRequest;
import com.elearning.dto.response.AuthResponse;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.security.LoginRateLimiter;
import com.elearning.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 8D.6: AuthController Rate Limiting Integration Tests")
class AuthControllerRateLimitTests {

    @Mock
    private AuthService authService;

    private LoginRateLimiter rateLimiter;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Configured with threshold of 3 attempts
        rateLimiter = new LoginRateLimiter(3, 60);
        AuthController authController = new AuthController(authService, rateLimiter);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GIVEN login requests exceeding rate limit threshold WHEN calling login THEN returns HTTP 429 TOO_MANY_REQUESTS")
    void testRateLimitExceededReturns429() throws Exception {
        AuthResponse mockAuthResponse = new AuthResponse("token123", "Bearer", 1L, "user@example.com", "Test User", List.of("Learner"));
        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

        LoginRequest loginRequest = new LoginRequest("user@example.com", "Password123!");
        String requestJson = objectMapper.writeValueAsString(loginRequest);

        // 3 successful requests within threshold
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        // 4th request triggers HTTP 429 Too Many Requests
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").value("Quá nhiều yêu cầu đăng nhập, vui lòng thử lại sau"));
    }
}
