package com.elearning.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for approving a lesson (POST /api/v1/moderator/lessons/{id}/approve).
 * Supports optional moderator notes/comments up to 500 characters.
 * Moderator identity is securely resolved on the server side from the authenticated SecurityContext.
 */
public class ApproveLessonRequest {

    @Size(max = 500, message = "Ghi chú phê duyệt không được vượt quá 500 ký tự")
    private String note;

    public ApproveLessonRequest() {
    }

    public ApproveLessonRequest(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
