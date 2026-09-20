package com.elearning.controller;

import com.elearning.dto.request.PersonalNoteRequest;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.PageResponse;
import com.elearning.dto.response.PersonalNoteResponse;
import com.elearning.service.PersonalNoteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Personal Study Notes on Vocabulary items.
 * Base paths:
 * - GET    /api/v1/vocabularies/{vocabId}/notes : Retrieve personal notes on a vocabulary item (paginated)
 * - POST   /api/v1/vocabularies/{vocabId}/notes : Create a new personal note (201 Created)
 * - PUT    /api/v1/notes/{noteId}               : Update an existing note content (200 OK)
 * - DELETE /api/v1/notes/{noteId}               : Delete an existing note (200 OK)
 *
 * Enforces thin-controller architecture: strictly delegates to {@link PersonalNoteService}.
 * Protected by Spring Security: Requires authenticated learner.
 */
@RestController
@RequestMapping("/api/v1")
public class PersonalNoteController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final PersonalNoteService personalNoteService;

    public PersonalNoteController(PersonalNoteService personalNoteService) {
        this.personalNoteService = personalNoteService;
    }

    /**
     * Retrieves personal study notes created by the authenticated user for a specific vocabulary word.
     * Supports pagination via query parameters: page (0-indexed) and size.
     *
     * @param vocabId ID of the vocabulary word
     * @param page page number (0-indexed, default 0)
     * @param size page size (default 20, max 100)
     * @return ApiResponse wrapping PageResponse of PersonalNoteResponse
     */
    @GetMapping("/vocabularies/{vocabId}/notes")
    public ResponseEntity<ApiResponse<PageResponse<PersonalNoteResponse>>> getNotesByVocabulary(
            @PathVariable("vocabId") Long vocabId,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size) {
        // Enforce server-side bounds
        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveSize = DEFAULT_PAGE_SIZE;
        if (size != null) {
            effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        }
        Pageable pageable = PageRequest.of(effectivePage, effectiveSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<PersonalNoteResponse> notes = personalNoteService.getNotesByVocabulary(vocabId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    /**
     * Creates a new personal study note on a vocabulary word for the authenticated user.
     *
     * @param vocabId ID of the vocabulary word
     * @param request Validated request payload containing content (max 500 characters)
     * @return 201 Created ApiResponse wrapping PersonalNoteResponse
     */
    @PostMapping("/vocabularies/{vocabId}/notes")
    public ResponseEntity<ApiResponse<PersonalNoteResponse>> createNote(
            @PathVariable("vocabId") Long vocabId,
            @Valid @RequestBody PersonalNoteRequest request) {
        PersonalNoteResponse response = personalNoteService.createNote(vocabId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo ghi chú thành công", response));
    }

    /**
     * Updates an existing personal note owned by the authenticated user.
     *
     * @param noteId  ID of the note to update
     * @param request Validated request payload containing updated content (max 500 characters)
     * @return 200 OK ApiResponse wrapping updated PersonalNoteResponse
     */
    @PutMapping("/notes/{noteId}")
    public ResponseEntity<ApiResponse<PersonalNoteResponse>> updateNote(
            @PathVariable("noteId") Long noteId,
            @Valid @RequestBody PersonalNoteRequest request) {
        PersonalNoteResponse response = personalNoteService.updateNote(noteId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ghi chú thành công", response));
    }

    /**
     * Deletes an existing personal note owned by the authenticated user.
     *
     * @param noteId ID of the note to delete
     * @return 200 OK ApiResponse with success message
     */
    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @PathVariable("noteId") Long noteId) {
        personalNoteService.deleteNote(noteId);
        return ResponseEntity.ok(ApiResponse.success("Xóa ghi chú thành công", null));
    }
}
