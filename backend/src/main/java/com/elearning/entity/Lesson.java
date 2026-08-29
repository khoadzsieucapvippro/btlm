package com.elearning.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity mapping table LESSON.
 * Represents a study lesson created by a creator Account, containing an ordered list of Vocabularies.
 */
@Entity
@Table(name = "lesson")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "excel_file_url", length = 500)
    private String excelFileUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Account createdBy;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "Draft";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<LessonVocabulary> lessonVocabularies = new ArrayList<>();

    public Lesson() {
    }

    public Lesson(String title, Account createdBy) {
        this.title = title;
        this.createdBy = createdBy;
        this.status = "Draft";
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addVocabulary(Vocabulary vocabulary, int orderIndex) {
        LessonVocabulary lv = new LessonVocabulary(this, vocabulary, orderIndex);
        this.lessonVocabularies.add(lv);
    }

    public void removeVocabulary(Vocabulary vocabulary) {
        if (vocabulary != null) {
            this.lessonVocabularies.removeIf(lv -> lv.getVocabulary() != null && vocabulary.equals(lv.getVocabulary()));
        }
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

    public String getExcelFileUrl() {
        return excelFileUrl;
    }

    public void setExcelFileUrl(String excelFileUrl) {
        this.excelFileUrl = excelFileUrl;
    }

    public Account getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Account createdBy) {
        this.createdBy = createdBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<LessonVocabulary> getLessonVocabularies() {
        return lessonVocabularies;
    }

    public void setLessonVocabularies(List<LessonVocabulary> lessonVocabularies) {
        this.lessonVocabularies = lessonVocabularies;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lesson other)) return false;
        return lessonId != null && lessonId.equals(other.lessonId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
