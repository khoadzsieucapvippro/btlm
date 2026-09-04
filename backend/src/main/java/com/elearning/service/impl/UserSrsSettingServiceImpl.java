package com.elearning.service.impl;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.UpdateSrsSettingRequest;
import com.elearning.dto.response.UserSrsSettingResponse;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.exception.BusinessException;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
import com.elearning.service.UserSrsSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing personalized Spaced Repetition System (SRS) study parameters.
 */
@Service
public class UserSrsSettingServiceImpl implements UserSrsSettingService {

    private static final Logger log = LoggerFactory.getLogger(UserSrsSettingServiceImpl.class);

    private final UserSrsSettingRepository userSrsSettingRepository;
    private final UserProfileRepository userProfileRepository;

    public UserSrsSettingServiceImpl(UserSrsSettingRepository userSrsSettingRepository,
                                     UserProfileRepository userProfileRepository) {
        this.userSrsSettingRepository = userSrsSettingRepository;
        this.userProfileRepository = userProfileRepository;
    }

    private UserProfile resolveCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Người dùng chưa được xác thực");
        }
        return userProfileRepository.findByAccountEmailOrPhone(auth.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người dùng"));
    }

    @Override
    @Transactional
    public UserSrsSettingResponse getCurrentUserSettings() {
        UserProfile user = resolveCurrentUserProfile();
        UserSrsSetting setting = userSrsSettingRepository.findByUser(user)
                .orElseGet(() -> {
                    log.info("Materializing default SRS settings (20/100) for user_id={}", user.getUserId());
                    return userSrsSettingRepository.save(new UserSrsSetting(user, 20, 100));
                });
        return UserSrsSettingResponse.fromEntity(setting);
    }

    @Override
    @Transactional
    public UserSrsSettingResponse updateCurrentUserSettings(UpdateSrsSettingRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Yêu cầu cấu hình không được để trống");
        }
        if (request.getNewCardsPerDay() == null || request.getNewCardsPerDay() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số thẻ mới mỗi ngày phải là số nguyên dương lớn hơn 0");
        }
        if (request.getMaxReviewPerDay() == null || request.getMaxReviewPerDay() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Số thẻ ôn tập tối đa mỗi ngày phải là số nguyên dương lớn hơn 0");
        }

        UserProfile user = resolveCurrentUserProfile();
        UserSrsSetting setting = userSrsSettingRepository.findByUser(user)
                .orElseGet(() -> new UserSrsSetting(user, 20, 100));

        setting.setNewCardsPerDay(request.getNewCardsPerDay());
        setting.setMaxReviewPerDay(request.getMaxReviewPerDay());

        UserSrsSetting saved = userSrsSettingRepository.save(setting);
        log.info("Updated SRS settings for user_id={}: newCardsPerDay={}, maxReviewPerDay={}",
                user.getUserId(), saved.getNewCardsPerDay(), saved.getMaxReviewPerDay());

        return UserSrsSettingResponse.fromEntity(saved);
    }
}
