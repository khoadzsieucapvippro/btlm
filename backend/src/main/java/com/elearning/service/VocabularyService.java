package com.elearning.service;

import com.elearning.dto.request.VocabularySearchCriteria;
import com.elearning.dto.response.VocabularyDetailResponse;
import com.elearning.dto.response.VocabularyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Vocabulary Catalog domain.
 * Supports paginated listing, multi-criteria dynamic search, and detailed item lookup.
 * Conforms to .agents/API.md section 2.2 and zero-entity-leak rules.
 */
public interface VocabularyService {

    /**
     * Retrieves a paginated list of all vocabularies.
     * Default canonical ordering is by vocabId ascending.
     *
     * @param pageable pagination and sorting parameters
     * @return Page of {@link VocabularyResponse}
     */
    Page<VocabularyResponse> getAllVocabularies(Pageable pageable);

    /**
     * Searches vocabularies based on the provided dynamic criteria.
     * Supports filtering by hanzi, pinyin (accented), pinyinRaw (toneless),
     * radicalId (constituent radical), and unified keyword search.
     *
     * @param criteria search criteria filter object
     * @param pageable pagination and sorting parameters
     * @return Page of matching {@link VocabularyResponse}
     */
    Page<VocabularyResponse> searchVocabularies(VocabularySearchCriteria criteria, Pageable pageable);

    /**
     * Retrieves the details of a single vocabulary by its primary ID,
     * including its constituent Kangxi radicals.
     *
     * @param vocabId vocabulary primary ID
     * @return {@link VocabularyDetailResponse}
     * @throws com.elearning.exception.BusinessException if ID is null or not found
     */
    VocabularyDetailResponse getVocabularyById(Long vocabId);

    /**
     * Creates a new vocabulary record and links its constituent radicals (Admin-only).
     *
     * @param request creation request data with validation
     * @return {@link VocabularyDetailResponse} containing created record with linked radicals
     * @throws com.elearning.exception.BusinessException if hanzi+pinyinRaw already exists (CONFLICT)
     *                                                  or any radical ID not found (NOT_FOUND)
     */
    VocabularyDetailResponse createVocabulary(com.elearning.dto.request.CreateVocabularyRequest request);

    /**
     * Updates an existing vocabulary record and updates linked radicals (Admin-only).
     *
     * @param vocabId vocabulary ID to update
     * @param request update request data with validation
     * @return {@link VocabularyDetailResponse} containing updated record
     * @throws com.elearning.exception.BusinessException if not found (NOT_FOUND)
     *                                                  or hanzi+pinyinRaw conflicts (CONFLICT)
     */
    VocabularyDetailResponse updateVocabulary(Long vocabId, com.elearning.dto.request.UpdateVocabularyRequest request);

    /**
     * Deletes a vocabulary record by its ID (Admin-only).
     *
     * @param vocabId vocabulary ID to delete
     * @throws com.elearning.exception.BusinessException if not found (NOT_FOUND)
     *                                                  or referenced by existing lessons (CONFLICT)
     */
    void deleteVocabulary(Long vocabId);
}
