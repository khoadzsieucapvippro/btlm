package com.elearning;

import com.elearning.controller.UserSrsSettingController;
import com.elearning.dto.request.UpdateSrsSettingRequest;
import com.elearning.dto.response.UserSrsSettingResponse;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.service.UserSrsSettingService;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 8B.1: UserSrsSettingController Unit Tests (MockMvc & Delegation)")
class UserSrsSettingControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserSrsSettingService userSrsSettingService;

    @InjectMocks
    private UserSrsSettingController userSrsSettingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userSrsSettingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("1. GET /api/v1/srs/settings")
    class GetSettingsTests {

        @Test
        @DisplayName("GET /api/v1/srs/settings delegates to userSrsSettingService.getCurrentUserSettings")
        void testGetSettings_delegatesCorrectly() throws Exception {
            UserSrsSettingResponse response = new UserSrsSettingResponse(1L, 20, 100);
            when(userSrsSettingService.getCurrentUserSettings()).thenReturn(response);

            mockMvc.perform(get("/api/v1/srs/settings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.settingId", is(1)))
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(20)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(100)));

            verify(userSrsSettingService).getCurrentUserSettings();
        }
    }

    @Nested
    @DisplayName("2. PUT /api/v1/srs/settings")
    class PutSettingsTests {

        @Test
        @DisplayName("PUT /api/v1/srs/settings with valid positive values delegates to updateCurrentUserSettings")
        void testPutSettings_valid_delegatesCorrectly() throws Exception {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(30, 120);
            UserSrsSettingResponse response = new UserSrsSettingResponse(1L, 30, 120);
            when(userSrsSettingService.updateCurrentUserSettings(any(UpdateSrsSettingRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/v1/srs/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.newCardsPerDay", is(30)))
                    .andExpect(jsonPath("$.data.maxReviewPerDay", is(120)));

            verify(userSrsSettingService).updateCurrentUserSettings(any(UpdateSrsSettingRequest.class));
        }

        @Test
        @DisplayName("PUT with zero newCardsPerDay fails @Positive validation with 400 without calling service")
        void testPutSettings_zeroNewCards_failsValidation() throws Exception {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(0, 100);

            mockMvc.perform(put("/api/v1/srs/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            verify(userSrsSettingService, never()).updateCurrentUserSettings(any());
        }

        @Test
        @DisplayName("PUT with negative maxReviewPerDay fails @Positive validation with 400 without calling service")
        void testPutSettings_negativeMaxReview_failsValidation() throws Exception {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(20, -5);

            mockMvc.perform(put("/api/v1/srs/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            verify(userSrsSettingService, never()).updateCurrentUserSettings(any());
        }

        @Test
        @DisplayName("PUT with null field fails @NotNull validation with 400 without calling service")
        void testPutSettings_nullField_failsValidation() throws Exception {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(null, 100);

            mockMvc.perform(put("/api/v1/srs/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            verify(userSrsSettingService, never()).updateCurrentUserSettings(any());
        }
    }
}
