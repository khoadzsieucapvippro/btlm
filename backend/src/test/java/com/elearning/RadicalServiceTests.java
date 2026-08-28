package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.response.RadicalDetailResponse;
import com.elearning.dto.response.RadicalResponse;
import com.elearning.entity.Radical;
import com.elearning.exception.BusinessException;
import com.elearning.repository.RadicalRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 4A.1: RadicalService Unit Tests (Clean Metadata Contract)")
class RadicalServiceTests {

    @Mock
    private RadicalRepository radicalRepository;

    private RadicalService radicalService;

    private Radical sampleRadical1;
    private Radical sampleRadical2;

    @BeforeEach
    void setUp() {
        radicalService = new RadicalServiceImpl(radicalRepository);

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
        @DisplayName("GIVEN existing radical ID WHEN getRadicalById THEN returns detail response with metadata only")
        void testGetRadicalByIdFound() {
            when(radicalRepository.findById(1)).thenReturn(Optional.of(sampleRadical1));

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

            RadicalDetailResponse response = radicalService.getRadicalByCharacter("一");

            assertThat(response).isNotNull();
            assertThat(response.getRadicalId()).isEqualTo(1);
            assertThat(response.getCharacter()).isEqualTo("一");
            assertThat(response.getMeaningHanViet()).isEqualTo("Nhất");
            assertThat(response.getMeaningVi()).isEqualTo("Một, thứ nhất");
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
    @DisplayName("4. Create Radical (createRadical)")
    class CreateRadicalTests {

        @Test
        @DisplayName("GIVEN valid create request WHEN createRadical THEN persists and returns detail response")
        void testCreateRadicalSuccess() {
            com.elearning.dto.request.CreateRadicalRequest request = new com.elearning.dto.request.CreateRadicalRequest(
                    "口", "kǒu", "Khẩu", "Miệng", "http://audio.mp3", "http://video.mp4"
            );

            when(radicalRepository.existsByCharacter("口")).thenReturn(false);
            when(radicalRepository.save(any(Radical.class))).thenAnswer(invocation -> {
                Radical r = invocation.getArgument(0);
                r.setRadicalId(30);
                r.setCreatedAt(LocalDateTime.now());
                r.setUpdatedAt(LocalDateTime.now());
                return r;
            });

            RadicalDetailResponse response = radicalService.createRadical(request);

            assertThat(response).isNotNull();
            assertThat(response.getRadicalId()).isEqualTo(30);
            assertThat(response.getCharacter()).isEqualTo("口");
            assertThat(response.getPinyin()).isEqualTo("kǒu");
            assertThat(response.getMeaningHanViet()).isEqualTo("Khẩu");
            assertThat(response.getMeaningVi()).isEqualTo("Miệng");
            assertThat(response.getAudioUrl()).isEqualTo("http://audio.mp3");
            assertThat(response.getVideoWritingUrl()).isEqualTo("http://video.mp4");
            verify(radicalRepository).save(any(Radical.class));
        }

        @Test
        @DisplayName("GIVEN duplicate character WHEN createRadical THEN throws CONFLICT")
        void testCreateRadicalDuplicate() {
            com.elearning.dto.request.CreateRadicalRequest request = new com.elearning.dto.request.CreateRadicalRequest(
                    "一", "yī", "Nhất", "Một", null, null
            );

            when(radicalRepository.existsByCharacter("一")).thenReturn(true);

            assertThatThrownBy(() -> radicalService.createRadical(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });
        }

        @Test
        @DisplayName("GIVEN null request or blank character WHEN createRadical THEN throws VALIDATION_ERROR")
        void testCreateRadicalValidation() {
            assertThatThrownBy(() -> radicalService.createRadical(null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

            com.elearning.dto.request.CreateRadicalRequest blankReq = new com.elearning.dto.request.CreateRadicalRequest(
                    "  ", "yī", "Nhất", "Một", null, null
            );
            assertThatThrownBy(() -> radicalService.createRadical(blankReq))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }
    }

    @Nested
    @DisplayName("5. Update Radical (updateRadical)")
    class UpdateRadicalTests {

        @Test
        @DisplayName("GIVEN existing radical and valid update WHEN updateRadical THEN updates and returns detail response")
        void testUpdateRadicalSuccess() {
            com.elearning.dto.request.UpdateRadicalRequest request = new com.elearning.dto.request.UpdateRadicalRequest(
                    "一", "yī", "Nhất", "Số một, đứng đầu", "http://new-audio.mp3", null
            );

            when(radicalRepository.findById(1)).thenReturn(Optional.of(sampleRadical1));
            when(radicalRepository.save(any(Radical.class))).thenAnswer(inv -> inv.getArgument(0));

            RadicalDetailResponse response = radicalService.updateRadical(1, request);

            assertThat(response).isNotNull();
            assertThat(response.getRadicalId()).isEqualTo(1);
            assertThat(response.getMeaningVi()).isEqualTo("Số một, đứng đầu");
            assertThat(response.getAudioUrl()).isEqualTo("http://new-audio.mp3");
            verify(radicalRepository).save(sampleRadical1);
        }

        @Test
        @DisplayName("GIVEN nonexistent ID WHEN updateRadical THEN throws NOT_FOUND")
        void testUpdateRadicalNotFound() {
            com.elearning.dto.request.UpdateRadicalRequest request = new com.elearning.dto.request.UpdateRadicalRequest(
                    "一", "yī", "Nhất", "Số một", null, null
            );

            when(radicalRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> radicalService.updateRadical(999, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("GIVEN character conflict with another radical WHEN updateRadical THEN throws CONFLICT")
        void testUpdateRadicalCharacterConflict() {
            com.elearning.dto.request.UpdateRadicalRequest request = new com.elearning.dto.request.UpdateRadicalRequest(
                    "丨", "gǔn", "Cổn", "Nét sổ", null, null
            );

            when(radicalRepository.findById(1)).thenReturn(Optional.of(sampleRadical1));
            when(radicalRepository.findByCharacter("丨")).thenReturn(Optional.of(sampleRadical2));

            assertThatThrownBy(() -> radicalService.updateRadical(1, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }
    }

    @Nested
    @DisplayName("6. Delete Radical (deleteRadical)")
    class DeleteRadicalTests {

        @Test
        @DisplayName("GIVEN existing radical not linked WHEN deleteRadical THEN deletes successfully")
        void testDeleteRadicalSuccess() {
            when(radicalRepository.findById(1)).thenReturn(Optional.of(sampleRadical1));

            radicalService.deleteRadical(1);

            verify(radicalRepository).delete(sampleRadical1);
        }

        @Test
        @DisplayName("GIVEN nonexistent ID WHEN deleteRadical THEN throws NOT_FOUND")
        void testDeleteRadicalNotFound() {
            when(radicalRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> radicalService.deleteRadical(999))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("GIVEN null ID WHEN deleteRadical THEN throws VALIDATION_ERROR")
        void testDeleteRadicalNullId() {
            assertThatThrownBy(() -> radicalService.deleteRadical(null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }

        @Test
        @DisplayName("GIVEN radical linked to vocabularies WHEN deleteRadical THEN throws CONFLICT")
        void testDeleteRadicalLinkedToVocabularies() {
            Radical linkedRadical = new Radical();
            linkedRadical.setRadicalId(5);
            linkedRadical.setCharacter("乙");
            com.elearning.entity.Vocabulary vocab = new com.elearning.entity.Vocabulary();
            linkedRadical.getVocabularies().add(vocab);

            when(radicalRepository.findById(5)).thenReturn(Optional.of(linkedRadical));

            assertThatThrownBy(() -> radicalService.deleteRadical(5))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        }
    }
}
