package com.elearning.dto.response;

import com.elearning.entity.Vocabulary;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Detailed Response DTO for a single Vocabulary item (GET /api/v1/vocabulary/{id}).
 * Includes constituent Kangxi radicals as {@link RadicalResponse} and lifecycle audit timestamps.
 * Conforms to .agents/API.md section 2.2 and zero-entity-leak rules.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VocabularyDetailResponse {

    private Long vocabId;
    private String hanzi;
    private String pinyin;
    private String pinyinRaw;
    private String meaningHanViet;
    private String meaningVi;
    private String audioUrl;
    private String videoWritingUrl;
    private String exampleSentence;
    private String exampleTranslation;
    private List<RadicalResponse> radicals = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public VocabularyDetailResponse() {
        this.radicals = new ArrayList<>();
    }

    public VocabularyDetailResponse(Long vocabId, String hanzi, String pinyin, String pinyinRaw,
                                    String meaningHanViet, String meaningVi, String audioUrl,
                                    String videoWritingUrl, String exampleSentence, String exampleTranslation,
                                    List<RadicalResponse> radicals,
                                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.vocabId = vocabId;
        this.hanzi = hanzi;
        this.pinyin = pinyin;
        this.pinyinRaw = pinyinRaw;
        this.meaningHanViet = meaningHanViet;
        this.meaningVi = meaningVi;
        this.audioUrl = audioUrl;
        this.videoWritingUrl = videoWritingUrl;
        this.exampleSentence = exampleSentence;
        this.exampleTranslation = exampleTranslation;
        this.radicals = (radicals != null) ? radicals : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public VocabularyDetailResponse(Long vocabId, String hanzi, String pinyin, String pinyinRaw,
                                    String meaningHanViet, String meaningVi) {
        this(vocabId, hanzi, pinyin, pinyinRaw, meaningHanViet, meaningVi, null, null, null, null, new ArrayList<>(), null, null);
    }

    public static VocabularyDetailResponse fromEntity(Vocabulary vocabulary) {
        if (vocabulary == null) {
            return null;
        }

        List<RadicalResponse> radicalResponses = Collections.emptyList();
        if (vocabulary.getRadicals() != null && !vocabulary.getRadicals().isEmpty()) {
            radicalResponses = vocabulary.getRadicals().stream()
                    .map(RadicalResponse::fromEntity)
                    .sorted(Comparator.comparing(RadicalResponse::getRadicalId))
                    .toList();
        }

        return new VocabularyDetailResponse(
                vocabulary.getVocabId(),
                vocabulary.getHanzi(),
                vocabulary.getPinyin(),
                vocabulary.getPinyinRaw(),
                vocabulary.getMeaningHanViet(),
                vocabulary.getMeaningVi(),
                vocabulary.getAudioUrl(),
                vocabulary.getVideoWritingUrl(),
                vocabulary.getExampleSentence(),
                vocabulary.getExampleTranslation(),
                radicalResponses,
                vocabulary.getCreatedAt(),
                vocabulary.getUpdatedAt()
        );
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

    public List<RadicalResponse> getRadicals() {
        return radicals;
    }

    public void setRadicals(List<RadicalResponse> radicals) {
        this.radicals = radicals;
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
