package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.UpdateAccountStatusRequest;
import com.elearning.dto.response.AccountResponse;
import com.elearning.dto.response.PageResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.service.impl.AdminAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 8D.1: AdminAccountService Unit Tests")
class AdminAccountServiceTests {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AdminAccountServiceImpl adminAccountService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setAccountId(10L);
        testAccount.setEmailOrPhone("user10@example.com");
        testAccount.setStatus("Active");

        UserProfile profile = new UserProfile();
        profile.setFullName("Nguyễn Văn A");
        testAccount.setUserProfile(profile);

        Role role = new Role(1, "Learner");
        testAccount.addRole(role);
    }

    @Nested
    @DisplayName("getAccounts Tests")
    class GetAccountsTests {

        @Test
        @DisplayName("GIVEN valid filters WHEN calling getAccounts THEN returns paginated accounts")
        void testGetAccounts_success() {
            Page<Account> page = new PageImpl<>(List.of(testAccount), PageRequest.of(0, 10), 1);
            when(accountRepository.findAllWithFilters(eq("Active"), eq("user10"), any(Pageable.class))).thenReturn(page);

            PageResponse<AccountResponse> response = adminAccountService.getAccounts("Active", "user10", PageRequest.of(0, 10));

            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getEmailOrPhone()).isEqualTo("user10@example.com");
            assertThat(response.getItems().get(0).getFullName()).isEqualTo("Nguyễn Văn A");
            assertThat(response.getItems().get(0).getStatus()).isEqualTo("Active");
        }
    }

    @Nested
    @DisplayName("updateAccountStatus Tests")
    class UpdateAccountStatusTests {

        @Test
        @DisplayName("GIVEN valid status update WHEN updating another account THEN status is updated successfully and auth version is incremented")
        void testUpdateAccountStatus_success() {
            testAccount.setAuthorizationVersion(1L);
            when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UpdateAccountStatusRequest request = new UpdateAccountStatusRequest("Banned");
            AccountResponse response = adminAccountService.updateAccountStatus(10L, request, "admin@example.com");

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("Banned");
            assertThat(testAccount.getAuthorizationVersion()).isEqualTo(2L);
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("GIVEN same status WHEN updating status THEN no save or auth version increment occurs (idempotent)")
        void testUpdateAccountStatus_sameStatus_idempotent() {
            testAccount.setAuthorizationVersion(1L);
            when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testAccount));

            UpdateAccountStatusRequest request = new UpdateAccountStatusRequest("Active");
            AccountResponse response = adminAccountService.updateAccountStatus(10L, request, "admin@example.com");

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("Active");
            assertThat(testAccount.getAuthorizationVersion()).isEqualTo(1L);
            verify(accountRepository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("GIVEN self-deactivation attempt by current admin WHEN updating status THEN throws BAD_REQUEST (POL-8D-01)")
        void testSelfDeactivation_throwsBadRequest() {
            when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testAccount));

            UpdateAccountStatusRequest request = new UpdateAccountStatusRequest("Inactive");

            assertThatThrownBy(() -> adminAccountService.updateAccountStatus(10L, request, "user10@example.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                    .hasMessage("Không thể tự vô hiệu hóa hoặc khóa tài khoản đang đăng nhập");
        }

        @Test
        @DisplayName("GIVEN invalid status string WHEN updating THEN throws VALIDATION_ERROR")
        void testInvalidStatus_throwsValidationError() {
            UpdateAccountStatusRequest request = new UpdateAccountStatusRequest("UnknownStatus");

            assertThatThrownBy(() -> adminAccountService.updateAccountStatus(10L, request, "admin@example.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
        }

        @Test
        @DisplayName("GIVEN non-existent account ID WHEN updating THEN throws NOT_FOUND")
        void testNonExistentAccount_throwsNotFound() {
            when(accountRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            UpdateAccountStatusRequest request = new UpdateAccountStatusRequest("Active");

            assertThatThrownBy(() -> adminAccountService.updateAccountStatus(999L, request, "admin@example.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
        }
    }
}
