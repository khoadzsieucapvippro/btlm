package com.elearning.controller;

import com.elearning.dto.request.UpdateAccountStatusRequest;
import com.elearning.dto.response.AccountResponse;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.PageResponse;
import com.elearning.service.AdminAccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * REST Controller for administrative account lifecycle management (Module 8D.1).
 * Protected: Requires ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/accounts")
@PreAuthorize("hasAnyRole('Admin', 'ADMIN')")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    /**
     * Retrieves paginated accounts with optional status and search filtering.
     *
     * @param status   optional status filter (Active, Inactive, Banned)
     * @param search   optional search query
     * @param pageable pagination parameters
     * @return ApiResponse wrapping PageResponse of AccountResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AccountResponse>>> getAccounts(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search,
            Pageable pageable) {
        PageResponse<AccountResponse> response = adminAccountService.getAccounts(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates an account's lifecycle status.
     *
     * @param id        target account ID
     * @param request   status update payload
     * @param principal authenticated user principal
     * @return ApiResponse wrapping updated AccountResponse
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAccountStatusRequest request,
            Principal principal) {
        String currentAdminEmail = (principal != null) ? principal.getName() : null;
        AccountResponse response = adminAccountService.updateAccountStatus(id, request, currentAdminEmail);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái tài khoản thành công", response));
    }
}
