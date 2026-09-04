package com.elearning.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a personal study note on a vocabulary item.
 * Content length is restricted to maximum 500 characters.
 */
public class PersonalNoteRequest {

    @NotNull(message = "Nội dung ghi chú không được để null")
    @Size(max = 500, message = "Nội dung ghi chú tối đa 500 ký tự")
    private String content;

    public PersonalNoteRequest() {
    }

    public PersonalNoteRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
