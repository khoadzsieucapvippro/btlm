package com.elearning.controller;

import com.elearning.dto.request.UpdateSrsSettingRequest;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.UserSrsSettingResponse;
import com.elearning.service.UserSrsSettingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for managing personal Spaced Repetition System (SRS) study settings.
 * Base path: /api/v1/srs/settings
 * Protected by Spring Security: Requires authenticated user.
 * Enforces thin-controller architecture: delegates directly to {@link UserSrsSettingService}.
 */
@RestController
@RequestMapping("/api/v1/srs/settings")
public class UserSrsSettingController {

    private final UserSrsSettingService userSrsSettingService;

    public UserSrsSettingController(UserSrsSettingService userSrsSettingService) {
        this.userSrsSettingService = userSrsSettingService;
    }

    /**
     * Retrieves the current authenticated user's personalized SRS study settings.
     *
     * @return ApiResponse wrapping UserSrsSettingResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserSrsSettingResponse>> getSettings() {
        UserSrsSettingResponse response = userSrsSettingService.getCurrentUserSettings();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates the current authenticated user's personalized SRS study settings.
     *
     * @param request Validated request payload containing newCardsPerDay and maxReviewPerDay (> 0)
     * @return ApiResponse wrapping updated UserSrsSettingResponse
     */
    @PutMapping
    public ResponseEntity<ApiResponse<UserSrsSettingResponse>> updateSettings(
            @Valid @RequestBody UpdateSrsSettingRequest request) {
        UserSrsSettingResponse response = userSrsSettingService.updateCurrentUserSettings(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình học tập thành công", response));
    }
}
