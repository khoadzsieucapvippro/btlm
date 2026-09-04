package com.elearning.service.impl;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.UpdateAccountStatusRequest;
import com.elearning.dto.response.AccountResponse;
import com.elearning.dto.response.PageResponse;
import com.elearning.entity.Account;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.service.AdminAccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Implementation of {@link AdminAccountService} for administrative account management (Task 8D.1).
 */
@Service
public class AdminAccountServiceImpl implements AdminAccountService {

    private static final Set<String> VALID_STATUSES = Set.of("Active", "Inactive", "Banned");

    private final AccountRepository accountRepository;

    public AdminAccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> getAccounts(String status, String search, Pageable pageable) {
        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            normalizedStatus = normalizeStatus(status);
        }

        String searchPattern = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Account> accountPage = accountRepository.findAllWithFilters(normalizedStatus, searchPattern, pageable);
        return PageResponse.from(accountPage.map(AccountResponse::fromEntity));
    }

    @Override
    @Transactional
    public AccountResponse updateAccountStatus(Long accountId, UpdateAccountStatusRequest request, String currentAdminEmail) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trạng thái tài khoản không được để trống");
        }

        String normalizedStatus = normalizeStatus(request.getStatus());

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy tài khoản với ID: " + accountId));

        // Self-deactivation protection (POL-8D-01)
        if (currentAdminEmail != null && currentAdminEmail.equalsIgnoreCase(account.getEmailOrPhone())
                && !normalizedStatus.equalsIgnoreCase("Active")) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Không thể tự vô hiệu hóa hoặc khóa tài khoản đang đăng nhập"
            );
        }

        if (!account.getStatus().equalsIgnoreCase(normalizedStatus)) {
            account.setStatus(normalizedStatus);
            account.incrementAuthorizationVersion();
            account = accountRepository.save(account);
        }

        return AccountResponse.fromEntity(account);
    }

    private String normalizeStatus(String rawStatus) {
        for (String validStatus : VALID_STATUSES) {
            if (validStatus.equalsIgnoreCase(rawStatus.trim())) {
                return validStatus;
            }
        }
        throw new BusinessException(
                ErrorCode.VALIDATION_ERROR,
                "Trạng thái tài khoản phải là Active, Inactive hoặc Banned"
        );
    }
}
