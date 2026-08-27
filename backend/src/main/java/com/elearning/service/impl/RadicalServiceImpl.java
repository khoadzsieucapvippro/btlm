package com.elearning.service.impl;

import com.elearning.common.ErrorCode;
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
}
