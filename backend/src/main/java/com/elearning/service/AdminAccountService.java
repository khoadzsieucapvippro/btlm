package com.elearning.service;

import com.elearning.dto.request.UpdateAccountStatusRequest;
import com.elearning.dto.response.AccountResponse;
import com.elearning.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for administrative account management operations (Module 8D.1).
 */
public interface AdminAccountService {

    /**
     * Retrieves paginated accounts with optional status and search filters.
     *
     * @param status   optional status filter (Active, Inactive, Banned)
     * @param search   optional search query (email/phone or full name)
     * @param pageable pagination parameters
     * @return paginated AccountResponse
     */
    PageResponse<AccountResponse> getAccounts(String status, String search, Pageable pageable);

    /**
     * Updates an account's status with self-deactivation protection.
     *
     * @param accountId         target account ID
     * @param request           status update payload
     * @param currentAdminEmail identifier of currently authenticated admin
     * @return updated AccountResponse
     */
    AccountResponse updateAccountStatus(Long accountId, UpdateAccountStatusRequest request, String currentAdminEmail);
}
