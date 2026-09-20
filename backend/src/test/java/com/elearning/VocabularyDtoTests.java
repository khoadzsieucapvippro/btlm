package com.elearning;

import com.elearning.dto.request.VocabularySearchCriteria;
import com.elearning.dto.response.VocabularyDetailResponse;
import com.elearning.dto.response.VocabularyResponse;
import com.elearning.entity.Radical;
import com.elearning.entity.Vocabulary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Vocabulary DTO Unit Tests")
class VocabularyDtoTests {

    @Nested
    @DisplayName("VocabularyResponse Mapping Tests")
    class VocabularyResponseTests {

        @Test
        @DisplayName("GIVEN populated Vocabulary entity WHEN fromEntity THEN maps all fields accurately")
        void testFromEntityPopulated() {
            Vocabulary vocab = new Vocabulary();
            vocab.setVocabId(10L);
            vocab.setHanzi("学");
            vocab.setPinyin("xué");
            vocab.setPinyinRaw("xue");
            vocab.setMeaningHanViet("Học");
            vocab.setMeaningVi("Học tập");
            vocab.setAudioUrl("http://audio/xue.mp3");
            vocab.setVideoWritingUrl("http://video/xue.mp4");
            vocab.setExampleSentence("他在学习汉语。");
            vocab.setExampleTranslation("Anh ấy đang học tiếng Trung.");

            VocabularyResponse response = VocabularyResponse.fromEntity(vocab);

            assertThat(response).isNotNull();
            assertThat(response.getVocabId()).isEqualTo(10L);
            assertThat(response.getHanzi()).isEqualTo("学");
            assertThat(response.getPinyin()).isEqualTo("xué");
            assertThat(response.getPinyinRaw()).isEqualTo("xue");
            assertThat(response.getMeaningHanViet()).isEqualTo("Học");
            assertThat(response.getMeaningVi()).isEqualTo("Học tập");
            assertThat(response.getAudioUrl()).isEqualTo("http://audio/xue.mp3");
            assertThat(response.getVideoWritingUrl()).isEqualTo("http://video/xue.mp4");
            assertThat(response.getExampleSentence()).isEqualTo("他在学习汉语。");
            assertThat(response.getExampleTranslation()).isEqualTo("Anh ấy đang học tiếng Trung.");
        }

        @Test
        @DisplayName("GIVEN null entity WHEN fromEntity THEN returns null")
        void testFromEntityNull() {
            assertThat(VocabularyResponse.fromEntity(null)).isNull();
        }
    }

    @Nested
    @DisplayName("VocabularyDetailResponse Mapping Tests")
    class VocabularyDetailResponseTests {

        @Test
        @DisplayName("GIVEN Vocabulary entity with associated Radicals WHEN fromEntity THEN maps radicals sorted by radicalId")
        void testFromEntityWithRadicals() {
            Vocabulary vocab = new Vocabulary();
            vocab.setVocabId(25L);
            vocab.setHanzi("休");
            vocab.setPinyin("xiū");
            vocab.setPinyinRaw("xiu");
            vocab.setMeaningHanViet("Hưu");
            vocab.setMeaningVi("Nghỉ ngơi");
            vocab.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
            vocab.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 12, 0));

            Radical r1 = new Radical();
            r1.setRadicalId(75);
            r1.setCharacter("木");
            r1.setPinyin("mù");
            r1.setMeaningHanViet("Mộc");
            r1.setMeaningVi("Cây gỗ");

            Radical r2 = new Radical();
            r2.setRadicalId(9);
            r2.setCharacter("人");
            r2.setPinyin("rén");
            r2.setMeaningHanViet("Nhân");
            r2.setMeaningVi("Người");

            vocab.setRadicals(Set.of(r1, r2));

            VocabularyDetailResponse response = VocabularyDetailResponse.fromEntity(vocab);

            assertThat(response).isNotNull();
            assertThat(response.getVocabId()).isEqualTo(25L);
            assertThat(response.getHanzi()).isEqualTo("休");
            assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
            assertThat(response.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 12, 0));

            // Radicals should be sorted by radicalId (9 before 75)
            assertThat(response.getRadicals()).hasSize(2);
            assertThat(response.getRadicals().get(0).getRadicalId()).isEqualTo(9);
            assertThat(response.getRadicals().get(0).getCharacter()).isEqualTo("人");
            assertThat(response.getRadicals().get(1).getRadicalId()).isEqualTo(75);
            assertThat(response.getRadicals().get(1).getCharacter()).isEqualTo("木");
        }

        @Test
        @DisplayName("GIVEN Vocabulary entity without radicals WHEN fromEntity THEN returns empty radical list")
        void testFromEntityWithoutRadicals() {
            Vocabulary vocab = new Vocabulary("买", "mǎi", "mai", "Mãi", "Mua");
            vocab.setVocabId(5L);

            VocabularyDetailResponse response = VocabularyDetailResponse.fromEntity(vocab);

            assertThat(response).isNotNull();
            assertThat(response.getVocabId()).isEqualTo(5L);
            assertThat(response.getRadicals()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("GIVEN null entity WHEN fromEntity THEN returns null")
        void testFromEntityNull() {
            assertThat(VocabularyDetailResponse.fromEntity(null)).isNull();
        }
    }

    @Nested
    @DisplayName("VocabularySearchCriteria Tests")
    class VocabularySearchCriteriaTests {

        @Test
        @DisplayName("GIVEN empty or whitespace criteria WHEN isEmpty THEN returns true")
        void testIsEmptyTrue() {
            VocabularySearchCriteria c1 = new VocabularySearchCriteria();
            assertThat(c1.isEmpty()).isTrue();

            VocabularySearchCriteria c2 = new VocabularySearchCriteria("", "   ", "\t", null, "");
            assertThat(c2.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("GIVEN populated field WHEN isEmpty THEN returns false")
        void testIsEmptyFalse() {
            VocabularySearchCriteria c1 = new VocabularySearchCriteria();
            c1.setHanzi("学");
            assertThat(c1.isEmpty()).isFalse();

            VocabularySearchCriteria c2 = new VocabularySearchCriteria();
            c2.setPinyin("xue");
            assertThat(c2.isEmpty()).isFalse();

            VocabularySearchCriteria c3 = new VocabularySearchCriteria();
            c3.setPinyinRaw("xue");
            assertThat(c3.isEmpty()).isFalse();

            VocabularySearchCriteria c4 = new VocabularySearchCriteria();
            c4.setRadicalId(9);
            assertThat(c4.isEmpty()).isFalse();

            VocabularySearchCriteria c5 = VocabularySearchCriteria.byKeyword("zhong");
            assertThat(c5.isEmpty()).isFalse();
        }
    }
}
