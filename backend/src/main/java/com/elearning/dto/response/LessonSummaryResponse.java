package com.elearning.dto.response;

import com.elearning.entity.Lesson;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Summary Response DTO for public lesson exploration (GET /api/v1/lessons).
 * Encapsulates public lesson metadata without leaking internal creator or moderation info.
 * Conforms to .agents/API.md section 2.3 and zero-entity-leak rules.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonSummaryResponse {

    private Long lessonId;
    private String title;
    private String status;
    private Integer vocabularyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LessonSummaryResponse() {
    }

    public LessonSummaryResponse(Long lessonId, String title, String status, Integer vocabularyCount,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.lessonId = lessonId;
        this.title = title;
        this.status = status;
        this.vocabularyCount = vocabularyCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Overload supporting Long counts for JPQL constructor projections.
     */
    public LessonSummaryResponse(Long lessonId, String title, String status, Long vocabularyCount,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(lessonId, title, status, vocabularyCount != null ? vocabularyCount.intValue() : 0, createdAt, updatedAt);
    }

    public static LessonSummaryResponse fromEntity(Lesson lesson) {
        if (lesson == null) {
            return null;
        }
        int count = (lesson.getLessonVocabularies() != null) ? lesson.getLessonVocabularies().size() : 0;
        return new LessonSummaryResponse(
                lesson.getLessonId(),
                lesson.getTitle(),
                lesson.getStatus(),
                count,
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }

    public static LessonSummaryResponse fromEntity(Lesson lesson, int vocabularyCount) {
        if (lesson == null) {
            return null;
        }
        return new LessonSummaryResponse(
                lesson.getLessonId(),
                lesson.getTitle(),
                lesson.getStatus(),
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
