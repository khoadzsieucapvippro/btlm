package com.elearning.dto.response;

import com.elearning.entity.Lesson;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Response DTO for Moderator Queue exploration (GET /api/v1/moderator/lessons/pending).
 * Encapsulates pending lesson metadata, creator identity, and vocabulary count without leaking JPA entities.
 * Conforms to .agents/API.md section 2.5 and zero-entity-leak rules.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModerationQueueResponse {

    private Long lessonId;
    private String title;
    private String status;
    private Long creatorId;
    private String creatorEmail;
    private Integer vocabularyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ModerationQueueResponse() {
    }

    public ModerationQueueResponse(Long lessonId, String title, String status, Long creatorId,
                                   String creatorEmail, Integer vocabularyCount,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.lessonId = lessonId;
        this.title = title;
        this.status = status;
        this.creatorId = creatorId;
        this.creatorEmail = creatorEmail;
        this.vocabularyCount = vocabularyCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Overload supporting Long count for JPQL constructor projections.
     */
    public ModerationQueueResponse(Long lessonId, String title, String status, Long creatorId,
                                   String creatorEmail, Long vocabularyCount,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(lessonId, title, status, creatorId, creatorEmail,
                vocabularyCount != null ? vocabularyCount.intValue() : 0,
                createdAt, updatedAt);
    }

    public static ModerationQueueResponse fromEntity(Lesson lesson) {
        if (lesson == null) {
            return null;
        }
        int count = (lesson.getLessonVocabularies() != null) ? lesson.getLessonVocabularies().size() : 0;
        Long cId = (lesson.getCreatedBy() != null) ? lesson.getCreatedBy().getAccountId() : null;
        String cEmail = (lesson.getCreatedBy() != null) ? lesson.getCreatedBy().getEmailOrPhone() : null;
        return new ModerationQueueResponse(
                lesson.getLessonId(),
                lesson.getTitle(),
                lesson.getStatus(),
                cId,
                cEmail,
                count,
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }

    public static ModerationQueueResponse fromEntity(Lesson lesson, int vocabularyCount) {
        if (lesson == null) {
            return null;
        }
        Long cId = (lesson.getCreatedBy() != null) ? lesson.getCreatedBy().getAccountId() : null;
        String cEmail = (lesson.getCreatedBy() != null) ? lesson.getCreatedBy().getEmailOrPhone() : null;
        return new ModerationQueueResponse(
                lesson.getLessonId(),
                lesson.getTitle(),
                lesson.getStatus(),
                cId,
                cEmail,
                vocabularyCount,
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorEmail() {
        return creatorEmail;
    }

    public void setCreatorEmail(String creatorEmail) {
        this.creatorEmail = creatorEmail;
    }

    public Integer getVocabularyCount() {
        return vocabularyCount;
    }

    public void setVocabularyCount(Integer vocabularyCount) {
        this.vocabularyCount = vocabularyCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
