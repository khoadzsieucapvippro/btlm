package com.elearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for creating a new Lesson (POST /api/v1/creator/lessons).
 * Server controls lessonId, createdBy (from security principal), and initial status ('Draft').
 */
public class CreateLessonRequest {

    @NotBlank(message = "Tiêu đề bài học không được để trống")
    @Size(max = 200, message = "Tiêu đề bài học không được vượt quá 200 ký tự")
    private String title;

    @Size(max = 500, message = "Đường dẫn file excel không được vượt quá 500 ký tự")
    private String excelFileUrl;

    private List<@NotNull(message = "ID từ vựng không được để trống") @Positive(message = "ID từ vựng phải là số nguyên dương") Long> vocabularyIds = new ArrayList<>();

    public CreateLessonRequest() {
    }

    public CreateLessonRequest(String title) {
        this.title = title;
    }

    public CreateLessonRequest(String title, List<Long> vocabularyIds) {
        this.title = title;
        this.vocabularyIds = (vocabularyIds != null) ? vocabularyIds : new ArrayList<>();
    }

    public CreateLessonRequest(String title, String excelFileUrl, List<Long> vocabularyIds) {
        this.title = title;
        this.excelFileUrl = excelFileUrl;
        this.vocabularyIds = (vocabularyIds != null) ? vocabularyIds : new ArrayList<>();
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

    public List<Long> getVocabularyIds() {
        return vocabularyIds;
    }

    public void setVocabularyIds(List<Long> vocabularyIds) {
        this.vocabularyIds = (vocabularyIds != null) ? vocabularyIds : new ArrayList<>();
    }
}
