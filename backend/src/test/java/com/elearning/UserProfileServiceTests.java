package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.UpdateProfileRequest;
import com.elearning.dto.response.UserProfileResponse;
import com.elearning.entity.Account;
import com.elearning.entity.UserProfile;
import com.elearning.exception.BusinessException;
import com.elearning.repository.UserProfileRepository;
import com.elearning.service.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService Unit Tests")
class UserProfileServiceTests {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private Account testAccount;
    private UserProfile testProfile;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        try {
            var field = Account.class.getDeclaredField("accountId");
            field.setAccessible(true);
            field.set(testAccount, 100L);
        } catch (Exception ignored) {}
        testAccount.setEmailOrPhone("user@elearning.com");
        testAccount.setStatus("Active");

        testProfile = new UserProfile();
        try {
            var field = UserProfile.class.getDeclaredField("userId");
            field.setAccessible(true);
            field.set(testProfile, 10L);
        } catch (Exception ignored) {}
        testProfile.setAccount(testAccount);
        testProfile.setFullName("Nguyen Van A");
        testProfile.setAvatarUrl("https://example.com/avatar.jpg");
        try {
            var fieldCreated = UserProfile.class.getDeclaredField("createdAt");
            fieldCreated.setAccessible(true);
            fieldCreated.set(testProfile, LocalDateTime.now());
            var fieldUpdated = UserProfile.class.getDeclaredField("updatedAt");
            fieldUpdated.setAccessible(true);
            fieldUpdated.set(testProfile, LocalDateTime.now());
        } catch (Exception ignored) {}
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GIVEN authenticated user WHEN getCurrentUserProfile THEN returns profile matching current user")
    void testGetCurrentUserProfileSuccess() {
        authenticateAs("user@elearning.com");
        when(userProfileRepository.findByAccountEmailOrPhone("user@elearning.com"))
                .thenReturn(Optional.of(testProfile));

        UserProfileResponse response = userProfileService.getCurrentUserProfile();

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getAccountId()).isEqualTo(100L);
        assertThat(response.getEmailOrPhone()).isEqualTo("user@elearning.com");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
    }

    @Test
    @DisplayName("GIVEN unauthenticated request WHEN getCurrentUserProfile THEN throws BusinessException UNAUTHORIZED")
    void testGetCurrentUserProfileUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userProfileService.getCurrentUserProfile())
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("GIVEN non-existent profile WHEN getCurrentUserProfile THEN throws BusinessException NOT_FOUND")
    void testGetCurrentUserProfileNotFound() {
        authenticateAs("missing@elearning.com");
        when(userProfileRepository.findByAccountEmailOrPhone("missing@elearning.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getCurrentUserProfile())
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    @DisplayName("GIVEN valid update request WHEN updateCurrentUserProfile THEN updates and persists only allowed fields")
    void testUpdateCurrentUserProfileSuccess() {
        authenticateAs("user@elearning.com");
        when(userProfileRepository.findByAccountEmailOrPhone("user@elearning.com"))
                .thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest updateRequest = new UpdateProfileRequest("Nguyen Van A Updated", "https://example.com/new.png");
        UserProfileResponse response = userProfileService.updateCurrentUserProfile(updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A Updated");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/new.png");
        assertThat(testProfile.getFullName()).isEqualTo("Nguyen Van A Updated");
        assertThat(testProfile.getAvatarUrl()).isEqualTo("https://example.com/new.png");
        verify(userProfileRepository).save(testProfile);
    }

    @Test
    @DisplayName("GIVEN unauthenticated request WHEN updateCurrentUserProfile THEN throws BusinessException UNAUTHORIZED")
    void testUpdateCurrentUserProfileUnauthenticated() {
        SecurityContextHolder.clearContext();
        UpdateProfileRequest updateRequest = new UpdateProfileRequest("New Name", null);

        assertThatThrownBy(() -> userProfileService.updateCurrentUserProfile(updateRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}
