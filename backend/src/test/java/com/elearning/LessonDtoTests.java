package com.elearning;

import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.dto.response.LessonVocabItemResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Vocabulary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Lesson DTO Unit Tests (LessonSummaryResponse, LessonDetailResponse, LessonVocabItemResponse)")
class LessonDtoTests {

    @Nested
    @DisplayName("1. LessonVocabItemResponse Tests")
    class LessonVocabItemResponseTests {

        @Test
        @DisplayName("GIVEN null LessonVocabulary or null Vocabulary WHEN fromEntity THEN returns null")
        void testFromEntityNull() {
            assertThat(LessonVocabItemResponse.fromEntity(null)).isNull();

            LessonVocabulary lvWithoutVocab = new LessonVocabulary();
            assertThat(LessonVocabItemResponse.fromEntity(lvWithoutVocab)).isNull();
        }

        @Test
        @DisplayName("GIVEN valid LessonVocabulary WHEN fromEntity THEN all scalar fields and orderIndex correctly mapped")
        void testFromEntityValid() {
            Vocabulary vocab = new Vocabulary("书", "shū", "shu", "Thư", "Sách");
            vocab.setVocabId(10L);
            vocab.setAudioUrl("http://audio/shu.mp3");
            vocab.setVideoWritingUrl("http://video/shu.mp4");
            vocab.setExampleSentence("我买了一本书。");
            vocab.setExampleTranslation("Tôi đã mua một cuốn sách.");

            Lesson lesson = new Lesson("Bài 1", new Account());
            lesson.setLessonId(1L);

            LessonVocabulary lv = new LessonVocabulary(lesson, vocab, 1);

            LessonVocabItemResponse response = LessonVocabItemResponse.fromEntity(lv);

            assertThat(response).isNotNull();
            assertThat(response.getVocabId()).isEqualTo(10L);
            assertThat(response.getHanzi()).isEqualTo("书");
            assertThat(response.getPinyin()).isEqualTo("shū");
            assertThat(response.getPinyinRaw()).isEqualTo("shu");
            assertThat(response.getMeaningHanViet()).isEqualTo("Thư");
            assertThat(response.getMeaningVi()).isEqualTo("Sách");
            assertThat(response.getAudioUrl()).isEqualTo("http://audio/shu.mp3");
            assertThat(response.getVideoWritingUrl()).isEqualTo("http://video/shu.mp4");
            assertThat(response.getExampleSentence()).isEqualTo("我买了一本书。");
            assertThat(response.getExampleTranslation()).isEqualTo("Tôi đã mua một cuốn sách.");
            assertThat(response.getOrderIndex()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("2. LessonSummaryResponse Tests")
    class LessonSummaryResponseTests {

        @Test
        @DisplayName("GIVEN null Lesson WHEN fromEntity THEN returns null")
        void testFromEntityNull() {
            assertThat(LessonSummaryResponse.fromEntity(null)).isNull();
            assertThat(LessonSummaryResponse.fromEntity(null, 5)).isNull();
        }

        @Test
        @DisplayName("GIVEN Lesson with vocabularies WHEN fromEntity THEN maps correctly with vocabularyCount")
        void testFromEntityValid() {
            Lesson lesson = new Lesson("Học tiếng Trung cơ bản", new Account());
            lesson.setLessonId(5L);
            lesson.setStatus("Approved");

            Vocabulary v1 = new Vocabulary("你", "nǐ", "ni", "Nhĩ", "Bạn");
            lesson.addVocabulary(v1, 1);

            LessonSummaryResponse response = LessonSummaryResponse.fromEntity(lesson);

            assertThat(response).isNotNull();
            assertThat(response.getLessonId()).isEqualTo(5L);
            assertThat(response.getTitle()).isEqualTo("Học tiếng Trung cơ bản");
            assertThat(response.getStatus()).isEqualTo("Approved");
            assertThat(response.getVocabularyCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("GIVEN JPQL constructor projection with Long count WHEN instantiated THEN vocabularyCount cast to int")
        void testConstructorWithLongCount() {
            LocalDateTime now = LocalDateTime.now();
            LessonSummaryResponse response = new LessonSummaryResponse(10L, "Bài học 1", "Approved", 5L, now, now);

            assertThat(response.getLessonId()).isEqualTo(10L);
            assertThat(response.getVocabularyCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("3. LessonDetailResponse Tests")
    class LessonDetailResponseTests {

        @Test
        @DisplayName("GIVEN null Lesson WHEN fromEntity THEN returns null")
        void testFromEntityNull() {
            assertThat(LessonDetailResponse.fromEntity(null, null)).isNull();
        }

        @Test
        @DisplayName("GIVEN LessonVocabularies out of order (3, 1, 2) WHEN fromEntity THEN enforces strict ascending order (1, 2, 3)")
        void testFromEntityEnforcesOrdering() {
            Lesson lesson = new Lesson("Từ vựng HSK 1", new Account());
            lesson.setLessonId(1L);
            lesson.setStatus("Approved");

            Vocabulary v1 = new Vocabulary("一", "yī", "yi", "Nhất", "Một");
            v1.setVocabId(101L);
            Vocabulary v2 = new Vocabulary("二", "èr", "er", "Nhị", "Hai");
            v2.setVocabId(102L);
            Vocabulary v3 = new Vocabulary("三", "sān", "san", "Tam", "Ba");
            v3.setVocabId(103L);

            // Add out of order
            List<LessonVocabulary> outOfOrderList = new ArrayList<>();
            outOfOrderList.add(new LessonVocabulary(lesson, v3, 3));
            outOfOrderList.add(new LessonVocabulary(lesson, v1, 1));
            outOfOrderList.add(new LessonVocabulary(lesson, v2, 2));

            LessonDetailResponse response = LessonDetailResponse.fromEntity(lesson, outOfOrderList);

            assertThat(response).isNotNull();
            assertThat(response.getLessonId()).isEqualTo(1L);
            assertThat(response.getTitle()).isEqualTo("Từ vựng HSK 1");
            assertThat(response.getStatus()).isEqualTo("Approved");
            assertThat(response.getVocabularyCount()).isEqualTo(3);
            assertThat(response.getVocabularies()).hasSize(3);

            // Assert deterministic ascending ordering 1 -> 2 -> 3
            assertThat(response.getVocabularies().get(0).getOrderIndex()).isEqualTo(1);
            assertThat(response.getVocabularies().get(0).getHanzi()).isEqualTo("一");

            assertThat(response.getVocabularies().get(1).getOrderIndex()).isEqualTo(2);
            assertThat(response.getVocabularies().get(1).getHanzi()).isEqualTo("二");

            assertThat(response.getVocabularies().get(2).getOrderIndex()).isEqualTo(3);
            assertThat(response.getVocabularies().get(2).getHanzi()).isEqualTo("三");
        }
    }
}
