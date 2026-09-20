package com.elearning.dto.response;

import com.elearning.entity.CardProgress;
import com.elearning.entity.Radical;
import com.elearning.entity.Vocabulary;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Polymorphic response DTO representing a flashcard due for review in the SRS session.
 * Contains common card metadata as well as vocabulary or radical-specific fields for UI rendering.
 */
public class DueCardResponse {

    private String itemType;
    private Long itemId;
    private String hanzi;
    private String pinyin;
    private String meaningVi;
    private String meaningHanViet;
    private String exampleSentence;
    private String exampleTranslation;
    private Integer strokeCount;
    private Integer repetitions;
    private BigDecimal easeFactor;
    private Integer intervalDays;
    private LocalDateTime nextReviewAt;
    private Boolean isNew;

    public DueCardResponse() {
    }

    public static DueCardResponse fromVocabulary(Vocabulary vocab, CardProgress progress) {
        DueCardResponse response = new DueCardResponse();
        response.setItemType("VOCABULARY");
        response.setItemId(vocab.getVocabId());
        response.setHanzi(vocab.getHanzi());
        response.setPinyin(vocab.getPinyin());
        response.setMeaningVi(vocab.getMeaningVi());
        response.setMeaningHanViet(vocab.getMeaningHanViet());
        response.setExampleSentence(vocab.getExampleSentence());
        response.setExampleTranslation(vocab.getExampleTranslation());
        response.setStrokeCount(null);

        if (progress != null) {
            response.setRepetitions(progress.getRepetitions());
            response.setEaseFactor(progress.getEaseFactor());
            response.setIntervalDays(progress.getIntervalDays());
            response.setNextReviewAt(progress.getNextReviewAt());
            response.setIsNew(progress.getRepetitions() == 0 && progress.getIntervalDays() == 0 && progress.getNextReviewAt() == null);
        } else {
            response.setRepetitions(0);
            response.setEaseFactor(new BigDecimal("2.50"));
            response.setIntervalDays(0);
            response.setNextReviewAt(null);
            response.setIsNew(true);
        }

        return response;
    }

    public static DueCardResponse fromRadical(Radical radical, CardProgress progress) {
        DueCardResponse response = new DueCardResponse();
        response.setItemType("RADICAL");
        response.setItemId(radical.getRadicalId() != null ? radical.getRadicalId().longValue() : null);
        response.setHanzi(radical.getCharacter());
        response.setPinyin(radical.getPinyin());
        response.setMeaningVi(radical.getMeaningVi());
        response.setMeaningHanViet(radical.getMeaningHanViet());
        response.setExampleSentence(null);
        response.setExampleTranslation(null);
        response.setStrokeCount(null);

        if (progress != null) {
            response.setRepetitions(progress.getRepetitions());
            response.setEaseFactor(progress.getEaseFactor());
            response.setIntervalDays(progress.getIntervalDays());
            response.setNextReviewAt(progress.getNextReviewAt());
            response.setIsNew(progress.getRepetitions() == 0 && progress.getIntervalDays() == 0 && progress.getNextReviewAt() == null);
        } else {
            response.setRepetitions(0);
            response.setEaseFactor(new BigDecimal("2.50"));
            response.setIntervalDays(0);
            response.setNextReviewAt(null);
            response.setIsNew(true);
        }

        return response;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
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

    public String getMeaningVi() {
        return meaningVi;
    }

    public void setMeaningVi(String meaningVi) {
        this.meaningVi = meaningVi;
    }

    public String getMeaningHanViet() {
        return meaningHanViet;
    }

    public void setMeaningHanViet(String meaningHanViet) {
        this.meaningHanViet = meaningHanViet;
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

    public Integer getStrokeCount() {
        return strokeCount;
    }

    public void setStrokeCount(Integer strokeCount) {
        this.strokeCount = strokeCount;
    }

    public Integer getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(Integer repetitions) {
        this.repetitions = repetitions;
    }

    public BigDecimal getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(BigDecimal easeFactor) {
        this.easeFactor = easeFactor;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public LocalDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(LocalDateTime nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }

    public Boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }
}
