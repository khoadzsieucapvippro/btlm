package com.elearning.controller;

import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.dto.response.StudyStatsResponse;
import com.elearning.service.SrsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Spaced Repetition System (SRS) review sessions.
 * Base path: /api/v1/srs
 * Protected by Spring Security: Requires authenticated learner or admin user.
 * Enforces thin-controller architecture; delegates due card queries,
 * review rating calculation, and study statistics retrieval to {@link SrsService}.
 */
@RestController
@RequestMapping("/api/v1/srs")
public class SrsController {

    private final SrsService srsService;

    public SrsController(SrsService srsService) {
        this.srsService = srsService;
    }

    /**
     * Retrieves flashcards due for review for the authenticated learner.
     *
     * @param itemType optional card type filter ("VOCABULARY" or "RADICAL")
     * @param limit    optional max cards count (constrained by daily review limits)
     * @return ApiResponse wrapping list of DueCardResponse
     */
    @GetMapping("/due")
    public ResponseEntity<ApiResponse<List<DueCardResponse>>> getDueCards(
            @RequestParam(name = "itemType", required = false) String itemType,
            @RequestParam(name = "limit", required = false) Integer limit) {
        List<DueCardResponse> dueCards = srsService.getDueCards(itemType, limit);
        return ResponseEntity.ok(ApiResponse.success(dueCards));
    }

    /**
     * Retrieves new-card candidates for a specific approved lesson for the authenticated learner.
     * Enforces lesson approval status, order_index ASC ordering, and daily new-card quota (R1-DEC-02, R1-DEC-04).
     *
     * @param lessonId the approved lesson ID
     * @param limit    optional max cards count
     * @return ApiResponse wrapping list of candidate DueCardResponse with isNew=true
     */
    @GetMapping("/new-cards")
    public ResponseEntity<ApiResponse<List<DueCardResponse>>> getNewCardCandidates(
            @RequestParam(name = "lessonId") Long lessonId,
            @RequestParam(name = "limit", required = false) Integer limit) {
        List<DueCardResponse> candidates = srsService.getNewCardCandidates(lessonId, limit);
        return ResponseEntity.ok(ApiResponse.success(candidates));
    }

    /**
     * RESTful sub-resource alias to retrieve new-card candidates for an approved lesson.
     *
     * @param lessonId the approved lesson ID
     * @param limit    optional max cards count
     * @return ApiResponse wrapping list of candidate DueCardResponse with isNew=true
     */
    @GetMapping("/lessons/{lessonId}/new-cards")
    public ResponseEntity<ApiResponse<List<DueCardResponse>>> getNewCardCandidatesForLesson(
            @org.springframework.web.bind.annotation.PathVariable("lessonId") Long lessonId,
            @RequestParam(name = "limit", required = false) Integer limit) {
        List<DueCardResponse> candidates = srsService.getNewCardCandidates(lessonId, limit);
        return ResponseEntity.ok(ApiResponse.success(candidates));
    }

    /**
     * Submits a flashcard review rating from the authenticated learner.
     * Delegates SM-2 mathematical calculation and atomic audit logging to SrsService.
     *
     * @param request validated review request payload
     * @return ApiResponse wrapping updated DueCardResponse with new interval and progress state
     */
    @PostMapping("/review")
    public ResponseEntity<ApiResponse<DueCardResponse>> reviewCard(
            @Valid @RequestBody ReviewCardRequest request) {
        DueCardResponse response = srsService.reviewCard(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves current study and review session statistics for the authenticated learner.
     *
     * @return ApiResponse wrapping StudyStatsResponse
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StudyStatsResponse>> getStudyStats() {
        StudyStatsResponse stats = srsService.getStudyStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
