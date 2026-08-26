package com.elearning;

import com.elearning.controller.UserProfileController;
import com.elearning.dto.request.UpdateProfileRequest;
import com.elearning.dto.response.UserProfileResponse;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileController MockMvc Contract Tests")
class UserProfileControllerTests {

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private UserProfileController userProfileController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userProfileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GIVEN authenticated request WHEN GET /api/v1/users/profile THEN returns 200 OK with UserProfileResponse")
    void testGetProfileSuccess() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
                1L, 100L, "learner@elearning.com", "Tran Van B", "https://avatar.url/b.jpg",
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(userProfileService.getCurrentUserProfile()).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.accountId").value(100))
                .andExpect(jsonPath("$.data.emailOrPhone").value("learner@elearning.com"))
                .andExpect(jsonPath("$.data.fullName").value("Tran Van B"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://avatar.url/b.jpg"));
    }

    @Test
    @DisplayName("GIVEN valid UpdateProfileRequest WHEN PUT /api/v1/users/profile THEN returns 200 OK with updated profile")
    void testUpdateProfileSuccess() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Tran Van B Modified", "https://avatar.url/new.jpg");
        UserProfileResponse response = new UserProfileResponse(
                1L, 100L, "learner@elearning.com", "Tran Van B Modified", "https://avatar.url/new.jpg",
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(userProfileService.updateCurrentUserProfile(any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.fullName").value("Tran Van B Modified"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://avatar.url/new.jpg"));
    }

    @Test
    @DisplayName("GIVEN invalid UpdateProfileRequest (blank fullName) WHEN PUT /api/v1/users/profile THEN returns 400 Bad Request")
    void testUpdateProfileValidationFailure() throws Exception {
        UpdateProfileRequest invalidRequest = new UpdateProfileRequest("", "https://avatar.url/new.jpg");

        mockMvc.perform(put("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
