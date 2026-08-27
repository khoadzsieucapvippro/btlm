package com.elearning.service;

import com.elearning.dto.response.RadicalDetailResponse;
import com.elearning.dto.response.RadicalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service managing lookup and catalog operations for the 214 Kangxi Radicals.
 */
public interface RadicalService {

    /**
     * Retrieves Kangxi radicals with pagination.
     *
     * @param pageable pagination parameters
     * @return Page of RadicalResponse
     */
    Page<RadicalResponse> getAllRadicals(Pageable pageable);

    /**
     * Retrieves all 214 Kangxi radicals ordered canonically by radicalId ascending.
     *
     * @return List of all RadicalResponse
     */
    List<RadicalResponse> getAllRadicals();

    /**
     * Retrieves detailed information of a radical by its primary key ID.
     *
     * @param radicalId ID of the radical
     * @return RadicalDetailResponse containing radical metadata
     * @throws com.elearning.exception.BusinessException with NOT_FOUND if radical does not exist
     */
    RadicalDetailResponse getRadicalById(Integer radicalId);

    /**
     * Retrieves detailed information of a radical by its Chinese character symbol.
     *
     * @param character the radical Chinese character symbol
     * @return RadicalDetailResponse containing radical metadata
     * @throws com.elearning.exception.BusinessException with NOT_FOUND if radical does not exist
     */
    RadicalDetailResponse getRadicalByCharacter(String character);
}
