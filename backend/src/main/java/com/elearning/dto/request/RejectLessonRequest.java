package com.elearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for rejecting a lesson (POST /api/v1/moderator/lessons/{id}/reject).
 * Strictly requires a non-blank rejection reason (<= 500 chars) and supports optional flagged fields JSON.
 * Conforms to .agents/API.md section 2.5 and MODERATION_LOG schema.
 */
public class RejectLessonRequest {

    @NotBlank(message = "Lý do từ chối không được để trống")
    @Size(max = 500, message = "Lý do từ chối không được vượt quá 500 ký tự")
    private String rejectionReason;

    private String flaggedFields;

    public RejectLessonRequest() {
    }

    public RejectLessonRequest(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public RejectLessonRequest(String rejectionReason, String flaggedFields) {
        this.rejectionReason = rejectionReason;
        this.flaggedFields = flaggedFields;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getFlaggedFields() {
        return flaggedFields;
    }

    public void setFlaggedFields(String flaggedFields) {
        this.flaggedFields = flaggedFields;
    }
}
