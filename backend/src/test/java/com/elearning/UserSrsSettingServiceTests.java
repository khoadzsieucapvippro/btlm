package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.UpdateSrsSettingRequest;
import com.elearning.dto.response.UserSrsSettingResponse;
import com.elearning.entity.Account;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.exception.BusinessException;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
import com.elearning.service.impl.UserSrsSettingServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 8B.1: UserSrsSettingService Unit Tests (Mockito)")
class UserSrsSettingServiceTests {

    @Mock
    private UserSrsSettingRepository userSrsSettingRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserSrsSettingServiceImpl userSrsSettingService;

    private UserProfile userA;

    @BeforeEach
    void setUp() {
        Account acc = new Account();
        acc.setEmailOrPhone("usera@test.com");
        acc.setPasswordHash("hash");
        acc.setStatus("Active");

        userA = new UserProfile();
        userA.setUserId(10L);
        userA.setAccount(acc);
        userA.setFullName("User A");

        authenticateAs("usera@test.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_LEARNER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("1. Get Current User Settings Tests")
    class GetSettingsTests {

        @Test
        @DisplayName("Returns existing settings when user already has configured parameters")
        void testGetSettings_existingSettings() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));
            UserSrsSetting existing = new UserSrsSetting(userA, 30, 120);
            existing.setSettingId(1L);
            when(userSrsSettingRepository.findByUser(userA)).thenReturn(Optional.of(existing));

            UserSrsSettingResponse response = userSrsSettingService.getCurrentUserSettings();

            assertThat(response.getSettingId()).isEqualTo(1L);
            assertThat(response.getNewCardsPerDay()).isEqualTo(30);
            assertThat(response.getMaxReviewPerDay()).isEqualTo(120);
        }

        @Test
        @DisplayName("Materializes and persists default settings (20/100) when user has no row")
        void testGetSettings_materializesDefault() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));
            when(userSrsSettingRepository.findByUser(userA)).thenReturn(Optional.empty());

            UserSrsSetting created = new UserSrsSetting(userA, 20, 100);
            created.setSettingId(2L);
            when(userSrsSettingRepository.save(any(UserSrsSetting.class))).thenReturn(created);

            UserSrsSettingResponse response = userSrsSettingService.getCurrentUserSettings();

            assertThat(response.getSettingId()).isEqualTo(2L);
            assertThat(response.getNewCardsPerDay()).isEqualTo(20);
            assertThat(response.getMaxReviewPerDay()).isEqualTo(100);
            verify(userSrsSettingRepository).save(any(UserSrsSetting.class));
        }

        @Test
        @DisplayName("Unauthenticated caller throws UNAUTHORIZED")
        void testGetSettings_unauthenticated_throwsUnauthorized() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> userSrsSettingService.getCurrentUserSettings())
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        }
    }

    @Nested
    @DisplayName("2. Update Current User Settings Tests")
    class UpdateSettingsTests {

        @Test
        @DisplayName("Updates settings successfully with positive integer values")
        void testUpdateSettings_success() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));
            UserSrsSetting existing = new UserSrsSetting(userA, 20, 100);
            existing.setSettingId(5L);
            when(userSrsSettingRepository.findByUser(userA)).thenReturn(Optional.of(existing));

            UserSrsSetting updated = new UserSrsSetting(userA, 40, 150);
            updated.setSettingId(5L);
            when(userSrsSettingRepository.save(any(UserSrsSetting.class))).thenReturn(updated);

            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(40, 150);
            UserSrsSettingResponse response = userSrsSettingService.updateCurrentUserSettings(request);

            assertThat(response.getSettingId()).isEqualTo(5L);
            assertThat(response.getNewCardsPerDay()).isEqualTo(40);
            assertThat(response.getMaxReviewPerDay()).isEqualTo(150);
        }

        @Test
        @DisplayName("Non-positive newCardsPerDay (<= 0) throws VALIDATION_ERROR")
        void testUpdateSettings_zeroNewCards_throwsValidationError() {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(0, 100);

            assertThatThrownBy(() -> userSrsSettingService.updateCurrentUserSettings(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }

        @Test
        @DisplayName("Non-positive maxReviewPerDay (<= 0) throws VALIDATION_ERROR")
        void testUpdateSettings_zeroMaxReview_throwsValidationError() {
            UpdateSrsSettingRequest request = new UpdateSrsSettingRequest(20, 0);

            assertThatThrownBy(() -> userSrsSettingService.updateCurrentUserSettings(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }

        @Test
        @DisplayName("Null request throws VALIDATION_ERROR")
        void testUpdateSettings_nullRequest_throwsValidationError() {
            assertThatThrownBy(() -> userSrsSettingService.updateCurrentUserSettings(null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }
    }
}
