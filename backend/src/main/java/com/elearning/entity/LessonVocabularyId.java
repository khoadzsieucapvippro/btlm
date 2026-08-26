package com.elearning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link LessonVocabulary} entity.
 * Maps to physical composite primary key (lesson_id, vocab_id).
 */
@Embeddable
public class LessonVocabularyId implements Serializable {

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "vocab_id")
    private Long vocabId;

    public LessonVocabularyId() {
    }

    public LessonVocabularyId(Long lessonId, Long vocabId) {
        this.lessonId = lessonId;
        this.vocabId = vocabId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public Long getVocabId() {
        return vocabId;
    }

    public void setVocabId(Long vocabId) {
        this.vocabId = vocabId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LessonVocabularyId that)) return false;
        return Objects.equals(lessonId, that.lessonId) && Objects.equals(vocabId, that.vocabId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lessonId, vocabId);
    }
}
