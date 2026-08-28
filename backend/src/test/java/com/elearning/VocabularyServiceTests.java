package com.elearning;

import com.elearning.dto.request.CreateVocabularyRequest;
import com.elearning.dto.request.UpdateVocabularyRequest;
import com.elearning.dto.request.VocabularySearchCriteria;
import com.elearning.dto.response.VocabularyDetailResponse;
import com.elearning.dto.response.VocabularyResponse;
import com.elearning.entity.Radical;
import com.elearning.entity.Vocabulary;
import com.elearning.common.ErrorCode;
import com.elearning.exception.BusinessException;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.impl.VocabularyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VocabularyService Unit Tests")
class VocabularyServiceTests {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private RadicalRepository radicalRepository;

    @Mock
    private com.elearning.repository.LessonVocabularyRepository lessonVocabularyRepository;

    @Mock
    private com.elearning.repository.PersonalNoteRepository personalNoteRepository;

    @Mock
    private com.elearning.repository.CardProgressRepository cardProgressRepository;

    @Mock
    private com.elearning.repository.ReviewLogRepository reviewLogRepository;

    @InjectMocks
    private VocabularyServiceImpl vocabularyService;

    private Vocabulary sampleVocab;

    @BeforeEach
    void setUp() {
        sampleVocab = new Vocabulary("学", "xué", "xue", "Học", "Học tập");
        sampleVocab.setVocabId(1L);
    }

    @Nested
    @DisplayName("getAllVocabularies Tests")
    class GetAllVocabulariesTests {

        @Test
        @DisplayName("GIVEN valid Pageable WHEN getAllVocabularies THEN returns paginated VocabularyResponse")
        void testGetAllVocabulariesSuccess() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("vocabId").ascending());
            Page<Vocabulary> page = new PageImpl<>(List.of(sampleVocab), pageable, 1);
            when(vocabularyRepository.findAll(pageable)).thenReturn(page);

            Page<VocabularyResponse> result = vocabularyService.getAllVocabularies(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getHanzi()).isEqualTo("学");
            verify(vocabularyRepository).findAll(pageable);
        }

        @Test
        @DisplayName("GIVEN null Pageable WHEN getAllVocabularies THEN applies default pageable (0, 20, vocabId ASC)")
        void testGetAllVocabulariesNullPageable() {
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(vocabularyRepository.findAll(pageableCaptor.capture()))
                    .thenReturn(new PageImpl<>(List.of(sampleVocab)));

            Page<VocabularyResponse> result = vocabularyService.getAllVocabularies(null);

            assertThat(result).isNotNull();
            Pageable captured = pageableCaptor.getValue();
            assertThat(captured.getPageNumber()).isZero();
            assertThat(captured.getPageSize()).isEqualTo(20);
            assertThat(captured.getSort().getOrderFor("vocabId")).isNotNull();
            assertThat(captured.getSort().getOrderFor("vocabId").getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("GIVEN unsorted Pageable WHEN getAllVocabularies THEN applies default sort vocabId ASC")
        void testGetAllVocabulariesUnsortedPageable() {
            Pageable unsorted = PageRequest.of(1, 15);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(vocabularyRepository.findAll(pageableCaptor.capture()))
                    .thenReturn(new PageImpl<>(List.of()));

            vocabularyService.getAllVocabularies(unsorted);

            Pageable captured = pageableCaptor.getValue();
            assertThat(captured.getPageNumber()).isEqualTo(1);
            assertThat(captured.getPageSize()).isEqualTo(15);
            assertThat(captured.getSort().getOrderFor("vocabId")).isNotNull();
        }
    }

    @Nested
    @DisplayName("searchVocabularies Tests")
    class SearchVocabulariesTests {

        @Test
        @DisplayName("GIVEN null criteria WHEN searchVocabularies THEN delegates to getAllVocabularies")
        void testSearchWithNullCriteria() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("vocabId").ascending());
            when(vocabularyRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(sampleVocab), pageable, 1));

            Page<VocabularyResponse> result = vocabularyService.searchVocabularies(null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(vocabularyRepository).findAll(pageable);
        }

        @Test
        @DisplayName("GIVEN empty criteria WHEN searchVocabularies THEN delegates to getAllVocabularies")
        void testSearchWithEmptyCriteria() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("vocabId").ascending());
            when(vocabularyRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(sampleVocab), pageable, 1));

            VocabularySearchCriteria emptyCriteria = new VocabularySearchCriteria("", "  ", null, null, " ");
            Page<VocabularyResponse> result = vocabularyService.searchVocabularies(emptyCriteria, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(vocabularyRepository).findAll(pageable);
        }

        @Test
        @DisplayName("GIVEN hanzi criteria WHEN searchVocabularies THEN queries repository with Specification")
        void testSearchWithHanzi() {
            VocabularySearchCriteria criteria = new VocabularySearchCriteria();
            criteria.setHanzi("学");
            Pageable pageable = PageRequest.of(0, 10);

            when(vocabularyRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleVocab)));

            Page<VocabularyResponse> result = vocabularyService.searchVocabularies(criteria, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getHanzi()).isEqualTo("学");
            verify(vocabularyRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("GIVEN pinyin criteria WHEN searchVocabularies THEN queries repository with Specification")
        void testSearchWithPinyin() {
            VocabularySearchCriteria criteria = new VocabularySearchCriteria();
            criteria.setPinyin("xué");
            Pageable pageable = PageRequest.of(0, 10);

            when(vocabularyRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleVocab)));

            Page<VocabularyResponse> result = vocabularyService.searchVocabularies(criteria, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(vocabularyRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("GIVEN pinyinRaw criteria WHEN searchVocabularies THEN queries repository with Specification")
        void testSearchWithPinyinRaw() {
            VocabularySearchCriteria criteria = new VocabularySearchCriteria();
            criteria.setPinyinRaw("xue");
            Pageable pageable = PageRequest.of(0, 10);

            when(vocabularyRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleVocab)));

            Page<VocabularyResponse> result = vocabularyService.searchVocabularies(criteria, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(vocabularyRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("GIVEN radicalId criteria WHEN searchVocabularies THEN queries repository with Specification")
        void testSearchWithRadicalId() {
            VocabularySearchCriteria criteria = new VocabularySearchCriteria();
            criteria.setRadicalId(9);
            Pageable pageable = PageRequest.of(0, 10);

            when(vocabularyRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleVocab)));

            Page<VocabularyResponse> result = vocabularyService.searchVocabularies(criteria, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(vocabularyRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("GIVEN unified search keyword criteria WHEN searchVocabularies THEN queries repository with Specification")
        void testSearchWithUnifiedKeyword() {
            VocabularySearchCriteria criteria = VocabularySearchCriteria.byKeyword("zhong");
            Pageable pageable = PageRequest.of(0, 10);

            when(vocabularyRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleVocab)));

            Page<VocabularyResponse> result = vocabularyService.searchVocabularies(criteria, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(vocabularyRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("GIVEN combined criteria (hanzi + radicalId) WHEN searchVocabularies THEN queries repository with Specification")
        void testSearchWithCombinedCriteria() {
            VocabularySearchCriteria criteria = new VocabularySearchCriteria();
            criteria.setHanzi("休");
            criteria.setRadicalId(9);
            Pageable pageable = PageRequest.of(0, 10);

            when(vocabularyRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleVocab)));

            Page<VocabularyResponse> result = vocabularyService.searchVocabularies(criteria, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(vocabularyRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getVocabularyById Tests")
    class GetVocabularyByIdTests {

        @Test
        @DisplayName("GIVEN existing vocabId WHEN getVocabularyById THEN returns VocabularyDetailResponse with radicals")
        void testGetVocabularyByIdSuccess() {
            Radical r = new Radical();
            r.setRadicalId(39);
            r.setCharacter("子");
            r.setPinyin("zǐ");
            r.setMeaningHanViet("Tử");
            r.setMeaningVi("Con, hạt");
            sampleVocab.setRadicals(Set.of(r));

            when(vocabularyRepository.findById(1L)).thenReturn(Optional.of(sampleVocab));

            VocabularyDetailResponse response = vocabularyService.getVocabularyById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getVocabId()).isEqualTo(1L);
            assertThat(response.getHanzi()).isEqualTo("学");
            assertThat(response.getRadicals()).hasSize(1);
            assertThat(response.getRadicals().get(0).getCharacter()).isEqualTo("子");
            verify(vocabularyRepository).findById(1L);
        }

        @Test
        @DisplayName("GIVEN nonexistent vocabId WHEN getVocabularyById THEN throws BusinessException NOT_FOUND")
        void testGetVocabularyByIdNotFound() {
            when(vocabularyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vocabularyService.getVocabularyById(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                        assertThat(be.getMessage()).contains("999");
                    });
        }

        @Test
        @DisplayName("GIVEN null vocabId WHEN getVocabularyById THEN throws BusinessException VALIDATION_ERROR")
        void testGetVocabularyByIdNull() {
            assertThatThrownBy(() -> vocabularyService.getVocabularyById(null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                        assertThat(be.getMessage()).contains("không được để trống");
                    });
        }
    }

    @Nested
    @DisplayName("createVocabulary Tests")
    class CreateVocabularyTests {

        @Test
        @DisplayName("GIVEN valid request with radicals WHEN createVocabulary THEN creates and returns DetailResponse")
        void testCreateVocabularySuccess() {
            CreateVocabularyRequest request = new CreateVocabularyRequest("休", "xiū", "xiu", "Hưu", "Nghỉ ngơi");
            request.setRadicalIds(Set.of(9, 75));

            Radical r9 = new Radical(9, "人", "rén", "Nhân", "Người");
            Radical r75 = new Radical(75, "木", "mù", "Mộc", "Cây");

            when(vocabularyRepository.findByHanziAndPinyinRaw("休", "xiu")).thenReturn(Optional.empty());
            when(radicalRepository.findById(9)).thenReturn(Optional.of(r9));
            when(radicalRepository.findById(75)).thenReturn(Optional.of(r75));
            when(vocabularyRepository.save(any(Vocabulary.class))).thenAnswer(inv -> {
                Vocabulary v = inv.getArgument(0);
                v.setVocabId(10L);
                return v;
            });

            VocabularyDetailResponse response = vocabularyService.createVocabulary(request);

            assertThat(response).isNotNull();
            assertThat(response.getVocabId()).isEqualTo(10L);
            assertThat(response.getHanzi()).isEqualTo("休");
            assertThat(response.getPinyin()).isEqualTo("xiū");
            assertThat(response.getPinyinRaw()).isEqualTo("xiu");
            assertThat(response.getRadicals()).hasSize(2);
            verify(vocabularyRepository).save(any(Vocabulary.class));
        }

        @Test
        @DisplayName("GIVEN valid request without radicals WHEN createVocabulary THEN creates successfully")
        void testCreateVocabularyWithoutRadicals() {
            CreateVocabularyRequest request = new CreateVocabularyRequest("你", "nǐ", "ni", "Nhĩ", "Bạn");

            when(vocabularyRepository.findByHanziAndPinyinRaw("你", "ni")).thenReturn(Optional.empty());
            when(vocabularyRepository.save(any(Vocabulary.class))).thenAnswer(inv -> {
                Vocabulary v = inv.getArgument(0);
                v.setVocabId(11L);
                return v;
            });

            VocabularyDetailResponse response = vocabularyService.createVocabulary(request);

            assertThat(response.getVocabId()).isEqualTo(11L);
            assertThat(response.getRadicals()).isEmpty();
            verify(vocabularyRepository).save(any(Vocabulary.class));
        }

        @Test
        @DisplayName("GIVEN null request WHEN createVocabulary THEN throws BusinessException VALIDATION_ERROR")
        void testCreateVocabularyNullRequest() {
            assertThatThrownBy(() -> vocabularyService.createVocabulary(null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    });
        }

        @Test
        @DisplayName("GIVEN duplicate hanzi and pinyinRaw WHEN createVocabulary THEN throws BusinessException CONFLICT")
        void testCreateVocabularyConflict() {
            CreateVocabularyRequest request = new CreateVocabularyRequest("学", "xué", "xue", "Học", "Học tập");

            when(vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")).thenReturn(Optional.of(sampleVocab));

            assertThatThrownBy(() -> vocabularyService.createVocabulary(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("đã tồn tại");
                    });
        }

        @Test
        @DisplayName("GIVEN invalid radicalId WHEN createVocabulary THEN throws BusinessException NOT_FOUND")
        void testCreateVocabularyRadicalNotFound() {
            CreateVocabularyRequest request = new CreateVocabularyRequest("休", "xiū", "xiu", "Hưu", "Nghỉ ngơi");
            request.setRadicalIds(Set.of(999));

            when(vocabularyRepository.findByHanziAndPinyinRaw("休", "xiu")).thenReturn(Optional.empty());
            when(radicalRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vocabularyService.createVocabulary(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                        assertThat(be.getMessage()).contains("Không tìm thấy bộ thủ với ID: 999");
                    });
        }
    }

    @Nested
    @DisplayName("updateVocabulary Tests")
    class UpdateVocabularyTests {

        @Test
        @DisplayName("GIVEN valid update request WHEN updateVocabulary THEN updates and returns DetailResponse")
        void testUpdateVocabularySuccess() {
            UpdateVocabularyRequest request = new UpdateVocabularyRequest("学", "xué", "xue", "Học", "Học tập, nghiên cứu");
            request.setRadicalIds(Set.of(39));

            Radical r39 = new Radical(39, "子", "zǐ", "Tử", "Con");

            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sampleVocab));
            when(vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")).thenReturn(Optional.of(sampleVocab));
            when(radicalRepository.findById(39)).thenReturn(Optional.of(r39));
            when(vocabularyRepository.save(any(Vocabulary.class))).thenAnswer(inv -> inv.getArgument(0));

            VocabularyDetailResponse response = vocabularyService.updateVocabulary(1L, request);

            assertThat(response.getVocabId()).isEqualTo(1L);
            assertThat(response.getMeaningVi()).isEqualTo("Học tập, nghiên cứu");
            assertThat(response.getRadicals()).hasSize(1);
            assertThat(response.getRadicals().get(0).getRadicalId()).isEqualTo(39);
            verify(vocabularyRepository).save(any(Vocabulary.class));
        }

        @Test
        @DisplayName("GIVEN null vocabId WHEN updateVocabulary THEN throws BusinessException VALIDATION_ERROR")
        void testUpdateVocabularyNullId() {
            UpdateVocabularyRequest request = new UpdateVocabularyRequest("学", "xué", "xue", "Học", "Học tập");

            assertThatThrownBy(() -> vocabularyService.updateVocabulary(null, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    });
        }

        @Test
        @DisplayName("GIVEN null request WHEN updateVocabulary THEN throws BusinessException VALIDATION_ERROR")
        void testUpdateVocabularyNullRequest() {
            assertThatThrownBy(() -> vocabularyService.updateVocabulary(1L, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    });
        }

        @Test
        @DisplayName("GIVEN nonexistent vocabId WHEN updateVocabulary THEN throws BusinessException NOT_FOUND")
        void testUpdateVocabularyNotFound() {
            UpdateVocabularyRequest request = new UpdateVocabularyRequest("学", "xué", "xue", "Học", "Học tập");
            when(vocabularyRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vocabularyService.updateVocabulary(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("GIVEN update request conflicting with another record WHEN updateVocabulary THEN throws BusinessException CONFLICT")
        void testUpdateVocabularyConflict() {
            UpdateVocabularyRequest request = new UpdateVocabularyRequest("休", "xiū", "xiu", "Hưu", "Nghỉ");

            Vocabulary otherVocab = new Vocabulary("休", "xiū", "xiu", "Hưu", "Nghỉ ngơi");
            otherVocab.setVocabId(2L);

            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sampleVocab));
            when(vocabularyRepository.findByHanziAndPinyinRaw("休", "xiu")).thenReturn(Optional.of(otherVocab));

            assertThatThrownBy(() -> vocabularyService.updateVocabulary(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });
        }

        @Test
        @DisplayName("GIVEN invalid radicalId WHEN updateVocabulary THEN throws BusinessException NOT_FOUND")
        void testUpdateVocabularyRadicalNotFound() {
            UpdateVocabularyRequest request = new UpdateVocabularyRequest("学", "xué", "xue", "Học", "Học");
            request.setRadicalIds(Set.of(999));

            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sampleVocab));
            when(vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")).thenReturn(Optional.of(sampleVocab));
            when(radicalRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vocabularyService.updateVocabulary(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("deleteVocabulary Tests")
    class DeleteVocabularyTests {

        @Test
        @DisplayName("GIVEN existing vocabId WHEN deleteVocabulary THEN deletes successfully")
        void testDeleteVocabularySuccess() {
            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sampleVocab));

            vocabularyService.deleteVocabulary(1L);

            verify(vocabularyRepository).delete(sampleVocab);
            verify(vocabularyRepository).flush();
        }

        @Test
        @DisplayName("GIVEN null vocabId WHEN deleteVocabulary THEN throws BusinessException VALIDATION_ERROR")
        void testDeleteVocabularyNullId() {
            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    });
        }

        @Test
        @DisplayName("GIVEN nonexistent vocabId WHEN deleteVocabulary THEN throws BusinessException NOT_FOUND")
        void testDeleteVocabularyNotFound() {
            when(vocabularyRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("GIVEN vocab referenced by LessonVocabulary WHEN deleteVocabulary THEN throws BusinessException CONFLICT")
        void testDeleteVocabulary_referencedByLesson_throwsConflict() {
            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sampleVocab));
            when(lessonVocabularyRepository.existsByVocabulary_VocabId(1L)).thenReturn(true);

            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Không thể xóa từ vựng đang được liên kết với bài học");
                    });
        }

        @Test
        @DisplayName("GIVEN vocab referenced by PersonalNote WHEN deleteVocabulary THEN throws BusinessException CONFLICT")
        void testDeleteVocabulary_referencedByPersonalNote_throwsConflict() {
            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sampleVocab));
            when(lessonVocabularyRepository.existsByVocabulary_VocabId(1L)).thenReturn(false);
            when(personalNoteRepository.existsByVocabulary_VocabId(1L)).thenReturn(true);

            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Không thể xóa từ vựng đang có ghi chú cá nhân");
                    });
        }

        @Test
        @DisplayName("GIVEN vocab referenced by CardProgress WHEN deleteVocabulary THEN throws BusinessException CONFLICT")
        void testDeleteVocabulary_referencedByCardProgress_throwsConflict() {
            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sampleVocab));
            when(lessonVocabularyRepository.existsByVocabulary_VocabId(1L)).thenReturn(false);
            when(personalNoteRepository.existsByVocabulary_VocabId(1L)).thenReturn(false);
            when(cardProgressRepository.existsByItemTypeAndItemId("VOCABULARY", 1L)).thenReturn(true);

            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Không thể xóa từ vựng đang có tiến trình học tập");
                    });
        }

        @Test
        @DisplayName("GIVEN vocab referenced by ReviewLog WHEN deleteVocabulary THEN throws BusinessException CONFLICT")
        void testDeleteVocabulary_referencedByReviewLog_throwsConflict() {
            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sampleVocab));
            when(lessonVocabularyRepository.existsByVocabulary_VocabId(1L)).thenReturn(false);
            when(personalNoteRepository.existsByVocabulary_VocabId(1L)).thenReturn(false);
            when(cardProgressRepository.existsByItemTypeAndItemId("VOCABULARY", 1L)).thenReturn(false);
            when(reviewLogRepository.existsByItemTypeAndItemId("VOCABULARY", 1L)).thenReturn(true);

            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Không thể xóa từ vựng đã có lịch sử ôn tập");
                    });
        }

        @Test
        @DisplayName("GIVEN vocab referenced by other records WHEN deleteVocabulary THEN throws BusinessException CONFLICT")
        void testDeleteVocabularyDataIntegrityViolation() {
            when(vocabularyRepository.findByIdWithLock(1L)).thenReturn(Optional.of(sampleVocab));
            when(lessonVocabularyRepository.existsByVocabulary_VocabId(1L)).thenReturn(false);
            when(personalNoteRepository.existsByVocabulary_VocabId(1L)).thenReturn(false);
            when(cardProgressRepository.existsByItemTypeAndItemId("VOCABULARY", 1L)).thenReturn(false);
            when(reviewLogRepository.existsByItemTypeAndItemId("VOCABULARY", 1L)).thenReturn(false);
            org.mockito.Mockito.doThrow(new DataIntegrityViolationException("Constraint violation"))
                    .when(vocabularyRepository).flush();

            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Không thể xóa từ vựng đang được liên kết");
                    });
        }
    }
}
