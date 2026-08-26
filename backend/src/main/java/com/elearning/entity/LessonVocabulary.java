package com.elearning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA Entity mapping table LESSON_VOCABULARY.
 * Represents association between Lesson and Vocabulary with display ordering (order_index).
 */
@Entity
@Table(name = "lesson_vocabulary", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lesson_order_index", columnNames = {"lesson_id", "order_index"})
})
public class LessonVocabulary {

    @EmbeddedId
    private LessonVocabularyId id = new LessonVocabularyId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("lessonId")
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("vocabId")
    @JoinColumn(name = "vocab_id", nullable = false)
    private Vocabulary vocabulary;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    public LessonVocabulary() {
    }

    public LessonVocabulary(Lesson lesson, Vocabulary vocabulary, Integer orderIndex) {
        this.lesson = lesson;
        this.vocabulary = vocabulary;
        this.orderIndex = orderIndex;
        if (lesson != null && lesson.getLessonId() != null && vocabulary != null && vocabulary.getVocabId() != null) {
            this.id = new LessonVocabularyId(lesson.getLessonId(), vocabulary.getVocabId());
        }
    }

    public LessonVocabularyId getId() {
        return id;
    }

    public void setId(LessonVocabularyId id) {
        this.id = id;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LessonVocabulary other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
