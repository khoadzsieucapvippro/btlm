package com.elearning.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parsed and validated vocabulary row from an imported Excel sheet.
 */
public class ParsedVocabularyItem {

    private Integer rowNumber;
    private String hanzi;
    private String pinyin;
    private String pinyinRaw;
    private String meaningHanViet;
    private String meaningVi;
    private String exampleSentence;
    private String exampleTranslation;
    private Boolean isExisting;
    private Long existingVocabId;
    private Boolean isValid;
    private List<String> errors = new ArrayList<>();

    public ParsedVocabularyItem() {
    }

    public ParsedVocabularyItem(Integer rowNumber, String hanzi, String pinyin, String pinyinRaw,
                                String meaningHanViet, String meaningVi, String exampleSentence,
                                String exampleTranslation, Boolean isExisting, Long existingVocabId,
                                Boolean isValid) {
        this.rowNumber = rowNumber;
        this.hanzi = hanzi;
        this.pinyin = pinyin;
        this.pinyinRaw = pinyinRaw;
        this.meaningHanViet = meaningHanViet;
        this.meaningVi = meaningVi;
        this.exampleSentence = exampleSentence;
        this.exampleTranslation = exampleTranslation;
        this.isExisting = isExisting;
        this.existingVocabId = existingVocabId;
        this.isValid = isValid;
    }

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.isValid = false;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
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

    public Boolean getIsExisting() {
        return isExisting;
    }

    public void setIsExisting(Boolean existing) {
        isExisting = existing;
    }

    public Long getExistingVocabId() {
        return existingVocabId;
    }

    public void setExistingVocabId(Long existingVocabId) {
        this.existingVocabId = existingVocabId;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(Boolean valid) {
        isValid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
