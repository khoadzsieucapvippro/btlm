package com.elearning.controller;

import com.elearning.dto.request.UpdateAccountRolesRequest;
import com.elearning.dto.response.AccountResponse;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.RoleResponse;
import com.elearning.service.AdminRoleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * REST Controller for administrative role exploration and role assignments (Module 8D.2).
 * Base paths:
 * - GET /api/v1/admin/roles
 * - PUT /api/v1/admin/accounts/{id}/roles
 * Protected: Requires ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('Admin', 'ADMIN')")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    public AdminRoleController(AdminRoleService adminRoleService) {
        this.adminRoleService = adminRoleService;
    }

    /**
     * Retrieves all available system roles.
     *
     * @return ApiResponse wrapping list of RoleResponse
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles() {
        List<RoleResponse> response = adminRoleService.getRoles();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Replaces assigned roles for a target account.
     *
     * @param id        target account ID
     * @param request   role assignment payload
     * @param principal authenticated user principal
     * @return ApiResponse wrapping updated AccountResponse
     */
    @PutMapping("/accounts/{id}/roles")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountRoles(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAccountRolesRequest request,
            Principal principal) {
        String currentAdminEmail = (principal != null) ? principal.getName() : null;
        AccountResponse response = adminRoleService.updateAccountRoles(id, request, currentAdminEmail);
        return ResponseEntity.ok(ApiResponse.success("Phân quyền tài khoản thành công", response));
    }
}
