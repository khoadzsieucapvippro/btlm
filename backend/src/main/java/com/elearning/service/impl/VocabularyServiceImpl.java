package com.elearning.service.impl;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.CreateVocabularyRequest;
import com.elearning.dto.request.UpdateVocabularyRequest;
import com.elearning.dto.request.VocabularySearchCriteria;
import com.elearning.dto.response.VocabularyDetailResponse;
import com.elearning.dto.response.VocabularyResponse;
import com.elearning.entity.Radical;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.VocabularyService;
import com.elearning.specification.VocabularySpecification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link VocabularyService}.
 * Read-only transaction at class level guarantees transactional safety.
 * Handles dynamic search via JPA {@link Specification}, Admin CRUD operations,
 * and encapsulates Entities to DTOs.
 */
@Service
@Transactional(readOnly = true)
public class VocabularyServiceImpl implements VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final RadicalRepository radicalRepository;
    private final com.elearning.repository.LessonVocabularyRepository lessonVocabularyRepository;
    private final com.elearning.repository.PersonalNoteRepository personalNoteRepository;
    private final com.elearning.repository.CardProgressRepository cardProgressRepository;
    private final com.elearning.repository.ReviewLogRepository reviewLogRepository;

    public VocabularyServiceImpl(VocabularyRepository vocabularyRepository,
                                 RadicalRepository radicalRepository,
                                 com.elearning.repository.LessonVocabularyRepository lessonVocabularyRepository,
                                 com.elearning.repository.PersonalNoteRepository personalNoteRepository,
                                 com.elearning.repository.CardProgressRepository cardProgressRepository,
                                 com.elearning.repository.ReviewLogRepository reviewLogRepository) {
        this.vocabularyRepository = vocabularyRepository;
        this.radicalRepository = radicalRepository;
        this.lessonVocabularyRepository = lessonVocabularyRepository;
        this.personalNoteRepository = personalNoteRepository;
        this.cardProgressRepository = cardProgressRepository;
        this.reviewLogRepository = reviewLogRepository;
    }

    @Override
    public Page<VocabularyResponse> getAllVocabularies(Pageable pageable) {
        Pageable effectivePageable = resolveEffectivePageable(pageable);
        return vocabularyRepository.findAll(effectivePageable)
                .map(VocabularyResponse::fromEntity);
    }

    @Override
    public Page<VocabularyResponse> searchVocabularies(VocabularySearchCriteria criteria, Pageable pageable) {
        if (criteria == null || criteria.isEmpty()) {
            return getAllVocabularies(pageable);
        }

        Pageable effectivePageable = resolveEffectivePageable(pageable);
        Specification<Vocabulary> spec = VocabularySpecification.fromCriteria(criteria);

        return vocabularyRepository.findAll(spec, effectivePageable)
                .map(VocabularyResponse::fromEntity);
    }

    @Override
    public VocabularyDetailResponse getVocabularyById(Long vocabId) {
        if (vocabId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID từ vựng không được để trống");
        }

        Vocabulary vocabulary = vocabularyRepository.findById(vocabId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng với ID: " + vocabId));

        return VocabularyDetailResponse.fromEntity(vocabulary);
    }

    @Override
    @Transactional
    public VocabularyDetailResponse createVocabulary(CreateVocabularyRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Yêu cầu tạo từ vựng không được để trống");
        }

        String rawPinyin = request.getPinyinRaw() != null && !request.getPinyinRaw().isBlank()
                ? ExcelParserServiceImpl.toPinyinRaw(request.getPinyinRaw())
                : ExcelParserServiceImpl.toPinyinRaw(request.getPinyin());

        String hanzi = request.getHanzi() != null ? request.getHanzi().trim() : "";

        vocabularyRepository.findByHanziAndPinyinRaw(hanzi, rawPinyin)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.CONFLICT,
                            "Từ vựng với chữ Hán '" + hanzi + "' và pinyin '" + rawPinyin + "' đã tồn tại");
                });

        Vocabulary vocabulary = new Vocabulary();
        vocabulary.setHanzi(hanzi);
        vocabulary.setPinyin(request.getPinyin() != null ? request.getPinyin().trim() : "");
        vocabulary.setPinyinRaw(rawPinyin);
        vocabulary.setMeaningHanViet(request.getMeaningHanViet() != null ? request.getMeaningHanViet().trim() : "");
        vocabulary.setMeaningVi(request.getMeaningVi() != null ? request.getMeaningVi().trim() : "");
        vocabulary.setAudioUrl(request.getAudioUrl());
        vocabulary.setVideoWritingUrl(request.getVideoWritingUrl());
        vocabulary.setExampleSentence(request.getExampleSentence());
        vocabulary.setExampleTranslation(request.getExampleTranslation());

        if (request.getRadicalIds() != null && !request.getRadicalIds().isEmpty()) {
            for (Integer radicalId : request.getRadicalIds()) {
                Radical radical = radicalRepository.findById(radicalId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                                "Không tìm thấy bộ thủ với ID: " + radicalId));
                vocabulary.addRadical(radical);
            }
        }

        Vocabulary saved = vocabularyRepository.save(vocabulary);
        return VocabularyDetailResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public VocabularyDetailResponse updateVocabulary(Long vocabId, UpdateVocabularyRequest request) {
        if (vocabId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID từ vựng không được để trống");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Yêu cầu cập nhật từ vựng không được để trống");
        }

        Vocabulary vocabulary = vocabularyRepository.findByIdWithLock(vocabId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng với ID: " + vocabId));

        String rawPinyin = request.getPinyinRaw() != null && !request.getPinyinRaw().isBlank()
                ? ExcelParserServiceImpl.toPinyinRaw(request.getPinyinRaw())
                : ExcelParserServiceImpl.toPinyinRaw(request.getPinyin());

        String hanzi = request.getHanzi() != null ? request.getHanzi().trim() : "";

        vocabularyRepository.findByHanziAndPinyinRaw(hanzi, rawPinyin)
                .ifPresent(existing -> {
                    if (!existing.getVocabId().equals(vocabId)) {
                        throw new BusinessException(ErrorCode.CONFLICT,
                                "Từ vựng với chữ Hán '" + hanzi + "' và pinyin '" + rawPinyin + "' đã tồn tại");
                    }
                });

        vocabulary.setHanzi(hanzi);
        vocabulary.setPinyin(request.getPinyin() != null ? request.getPinyin().trim() : "");
        vocabulary.setPinyinRaw(rawPinyin);
        vocabulary.setMeaningHanViet(request.getMeaningHanViet() != null ? request.getMeaningHanViet().trim() : "");
        vocabulary.setMeaningVi(request.getMeaningVi() != null ? request.getMeaningVi().trim() : "");
        vocabulary.setAudioUrl(request.getAudioUrl());
        vocabulary.setVideoWritingUrl(request.getVideoWritingUrl());
        vocabulary.setExampleSentence(request.getExampleSentence());
        vocabulary.setExampleTranslation(request.getExampleTranslation());

        // Update radical relationships: clear current and re-add
        Set<Radical> currentRadicals = new HashSet<>(vocabulary.getRadicals());
        for (Radical r : currentRadicals) {
            vocabulary.removeRadical(r);
        }

        if (request.getRadicalIds() != null && !request.getRadicalIds().isEmpty()) {
            for (Integer radicalId : request.getRadicalIds()) {
                Radical radical = radicalRepository.findById(radicalId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                                "Không tìm thấy bộ thủ với ID: " + radicalId));
                vocabulary.addRadical(radical);
            }
        }

        Vocabulary updated = vocabularyRepository.save(vocabulary);
        return VocabularyDetailResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteVocabulary(Long vocabId) {
        if (vocabId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID từ vựng không được để trống");
        }

        Vocabulary vocabulary = vocabularyRepository.findByIdWithLock(vocabId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng với ID: " + vocabId));

        // 1. Guard against breaking Curriculum / Lesson relationships (R3.1)
        if (lessonVocabularyRepository != null && lessonVocabularyRepository.existsByVocabulary_VocabId(vocabId)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Không thể xóa từ vựng đang được liên kết với bài học");
        }

        // 2. Guard against destroying learner personal notes (R3.1)
        if (personalNoteRepository != null && personalNoteRepository.existsByVocabulary_VocabId(vocabId)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Không thể xóa từ vựng đang có ghi chú cá nhân của người học");
        }

        // 3. Guard against orphaning mutable SRS CardProgress (R3.1)
        if (cardProgressRepository != null && cardProgressRepository.existsByItemTypeAndItemId("VOCABULARY", vocabId)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Không thể xóa từ vựng đang có tiến trình học tập (CardProgress) của người học");
        }

        // 4. Guard against orphaning immutable SRS ReviewLog history (R3.1)
        if (reviewLogRepository != null && reviewLogRepository.existsByItemTypeAndItemId("VOCABULARY", vocabId)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Không thể xóa từ vựng đã có lịch sử ôn tập (ReviewLog) trong hệ thống");
        }

        try {
            vocabularyRepository.delete(vocabulary);
            vocabularyRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Không thể xóa từ vựng đang được liên kết với bài học hoặc dữ liệu khác trong hệ thống");
        }
    }

    private Pageable resolveEffectivePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by("vocabId").ascending());
        }
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("vocabId").ascending());
        }
        return pageable;
    }
}
