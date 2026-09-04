package com.elearning.service;

import com.elearning.dto.request.PersonalNoteRequest;
import com.elearning.dto.response.PageResponse;
import com.elearning.dto.response.PersonalNoteResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service contract for managing personal study notes on vocabulary items.
 * Enforces ownership protection (learners can only access/mutate their own notes),
 * character limit (content <= 500 characters), and supports unlimited notes per vocabulary.
 */
public interface PersonalNoteService {

    /**
     * Retrieves personal study notes created by the current authenticated user for a specific vocabulary word.
     * Supports pagination to enforce bounded resource consumption (OWASP API4:2023).
     *
     * @param vocabId  ID of the vocabulary word
     * @param pageable pagination parameters (page, size, sort)
     * @return Page of personal note responses sorted by createdAt descending
     */
    PageResponse<PersonalNoteResponse> getNotesByVocabulary(Long vocabId, Pageable pageable);

    /**
     * Creates a new personal note on a vocabulary word for the current authenticated user.
     *
     * @param vocabId ID of the vocabulary word
     * @param request Payload containing note content (max 500 characters)
     * @return Created personal note response
     */
    PersonalNoteResponse createNote(Long vocabId, PersonalNoteRequest request);

    /**
     * Updates an existing personal note owned by the current authenticated user.
     *
     * @param noteId ID of the note to update
     * @param request Payload containing updated note content (max 500 characters)
     * @return Updated personal note response
     */
    PersonalNoteResponse updateNote(Long noteId, PersonalNoteRequest request);

    /**
     * Deletes an existing personal note owned by the current authenticated user.
     *
     * @param noteId ID of the note to delete
     */
    void deleteNote(Long noteId);
}
