package com.elearning.service;

import com.elearning.dto.request.UpdateSrsSettingRequest;
import com.elearning.dto.response.UserSrsSettingResponse;

/**
 * Service interface for managing personal Spaced Repetition System (SRS) study settings.
 */
public interface UserSrsSettingService {

    /**
     * Retrieves the current authenticated user's SRS study settings.
     * Initializes default parameters (20 new cards / 100 max reviews) if not yet configured.
     *
     * @return UserSrsSettingResponse containing daily limits
     */
    UserSrsSettingResponse getCurrentUserSettings();

    /**
     * Updates the current authenticated user's SRS study settings.
     *
     * @param request Validated request containing positive integer daily limits
     * @return Updated UserSrsSettingResponse
     */
    UserSrsSettingResponse updateCurrentUserSettings(UpdateSrsSettingRequest request);
}
