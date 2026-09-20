package com.elearning.dto.response;

import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Detailed Response DTO for public lesson exploration (GET /api/v1/lessons/{id}).
 * Contains public lesson metadata and an ordered list of vocabulary items.
 * Conforms to .agents/API.md section 2.3 and zero-entity-leak rules.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonDetailResponse {

    private Long lessonId;
    private String title;
    private String status;
    private Integer vocabularyCount;
    private List<LessonVocabItemResponse> vocabularies = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LessonDetailResponse() {
        this.vocabularies = new ArrayList<>();
    }

    public LessonDetailResponse(Long lessonId, String title, String status, Integer vocabularyCount,
                                List<LessonVocabItemResponse> vocabularies,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.lessonId = lessonId;
        this.title = title;
        this.status = status;
        this.vocabularyCount = vocabularyCount;
        this.vocabularies = (vocabularies != null) ? vocabularies : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static LessonDetailResponse fromEntity(Lesson lesson, List<LessonVocabulary> lessonVocabularies) {
        if (lesson == null) {
            return null;
        }
        List<LessonVocabItemResponse> vocabList = new ArrayList<>();
        if (lessonVocabularies != null) {
            for (LessonVocabulary lv : lessonVocabularies) {
                LessonVocabItemResponse item = LessonVocabItemResponse.fromEntity(lv);
                if (item != null) {
                    vocabList.add(item);
                }
            }
        }
        // Enforce deterministic ordering strictly by orderIndex ascending
        vocabList.sort(Comparator.comparing(LessonVocabItemResponse::getOrderIndex));

        return new LessonDetailResponse(
                lesson.getLessonId(),
                lesson.getTitle(),
                lesson.getStatus(),
                vocabList.size(),
                vocabList,
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

    public List<LessonVocabItemResponse> getVocabularies() {
        return vocabularies;
    }

    public void setVocabularies(List<LessonVocabItemResponse> vocabularies) {
        this.vocabularies = (vocabularies != null) ? vocabularies : new ArrayList<>();
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
