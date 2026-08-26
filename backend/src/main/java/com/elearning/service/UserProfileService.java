package com.elearning.service;

import com.elearning.dto.request.UpdateProfileRequest;
import com.elearning.dto.response.UserProfileResponse;

/**
 * Service managing user profile retrieval and updates for the authenticated user.
 */
public interface UserProfileService {

    /**
     * Retrieves the profile of the current authenticated user from SecurityContext.
     *
     * @return UserProfileResponse containing profile details
     */
    UserProfileResponse getCurrentUserProfile();

    /**
     * Updates the profile of the current authenticated user from SecurityContext.
     *
     * @param request profile update payload containing allowed editable fields (fullName, avatarUrl)
     * @return UserProfileResponse containing updated profile details
     */
    UserProfileResponse updateCurrentUserProfile(UpdateProfileRequest request);
}
