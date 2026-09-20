package com.elearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new Kangxi Radical (POST /api/v1/admin/radicals).
 * Conforms to .agents/API.md and .agents/DATABASE_DESIGN.md.
 */
public class CreateRadicalRequest {

    @NotBlank(message = "Ký tự bộ thủ không được để trống")
    @Size(max = 10, message = "Ký tự bộ thủ không được vượt quá 10 ký tự")
    private String character;

    @NotBlank(message = "Pinyin không được để trống")
    @Size(max = 50, message = "Pinyin không được vượt quá 50 ký tự")
    private String pinyin;

    @NotBlank(message = "Nghĩa Hán-Việt không được để trống")
    @Size(max = 100, message = "Nghĩa Hán-Việt không được vượt quá 100 ký tự")
    private String meaningHanViet;

    @NotBlank(message = "Nghĩa tiếng Việt không được để trống")
    @Size(max = 255, message = "Nghĩa tiếng Việt không được vượt quá 255 ký tự")
    private String meaningVi;

    @Size(max = 500, message = "Đường dẫn audio không được vượt quá 500 ký tự")
    private String audioUrl;

    @Size(max = 500, message = "Đường dẫn video không được vượt quá 500 ký tự")
    private String videoWritingUrl;

    public CreateRadicalRequest() {
    }

    public CreateRadicalRequest(String character, String pinyin, String meaningHanViet,
                                String meaningVi, String audioUrl, String videoWritingUrl) {
        this.character = character;
        this.pinyin = pinyin;
        this.meaningHanViet = meaningHanViet;
        this.meaningVi = meaningVi;
        this.audioUrl = audioUrl;
        this.videoWritingUrl = videoWritingUrl;
    }

    public CreateRadicalRequest(String character, String pinyin, String meaningHanViet, String meaningVi) {
        this(character, pinyin, meaningHanViet, meaningVi, null, null);
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

    @Override
    public String toString() {
        return "CreateRadicalRequest{" +
                "character='" + character + '\'' +
                ", pinyin='" + pinyin + '\'' +
                ", meaningHanViet='" + meaningHanViet + '\'' +
                ", meaningVi='" + meaningVi + '\'' +
                ", audioUrl='" + audioUrl + '\'' +
                ", videoWritingUrl='" + videoWritingUrl + '\'' +
                '}';
    }
}
