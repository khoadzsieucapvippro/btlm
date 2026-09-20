package com.elearning.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing lesson's metadata (PUT /api/v1/creator/lessons/{id}).
 * Only allowed for lessons in 'Draft' or 'Rejected' state.
 */
public class UpdateLessonRequest {

    @Size(max = 200, message = "Tiêu đề bài học không được vượt quá 200 ký tự")
    private String title;

    @Size(max = 500, message = "Đường dẫn file excel không được vượt quá 500 ký tự")
    private String excelFileUrl;

    public UpdateLessonRequest() {
    }

    public UpdateLessonRequest(String title) {
        this.title = title;
    }

    public UpdateLessonRequest(String title, String excelFileUrl) {
        this.title = title;
        this.excelFileUrl = excelFileUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExcelFileUrl() {
        return excelFileUrl;
    }

    public void setExcelFileUrl(String excelFileUrl) {
        this.excelFileUrl = excelFileUrl;
    }
}
