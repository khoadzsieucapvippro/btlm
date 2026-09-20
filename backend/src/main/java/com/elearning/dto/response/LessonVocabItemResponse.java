package com.elearning.dto.response;

import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Vocabulary;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing a single Vocabulary item within a Lesson (GET /api/v1/lessons/{id}).
 * Carries display order (orderIndex) and vocabulary material for learning.
 * Conforms to .agents/API.md section 2.3 and zero-entity-leak rules.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonVocabItemResponse {

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
    private Integer orderIndex;

    public LessonVocabItemResponse() {
    }

    public LessonVocabItemResponse(Long vocabId, String hanzi, String pinyin, String pinyinRaw,
                                  String meaningHanViet, String meaningVi, String audioUrl,
                                  String videoWritingUrl, String exampleSentence,
                                  String exampleTranslation, Integer orderIndex) {
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
        this.orderIndex = orderIndex;
    }

    public static LessonVocabItemResponse fromEntity(LessonVocabulary lessonVocabulary) {
        if (lessonVocabulary == null || lessonVocabulary.getVocabulary() == null) {
            return null;
        }
        Vocabulary v = lessonVocabulary.getVocabulary();
        return new LessonVocabItemResponse(
                v.getVocabId(),
                v.getHanzi(),
                v.getPinyin(),
                v.getPinyinRaw(),
                v.getMeaningHanViet(),
                v.getMeaningVi(),
                v.getAudioUrl(),
                v.getVideoWritingUrl(),
                v.getExampleSentence(),
                v.getExampleTranslation(),
                lessonVocabulary.getOrderIndex()
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

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
