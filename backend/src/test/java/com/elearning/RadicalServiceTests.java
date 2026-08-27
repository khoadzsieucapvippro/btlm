package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.response.RadicalDetailResponse;
import com.elearning.dto.response.RadicalResponse;
import com.elearning.entity.Radical;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.RadicalService;
import com.elearning.service.impl.RadicalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 4A.1: RadicalService Unit Tests")
class RadicalServiceTests {

    @Mock
    private RadicalRepository radicalRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    private RadicalService radicalService;

    private Radical sampleRadical1;
    private Radical sampleRadical2;
    private Vocabulary sampleVocab1;

    @BeforeEach
    void setUp() {
        radicalService = new RadicalServiceImpl(radicalRepository, vocabularyRepository);

        sampleRadical1 = new Radical();
        sampleRadical1.setRadicalId(1);
        sampleRadical1.setCharacter("一");
        sampleRadical1.setPinyin("yī");
        sampleRadical1.setMeaningHanViet("Nhất");
        sampleRadical1.setMeaningVi("Một, thứ nhất");
        sampleRadical1.setAudioUrl("https://cdn.example.com/audio/radicals/1.mp3");
        sampleRadical1.setVideoWritingUrl("https://cdn.example.com/video/radicals/1.mp4");
        sampleRadical1.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        sampleRadical1.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 12, 0));

        sampleRadical2 = new Radical();
        sampleRadical2.setRadicalId(2);
        sampleRadical2.setCharacter("丨");
        sampleRadical2.setPinyin("gǔn");
        sampleRadical2.setMeaningHanViet("Cổn");
        sampleRadical2.setMeaningVi("Nét sổ thẳng");
        sampleRadical2.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        sampleRadical2.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

        sampleVocab1 = new Vocabulary();
        sampleVocab1.setVocabId(101L);
        sampleVocab1.setHanzi("一天");
        sampleVocab1.setPinyin("yī tiān");
        sampleVocab1.setPinyinRaw("yi tian");
        sampleVocab1.setMeaningHanViet("Nhất thiên");
        sampleVocab1.setMeaningVi("Một ngày");
    }

    @Nested
    @DisplayName("1. Listing Kangxi Radicals (getAllRadicals)")
    class GetAllRadicalsTests {

        @Test
        @DisplayName("GIVEN unpaged request WHEN getAllRadicals THEN returns list sorted by radicalId")
        void testGetAllRadicalsList() {
            when(radicalRepository.findAll(any(Sort.class)))
                    .thenReturn(List.of(sampleRadical1, sampleRadical2));

            List<RadicalResponse> result = radicalService.getAllRadicals();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRadicalId()).isEqualTo(1);
            assertThat(result.get(0).getCharacter()).isEqualTo("一");
            assertThat(result.get(0).getPinyin()).isEqualTo("yī");
            assertThat(result.get(0).getMeaningHanViet()).isEqualTo("Nhất");
            assertThat(result.get(0).getMeaningVi()).isEqualTo("Một, thứ nhất");
            assertThat(result.get(0).getAudioUrl()).isEqualTo("https://cdn.example.com/audio/radicals/1.mp3");
            assertThat(result.get(0).getVideoWritingUrl()).isEqualTo("https://cdn.example.com/video/radicals/1.mp4");

            assertThat(result.get(1).getRadicalId()).isEqualTo(2);
            assertThat(result.get(1).getCharacter()).isEqualTo("丨");
            verify(radicalRepository).findAll(Sort.by(Sort.Direction.ASC, "radicalId"));
        }

        @Test
        @DisplayName("GIVEN Pageable without explicit sort WHEN getAllRadicals THEN applies default canonical sort")
        void testGetAllRadicalsPagedDefaultSort() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Radical> page = new PageImpl<>(List.of(sampleRadical1, sampleRadical2), pageable, 2);

            when(radicalRepository.findAll(any(Pageable.class))).thenReturn(page);

            Page<RadicalResponse> result = radicalService.getAllRadicals(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getCharacter()).isEqualTo("一");
        }
    }

    @Nested
    @DisplayName("2. Find Radical By ID (getRadicalById)")
    class GetRadicalByIdTests {

        @Test
        @DisplayName("GIVEN existing radical ID WHEN getRadicalById THEN returns detail response with related vocabularies")
        void testGetRadicalByIdFound() {
            when(radicalRepository.findById(1)).thenReturn(Optional.of(sampleRadical1));
            when(vocabularyRepository.findByRadicalId(1)).thenReturn(List.of(sampleVocab1));

            RadicalDetailResponse response = radicalService.getRadicalById(1);

            assertThat(response).isNotNull();
            assertThat(response.getRadicalId()).isEqualTo(1);
            assertThat(response.getCharacter()).isEqualTo("一");
            assertThat(response.getPinyin()).isEqualTo("yī");
            assertThat(response.getMeaningHanViet()).isEqualTo("Nhất");
            assertThat(response.getMeaningVi()).isEqualTo("Một, thứ nhất");
            assertThat(response.getAudioUrl()).isEqualTo("https://cdn.example.com/audio/radicals/1.mp3");
            assertThat(response.getVideoWritingUrl()).isEqualTo("https://cdn.example.com/video/radicals/1.mp4");
            assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
            assertThat(response.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 12, 0));

            assertThat(response.getRelatedVocabularies()).hasSize(1);
            assertThat(response.getRelatedVocabularies().get(0).getVocabId()).isEqualTo(101L);
            assertThat(response.getRelatedVocabularies().get(0).getHanzi()).isEqualTo("一天");
            assertThat(response.getRelatedVocabularies().get(0).getPinyin()).isEqualTo("yī tiān");
            assertThat(response.getRelatedVocabularies().get(0).getPinyinRaw()).isEqualTo("yi tian");
            assertThat(response.getRelatedVocabularies().get(0).getMeaningHanViet()).isEqualTo("Nhất thiên");
            assertThat(response.getRelatedVocabularies().get(0).getMeaningVi()).isEqualTo("Một ngày");
        }

        @Test
        @DisplayName("GIVEN nonexistent radical ID WHEN getRadicalById THEN throws BusinessException with NOT_FOUND")
        void testGetRadicalByIdNotFound() {
            when(radicalRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> radicalService.getRadicalById(999))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                        assertThat(be.getMessage()).contains("999");
                    });
        }

        @Test
        @DisplayName("GIVEN null ID WHEN getRadicalById THEN throws BusinessException with VALIDATION_ERROR")
        void testGetRadicalByIdNullId() {
            assertThatThrownBy(() -> radicalService.getRadicalById(null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    });
        }
    }

    @Nested
    @DisplayName("3. Find Radical By Character (getRadicalByCharacter)")
    class GetRadicalByCharacterTests {

        @Test
        @DisplayName("GIVEN existing character WHEN getRadicalByCharacter THEN returns detail response")
        void testGetRadicalByCharacterFound() {
            when(radicalRepository.findByCharacter("一")).thenReturn(Optional.of(sampleRadical1));
            when(vocabularyRepository.findByRadicalId(1)).thenReturn(Collections.emptyList());

            RadicalDetailResponse response = radicalService.getRadicalByCharacter("一");

            assertThat(response).isNotNull();
            assertThat(response.getRadicalId()).isEqualTo(1);
            assertThat(response.getCharacter()).isEqualTo("一");
            assertThat(response.getMeaningHanViet()).isEqualTo("Nhất");
            assertThat(response.getRelatedVocabularies()).isEmpty();
        }

        @Test
        @DisplayName("GIVEN nonexistent character WHEN getRadicalByCharacter THEN throws BusinessException with NOT_FOUND")
        void testGetRadicalByCharacterNotFound() {
            when(radicalRepository.findByCharacter("龍")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> radicalService.getRadicalByCharacter("龍"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                        assertThat(be.getMessage()).contains("龍");
                    });
        }

        @Test
        @DisplayName("GIVEN null or blank character WHEN getRadicalByCharacter THEN throws BusinessException with VALIDATION_ERROR")
        void testGetRadicalByCharacterBlank() {
            assertThatThrownBy(() -> radicalService.getRadicalByCharacter("  "))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    });

            assertThatThrownBy(() -> radicalService.getRadicalByCharacter(null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    });
        }
    }

    @Nested
    @DisplayName("4. Related Vocabularies By Radical ID (getRelatedVocabularies)")
    class GetRelatedVocabulariesTests {

        @Test
        @DisplayName("GIVEN existing radical ID WHEN getRelatedVocabularies THEN returns mapped vocabulary DTOs")
        void testGetRelatedVocabulariesSuccess() {
            when(radicalRepository.existsById(1)).thenReturn(true);
            when(vocabularyRepository.findByRadicalId(1)).thenReturn(List.of(sampleVocab1));

            List<RadicalDetailResponse.RelatedVocabularyDto> result = radicalService.getRelatedVocabularies(1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getVocabId()).isEqualTo(101L);
            assertThat(result.get(0).getHanzi()).isEqualTo("一天");
            assertThat(result.get(0).getPinyin()).isEqualTo("yī tiān");
            assertThat(result.get(0).getPinyinRaw()).isEqualTo("yi tian");
        }

        @Test
        @DisplayName("GIVEN nonexistent radical ID WHEN getRelatedVocabularies THEN throws BusinessException with NOT_FOUND")
        void testGetRelatedVocabulariesNotFound() {
            when(radicalRepository.existsById(999)).thenReturn(false);

            assertThatThrownBy(() -> radicalService.getRelatedVocabularies(999))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("GIVEN null ID WHEN getRelatedVocabularies THEN throws BusinessException with VALIDATION_ERROR")
        void testGetRelatedVocabulariesNullId() {
            assertThatThrownBy(() -> radicalService.getRelatedVocabularies(null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    });
        }
    }
}
