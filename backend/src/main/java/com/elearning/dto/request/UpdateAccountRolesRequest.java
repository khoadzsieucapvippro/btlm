package com.elearning.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request payload for updating account roles (PUT /api/v1/admin/accounts/{id}/roles).
 * Replaces the entire role set for the target account.
 */
public class UpdateAccountRolesRequest {

    @NotEmpty(message = "Danh sách vai trò không được để trống")
    private List<String> roles;

    public UpdateAccountRolesRequest() {
    }

    public UpdateAccountRolesRequest(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
