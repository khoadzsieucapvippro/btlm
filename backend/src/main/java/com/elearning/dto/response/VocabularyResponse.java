package com.elearning.dto.response;

import com.elearning.entity.Vocabulary;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Summary Response DTO for Vocabulary catalog listing and search results.
 * Conforms to .agents/API.md section 2.2 and zero-entity-leak rules.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VocabularyResponse {

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

    public VocabularyResponse() {
    }

    public VocabularyResponse(Long vocabId, String hanzi, String pinyin, String pinyinRaw,
                              String meaningHanViet, String meaningVi, String audioUrl,
                              String videoWritingUrl, String exampleSentence, String exampleTranslation) {
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
    }

    public VocabularyResponse(Long vocabId, String hanzi, String pinyin, String pinyinRaw,
                              String meaningHanViet, String meaningVi) {
        this(vocabId, hanzi, pinyin, pinyinRaw, meaningHanViet, meaningVi, null, null, null, null);
    }

    public static VocabularyResponse fromEntity(Vocabulary vocabulary) {
        if (vocabulary == null) {
            return null;
        }
        return new VocabularyResponse(
                vocabulary.getVocabId(),
                vocabulary.getHanzi(),
                vocabulary.getPinyin(),
                vocabulary.getPinyinRaw(),
                vocabulary.getMeaningHanViet(),
                vocabulary.getMeaningVi(),
                vocabulary.getAudioUrl(),
                vocabulary.getVideoWritingUrl(),
                vocabulary.getExampleSentence(),
                vocabulary.getExampleTranslation()
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
}
