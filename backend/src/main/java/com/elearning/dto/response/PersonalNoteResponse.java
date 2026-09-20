package com.elearning.dto.response;

import com.elearning.entity.PersonalNote;

import java.time.LocalDateTime;

/**
 * Response DTO representing a personal study note.
 */
public class PersonalNoteResponse {

    private Long noteId;
    private Long vocabId;
    private String content;
    private LocalDateTime createdAt;

    public PersonalNoteResponse() {
    }

    public PersonalNoteResponse(Long noteId, Long vocabId, String content, LocalDateTime createdAt) {
        this.noteId = noteId;
        this.vocabId = vocabId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static PersonalNoteResponse fromEntity(PersonalNote note) {
        if (note == null) {
            return null;
        }
        PersonalNoteResponse response = new PersonalNoteResponse();
        response.setNoteId(note.getNoteId());
        response.setVocabId(note.getVocabulary() != null ? note.getVocabulary().getVocabId() : null);
        response.setContent(note.getContent());
        response.setCreatedAt(note.getCreatedAt());
        return response;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public Long getVocabId() {
        return vocabId;
    }

    public void setVocabId(Long vocabId) {
        this.vocabId = vocabId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
