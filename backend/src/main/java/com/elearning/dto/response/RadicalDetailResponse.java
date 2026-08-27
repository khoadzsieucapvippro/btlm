package com.elearning.dto.response;

import com.elearning.entity.Radical;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Response DTO representing detailed information for a Kangxi radical,
 * including radical metadata and auditing timestamps.
 * Does not include vocabulary constituents (vocabulary operations belong to Module 4B).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RadicalDetailResponse {

    private Integer radicalId;
    private String character;
    private String pinyin;
    private String meaningHanViet;
    private String meaningVi;
    private String audioUrl;
    private String videoWritingUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RadicalDetailResponse() {
    }

    public RadicalDetailResponse(Integer radicalId, String character, String pinyin,
                                 String meaningHanViet, String meaningVi,
                                 String audioUrl, String videoWritingUrl,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.radicalId = radicalId;
        this.character = character;
        this.pinyin = pinyin;
        this.meaningHanViet = meaningHanViet;
        this.meaningVi = meaningVi;
        this.audioUrl = audioUrl;
        this.videoWritingUrl = videoWritingUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RadicalDetailResponse fromEntity(Radical radical) {
        if (radical == null) {
            return null;
        }
        return new RadicalDetailResponse(
                radical.getRadicalId(),
                radical.getCharacter(),
                radical.getPinyin(),
                radical.getMeaningHanViet(),
                radical.getMeaningVi(),
                radical.getAudioUrl(),
                radical.getVideoWritingUrl(),
                radical.getCreatedAt(),
                radical.getUpdatedAt()
        );
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

    @Override
    public String toString() {
        return "RadicalDetailResponse{" +
                "radicalId=" + radicalId +
                ", character='" + character + '\'' +
                ", pinyin='" + pinyin + '\'' +
                ", meaningHanViet='" + meaningHanViet + '\'' +
                ", meaningVi='" + meaningVi + '\'' +
                ", audioUrl='" + audioUrl + '\'' +
                ", videoWritingUrl='" + videoWritingUrl + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
