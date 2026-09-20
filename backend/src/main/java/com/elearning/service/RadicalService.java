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

    /**
     * Creates a new Kangxi radical (Admin operation).
     *
     * @param request creation payload
     * @return RadicalDetailResponse of created radical
     * @throws com.elearning.exception.BusinessException with CONFLICT if character already exists
     */
    RadicalDetailResponse createRadical(com.elearning.dto.request.CreateRadicalRequest request);

    /**
     * Updates an existing Kangxi radical (Admin operation).
     *
     * @param radicalId ID of radical to update
     * @param request update payload
     * @return RadicalDetailResponse of updated radical
     * @throws com.elearning.exception.BusinessException with NOT_FOUND if radical does not exist,
     *                                                   or CONFLICT if character belongs to another radical
     */
    RadicalDetailResponse updateRadical(Integer radicalId, com.elearning.dto.request.UpdateRadicalRequest request);

    /**
     * Deletes an existing Kangxi radical (Admin operation).
     *
     * @param radicalId ID of radical to delete
     * @throws com.elearning.exception.BusinessException with NOT_FOUND if radical does not exist,
     *                                                   or CONFLICT if radical is linked to vocabularies
     */
    void deleteRadical(Integer radicalId);
}
