package com.elearning.service.impl;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.UpdateProfileRequest;
import com.elearning.dto.response.UserProfileResponse;
import com.elearning.entity.UserProfile;
import com.elearning.exception.BusinessException;
import com.elearning.repository.UserProfileRepository;
import com.elearning.service.UserProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link UserProfileService} managing profile operations.
 * Derives user identity strictly from SecurityContextHolder to ensure ownership isolation.
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileServiceImpl(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        String emailOrPhone = getCurrentUserIdentifier();
        UserProfile userProfile = userProfileRepository.findByAccountEmailOrPhone(emailOrPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người dùng"));

        return UserProfileResponse.fromEntity(userProfile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UpdateProfileRequest request) {
        String emailOrPhone = getCurrentUserIdentifier();
        UserProfile userProfile = userProfileRepository.findByAccountEmailOrPhone(emailOrPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người dùng"));

        // Apply only allowed editable profile fields
        userProfile.setFullName(request.getFullName());
        userProfile.setAvatarUrl(request.getAvatarUrl());

        UserProfile updatedProfile = userProfileRepository.save(userProfile);
        return UserProfileResponse.fromEntity(updatedProfile);
    }

    /**
     * Resolves the current authenticated user's identifier (email or phone) from Spring SecurityContext.
     *
     * @return emailOrPhone of authenticated user
     * @throws BusinessException with UNAUTHORIZED if not authenticated
     */
    private String getCurrentUserIdentifier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Chưa xác thực hoặc phiên đăng nhập đã hết hạn");
        }
        return authentication.getName();
    }
}
