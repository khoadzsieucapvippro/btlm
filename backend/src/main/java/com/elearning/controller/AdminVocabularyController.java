package com.elearning.controller;

import com.elearning.dto.request.CreateVocabularyRequest;
import com.elearning.dto.request.UpdateVocabularyRequest;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.VocabularyDetailResponse;
import com.elearning.service.VocabularyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST Controller for Chinese Vocabulary catalog management (CRUD).
 * Base path: /api/v1/admin/vocabulary
 * Protected by Spring Security (Admin role required).
 * Endpoints:
 * - POST /api/v1/admin/vocabulary: Create a new vocabulary item (201 Created)
 * - PUT /api/v1/admin/vocabulary/{id}: Update an existing vocabulary item (200 OK)
 * - DELETE /api/v1/admin/vocabulary/{id}: Delete an existing vocabulary item (204 No Content)
 */
@RestController
@RequestMapping("/api/v1/admin/vocabulary")
public class AdminVocabularyController {

    private final VocabularyService vocabularyService;

    public AdminVocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VocabularyDetailResponse>> createVocabulary(
            @Valid @RequestBody CreateVocabularyRequest request) {
        VocabularyDetailResponse response = vocabularyService.createVocabulary(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo từ vựng thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VocabularyDetailResponse>> updateVocabulary(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateVocabularyRequest request) {
        VocabularyDetailResponse response = vocabularyService.updateVocabulary(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật từ vựng thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVocabulary(@PathVariable("id") Long id) {
        vocabularyService.deleteVocabulary(id);
        return ResponseEntity.noContent().build();
    }
}
