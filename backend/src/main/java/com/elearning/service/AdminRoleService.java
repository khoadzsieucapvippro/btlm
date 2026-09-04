package com.elearning.service;

import com.elearning.dto.request.UpdateAccountRolesRequest;
import com.elearning.dto.response.AccountResponse;
import com.elearning.dto.response.RoleResponse;

import java.util.List;

/**
 * Service interface for administrative role management and assignment (Module 8D.2).
 */
public interface AdminRoleService {

    /**
     * Retrieves all available predefined system roles.
     *
     * @return list of RoleResponse
     */
    List<RoleResponse> getRoles();

    /**
     * Deterministically updates/replaces the assigned role set for a target account.
     * Concurrency-safe: acquires a row lock on target account to prevent lost updates on authorization_version.
     *
     * @param accountId         target account ID
     * @param request           role replacement payload
     * @param currentAdminEmail identifier of currently authenticated admin
     * @return updated AccountResponse
     */
    AccountResponse updateAccountRoles(Long accountId, UpdateAccountRolesRequest request, String currentAdminEmail);
}
