package com.elearning.dto.response;

import com.elearning.entity.Radical;
import com.elearning.entity.Vocabulary;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response DTO representing detailed information for a Kangxi radical,
 * including auditing timestamps and related vocabulary constituents.
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
    private List<RelatedVocabularyDto> relatedVocabularies = new ArrayList<>();

    public RadicalDetailResponse() {
        this.relatedVocabularies = new ArrayList<>();
    }

    public RadicalDetailResponse(Integer radicalId, String character, String pinyin,
                                 String meaningHanViet, String meaningVi,
                                 String audioUrl, String videoWritingUrl,
                                 LocalDateTime createdAt, LocalDateTime updatedAt,
                                 List<RelatedVocabularyDto> relatedVocabularies) {
        this.radicalId = radicalId;
        this.character = character;
        this.pinyin = pinyin;
        this.meaningHanViet = meaningHanViet;
        this.meaningVi = meaningVi;
        this.audioUrl = audioUrl;
        this.videoWritingUrl = videoWritingUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.relatedVocabularies = relatedVocabularies != null ? relatedVocabularies : new ArrayList<>();
    }

    public static RadicalDetailResponse fromEntity(Radical radical) {
        if (radical == null) {
            return null;
        }
        List<RelatedVocabularyDto> vocabDtos = new ArrayList<>();
        if (radical.getVocabularies() != null && !radical.getVocabularies().isEmpty()) {
            vocabDtos = radical.getVocabularies().stream()
                    .map(RelatedVocabularyDto::fromEntity)
                    .toList();
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
                radical.getUpdatedAt(),
                vocabDtos
        );
    }

    public static RadicalDetailResponse fromEntity(Radical radical, List<Vocabulary> vocabularies) {
        if (radical == null) {
            return null;
        }
        List<RelatedVocabularyDto> vocabDtos = Collections.emptyList();
        if (vocabularies != null && !vocabularies.isEmpty()) {
            vocabDtos = vocabularies.stream()
                    .map(RelatedVocabularyDto::fromEntity)
                    .toList();
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
                radical.getUpdatedAt(),
                vocabDtos
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

    public List<RelatedVocabularyDto> getRelatedVocabularies() {
        return relatedVocabularies;
    }

    public void setRelatedVocabularies(List<RelatedVocabularyDto> relatedVocabularies) {
        this.relatedVocabularies = relatedVocabularies != null ? relatedVocabularies : new ArrayList<>();
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
                ", relatedVocabulariesCount=" + (relatedVocabularies != null ? relatedVocabularies.size() : 0) +
                '}';
    }

    /**
     * DTO representing related vocabulary constituent within a radical detail response.
     * Prevents leaking Vocabulary JPA entity or relations.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RelatedVocabularyDto {

        private Long vocabId;
        private String hanzi;
        private String pinyin;
        private String pinyinRaw;
        private String meaningHanViet;
        private String meaningVi;

        public RelatedVocabularyDto() {
        }

        public RelatedVocabularyDto(Long vocabId, String hanzi, String pinyin, String pinyinRaw,
                                    String meaningHanViet, String meaningVi) {
            this.vocabId = vocabId;
            this.hanzi = hanzi;
            this.pinyin = pinyin;
            this.pinyinRaw = pinyinRaw;
            this.meaningHanViet = meaningHanViet;
            this.meaningVi = meaningVi;
        }

        public static RelatedVocabularyDto fromEntity(Vocabulary vocabulary) {
            if (vocabulary == null) {
                return null;
            }
            return new RelatedVocabularyDto(
                    vocabulary.getVocabId(),
                    vocabulary.getHanzi(),
                    vocabulary.getPinyin(),
                    vocabulary.getPinyinRaw(),
                    vocabulary.getMeaningHanViet(),
                    vocabulary.getMeaningVi()
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

        @Override
        public String toString() {
            return "RelatedVocabularyDto{" +
                    "vocabId=" + vocabId +
                    ", hanzi='" + hanzi + '\'' +
                    ", pinyin='" + pinyin + '\'' +
                    ", pinyinRaw='" + pinyinRaw + '\'' +
                    ", meaningHanViet='" + meaningHanViet + '\'' +
                    ", meaningVi='" + meaningVi + '\'' +
                    '}';
        }
    }
}
