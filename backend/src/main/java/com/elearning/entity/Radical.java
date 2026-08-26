package com.elearning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity mapping table RADICAL (214 Kangxi radicals).
 */
@Entity
@Table(name = "radical", uniqueConstraints = {
        @UniqueConstraint(name = "uk_radical_character", columnNames = {"character"})
})
public class Radical {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "radical_id")
    private Integer radicalId;

    @Column(name = "`character`", nullable = false, length = 10, unique = true)
    private String character;

    @Column(name = "pinyin", nullable = false, length = 50)
    private String pinyin;

    @Column(name = "meaning_han_viet", nullable = false, length = 100)
    private String meaningHanViet;

    @Column(name = "meaning_vi", nullable = false, length = 255)
    private String meaningVi;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "video_writing_url", length = 500)
    private String videoWritingUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "radicals", fetch = FetchType.LAZY)
    private Set<Vocabulary> vocabularies = new HashSet<>();

    public Radical() {
    }

    public Radical(Integer radicalId, String character, String pinyin, String meaningHanViet, String meaningVi) {
        this.radicalId = radicalId;
        this.character = character;
        this.pinyin = pinyin;
        this.meaningHanViet = meaningHanViet;
        this.meaningVi = meaningVi;
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

    public Integer getRadicalId() {
        return radicalId;
    }

    public void setRadicalId(Integer radicalId) {
        this.radicalId = radicalId;
    }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getMeaningHanViet() {
        return meaningHanViet;
    }

    public void setMeaningHanViet(String meaningHanViet) {
        this.meaningHanViet = meaningHanViet;
    }

    public String getMeaningVi() {
        return meaningVi;
    }

    public void setMeaningVi(String meaningVi) {
        this.meaningVi = meaningVi;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getVideoWritingUrl() {
        return videoWritingUrl;
    }

    public void setVideoWritingUrl(String videoWritingUrl) {
        this.videoWritingUrl = videoWritingUrl;
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

    public Set<Vocabulary> getVocabularies() {
        return vocabularies;
    }

    public void setVocabularies(Set<Vocabulary> vocabularies) {
        this.vocabularies = vocabularies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Radical other)) return false;
        return radicalId != null && radicalId.equals(other.radicalId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
