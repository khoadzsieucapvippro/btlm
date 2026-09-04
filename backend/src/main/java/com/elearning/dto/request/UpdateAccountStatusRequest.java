package com.elearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for updating account status (PUT /api/v1/admin/accounts/{id}/status).
 * Valid statuses: Active, Inactive, Banned.
 */
public class UpdateAccountStatusRequest {

    @NotBlank(message = "Trạng thái tài khoản không được để trống")
    @Pattern(regexp = "^(?i)(Active|Inactive|Banned)$", message = "Trạng thái tài khoản phải là Active, Inactive hoặc Banned")
    private String status;

    public UpdateAccountStatusRequest() {
    }

    public UpdateAccountStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
