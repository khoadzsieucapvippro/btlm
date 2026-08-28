package com.elearning.service.impl;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.CreateRadicalRequest;
import com.elearning.dto.request.UpdateRadicalRequest;
import com.elearning.dto.response.RadicalDetailResponse;
import com.elearning.dto.response.RadicalResponse;
import com.elearning.entity.Radical;
import com.elearning.exception.BusinessException;
import com.elearning.repository.RadicalRepository;
import com.elearning.service.RadicalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link RadicalService} managing catalog and lookup operations for 214 Kangxi radicals.
 * Fully decoupled from Vocabulary domain (vocabulary operations belong to Module 4B).
 */
@Service
@Transactional(readOnly = true)
public class RadicalServiceImpl implements RadicalService {

    private final RadicalRepository radicalRepository;

    public RadicalServiceImpl(RadicalRepository radicalRepository) {
        this.radicalRepository = radicalRepository;
    }

    @Override
    public Page<RadicalResponse> getAllRadicals(Pageable pageable) {
        Pageable effectivePageable = pageable;
        if (pageable != null && pageable.getSort().isUnsorted()) {
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("radicalId").ascending());
        }
        return radicalRepository.findAll(effectivePageable != null ? effectivePageable : PageRequest.of(0, 20, Sort.by("radicalId").ascending()))
                .map(RadicalResponse::fromEntity);
    }

    @Override
    public List<RadicalResponse> getAllRadicals() {
        return radicalRepository.findAll(Sort.by(Sort.Direction.ASC, "radicalId"))
                .stream()
                .map(RadicalResponse::fromEntity)
                .toList();
    }

    @Override
    public RadicalDetailResponse getRadicalById(Integer radicalId) {
        if (radicalId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID bộ thủ không được để trống");
        }
        Radical radical = radicalRepository.findById(radicalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bộ thủ với ID: " + radicalId));

        return RadicalDetailResponse.fromEntity(radical);
    }

    @Override
    public RadicalDetailResponse getRadicalByCharacter(String character) {
        if (character == null || character.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Ký tự bộ thủ không được để trống");
        }
        Radical radical = radicalRepository.findByCharacter(character.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bộ thủ với ký tự: " + character));

        return RadicalDetailResponse.fromEntity(radical);
    }

    @Override
    @Transactional
    public RadicalDetailResponse createRadical(CreateRadicalRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu tạo bộ thủ không được để trống");
        }
        String character = request.getCharacter() != null ? request.getCharacter().trim() : "";
        if (character.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Ký tự bộ thủ không được để trống");
        }
        if (radicalRepository.existsByCharacter(character)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Bộ thủ với ký tự '" + character + "' đã tồn tại");
        }

        Radical radical = new Radical();
        radical.setCharacter(character);
        radical.setPinyin(request.getPinyin() != null ? request.getPinyin().trim() : "");
        radical.setMeaningHanViet(request.getMeaningHanViet() != null ? request.getMeaningHanViet().trim() : "");
        radical.setMeaningVi(request.getMeaningVi() != null ? request.getMeaningVi().trim() : "");
        radical.setAudioUrl(request.getAudioUrl());
        radical.setVideoWritingUrl(request.getVideoWritingUrl());

        Radical saved = radicalRepository.save(radical);
        return RadicalDetailResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public RadicalDetailResponse updateRadical(Integer radicalId, UpdateRadicalRequest request) {
        if (radicalId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID bộ thủ không được để trống");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu cập nhật bộ thủ không được để trống");
        }
        Radical radical = radicalRepository.findById(radicalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bộ thủ với ID: " + radicalId));

        String character = request.getCharacter() != null ? request.getCharacter().trim() : "";
        if (character.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Ký tự bộ thủ không được để trống");
        }

        if (!character.equals(radical.getCharacter())) {
            Optional<Radical> existing = radicalRepository.findByCharacter(character);
            if (existing.isPresent() && !existing.get().getRadicalId().equals(radicalId)) {
                throw new BusinessException(ErrorCode.CONFLICT, "Bộ thủ với ký tự '" + character + "' đã tồn tại");
            }
        }

        radical.setCharacter(character);
        radical.setPinyin(request.getPinyin() != null ? request.getPinyin().trim() : "");
        radical.setMeaningHanViet(request.getMeaningHanViet() != null ? request.getMeaningHanViet().trim() : "");
        radical.setMeaningVi(request.getMeaningVi() != null ? request.getMeaningVi().trim() : "");
        radical.setAudioUrl(request.getAudioUrl());
        radical.setVideoWritingUrl(request.getVideoWritingUrl());

        Radical updated = radicalRepository.save(radical);
        return RadicalDetailResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteRadical(Integer radicalId) {
        if (radicalId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID bộ thủ không được để trống");
        }
        Radical radical = radicalRepository.findById(radicalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bộ thủ với ID: " + radicalId));

        if (radical.getVocabularies() != null && !radical.getVocabularies().isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Không thể xóa bộ thủ đang được liên kết với từ vựng");
        }

        try {
            radicalRepository.delete(radical);
            radicalRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "Không thể xóa bộ thủ do có dữ liệu liên kết ràng buộc");
        }
    }
}
