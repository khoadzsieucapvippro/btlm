package com.elearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

/**
 * Request DTO for creating a new Vocabulary record (Admin-only).
 * Enforces schema constraints matching table VOCABULARY.
 */
public class CreateVocabularyRequest {

    @NotBlank(message = "Chữ Hán không được để trống")
    @Size(max = 50, message = "Chữ Hán không được vượt quá 50 ký tự")
    private String hanzi;

    @NotBlank(message = "Pinyin không được để trống")
    @Size(max = 100, message = "Pinyin không được vượt quá 100 ký tự")
    private String pinyin;

    @NotBlank(message = "Pinyin không dấu không được để trống")
    @Size(max = 100, message = "Pinyin không dấu không được vượt quá 100 ký tự")
    private String pinyinRaw;

    @NotBlank(message = "Nghĩa Hán-Việt không được để trống")
    @Size(max = 100, message = "Nghĩa Hán-Việt không được vượt quá 100 ký tự")
    private String meaningHanViet;

    @NotBlank(message = "Nghĩa tiếng Việt không được để trống")
    @Size(max = 255, message = "Nghĩa tiếng Việt không được vượt quá 255 ký tự")
    private String meaningVi;

    @Size(max = 500, message = "URL âm thanh không được vượt quá 500 ký tự")
    private String audioUrl;

    @Size(max = 500, message = "URL video viết chữ không được vượt quá 500 ký tự")
    private String videoWritingUrl;

    @Size(max = 500, message = "Câu ví dụ không được vượt quá 500 ký tự")
    private String exampleSentence;

    @Size(max = 500, message = "Dịch nghĩa câu ví dụ không được vượt quá 500 ký tự")
    private String exampleTranslation;

    private Set<@NotNull(message = "ID bộ thủ không được để trống") @Positive(message = "ID bộ thủ phải là số nguyên dương") Integer> radicalIds = new HashSet<>();

    public CreateVocabularyRequest() {
    }

    public CreateVocabularyRequest(String hanzi, String pinyin, String pinyinRaw,
                                   String meaningHanViet, String meaningVi) {
        this.hanzi = hanzi;
        this.pinyin = pinyin;
        this.pinyinRaw = pinyinRaw;
        this.meaningHanViet = meaningHanViet;
        this.meaningVi = meaningVi;
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

    public Set<Integer> getRadicalIds() {
        return radicalIds;
    }

    public void setRadicalIds(Set<Integer> radicalIds) {
        this.radicalIds = radicalIds;
    }
}
