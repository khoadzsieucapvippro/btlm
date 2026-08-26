package com.elearning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity mapping table VOCABULARY.
 * Preserves independent pinyin (with tone marks) and pinyin_raw (toneless).
 */
@Entity
@Table(name = "vocabulary", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vocab_hanzi_pinyin_raw", columnNames = {"hanzi", "pinyin_raw"})
})
public class Vocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vocab_id")
    private Long vocabId;

    @Column(name = "hanzi", nullable = false, length = 50)
    private String hanzi;

    @Column(name = "pinyin", nullable = false, length = 100)
    private String pinyin;

    @Column(name = "pinyin_raw", nullable = false, length = 100)
    private String pinyinRaw;

    @Column(name = "meaning_han_viet", nullable = false, length = 100)
    private String meaningHanViet;

    @Column(name = "meaning_vi", nullable = false, length = 255)
    private String meaningVi;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "video_writing_url", length = 500)
    private String videoWritingUrl;

    @Column(name = "example_sentence", length = 500)
    private String exampleSentence;

    @Column(name = "example_translation", length = 500)
    private String exampleTranslation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "vocab_radical",
            joinColumns = @JoinColumn(name = "vocab_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "radical_id", nullable = false)
    )
    private Set<Radical> radicals = new HashSet<>();

    public Vocabulary() {
    }

    public Vocabulary(String hanzi, String pinyin, String pinyinRaw, String meaningHanViet, String meaningVi) {
        this.hanzi = hanzi;
        this.pinyin = pinyin;
        this.pinyinRaw = pinyinRaw;
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

    public void addRadical(Radical radical) {
        if (radical != null) {
            this.radicals.add(radical);
            radical.getVocabularies().add(this);
        }
    }

    public void removeRadical(Radical radical) {
        if (radical != null) {
            this.radicals.remove(radical);
            radical.getVocabularies().remove(this);
        }
    }

    public Long getVocabId() {
        return vocabId;
    }

    public void setVocabId(Long vocabId) {
        this.vocabId = vocabId;
    }

    public String getHanzi() {
        return hanzi;
    }

    public void setHanzi(String hanzi) {
        this.hanzi = hanzi;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getPinyinRaw() {
        return pinyinRaw;
    }

    public void setPinyinRaw(String pinyinRaw) {
        this.pinyinRaw = pinyinRaw;
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

    public String getExampleSentence() {
        return exampleSentence;
    }

    public void setExampleSentence(String exampleSentence) {
        this.exampleSentence = exampleSentence;
    }

    public String getExampleTranslation() {
        return exampleTranslation;
    }

    public void setExampleTranslation(String exampleTranslation) {
        this.exampleTranslation = exampleTranslation;
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

    public Set<Radical> getRadicals() {
        return radicals;
    }

    public void setRadicals(Set<Radical> radicals) {
        this.radicals = radicals;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vocabulary other)) return false;
        return vocabId != null && vocabId.equals(other.vocabId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
