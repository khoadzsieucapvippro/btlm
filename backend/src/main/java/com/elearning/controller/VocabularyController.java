package com.elearning.controller;

import com.elearning.dto.request.VocabularySearchCriteria;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.PageResponse;
import com.elearning.dto.response.VocabularyDetailResponse;
import com.elearning.dto.response.VocabularyResponse;
import com.elearning.service.VocabularyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST Controller for Chinese Vocabulary catalog lookup.
 * Base path: /api/v1/vocabulary
 * Publicly accessible without authentication.
 * Endpoints:
 * - GET /api/v1/vocabulary: Paginated list of vocabulary with multi-criteria search
 * - GET /api/v1/vocabulary/{id}: Detail of a vocabulary item with constituent Kangxi radicals
 */
@RestController
@RequestMapping("/api/v1/vocabulary")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VocabularyResponse>>> getVocabularies(
            @RequestParam(value = "search", required = false) String search,
            Pageable pageable) {
        VocabularySearchCriteria criteria = VocabularySearchCriteria.byKeyword(search);
        Page<VocabularyResponse> page = vocabularyService.searchVocabularies(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VocabularyDetailResponse>> getVocabularyById(@PathVariable("id") Long id) {
        VocabularyDetailResponse response = vocabularyService.getVocabularyById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
