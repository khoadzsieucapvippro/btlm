package com.elearning.service;

import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.dto.response.StudyStatsResponse;

import java.util.List;

/**
 * Service interface managing Spaced Repetition System (SRS) review sessions,
 * card progress lifecycle, and study statistics for authenticated learners.
 */
public interface SrsService {

    /**
     * Retrieves flashcards due for review for the current authenticated user.
     * Applies daily limits from user settings and filters by optional itemType.
     *
     * @param itemType optional filter ("VOCABULARY" or "RADICAL", or null for all)
     * @param limit    maximum number of cards to retrieve (null for default daily quota)
     * @return list of {@link DueCardResponse} due for review
     */
    List<DueCardResponse> getDueCards(String itemType, Integer limit);

    /**
     * Retrieves new-card candidates for an approved lesson for the current authenticated learner.
     * Candidates are unstudied vocabulary items belonging to the specified approved lesson,
     * ordered strictly by order_index ASC, constrained by remaining daily new-card quota (R1-DEC-02, R1-DEC-04).
     * Candidate retrieval is idempotent and creates zero CardProgress records (preview vs introduction).
     *
     * @param lessonId the approved lesson ID
     * @param limit optional maximum number of candidate cards to retrieve
     * @return list of {@link DueCardResponse} candidate cards with isNew=true
     */
    List<DueCardResponse> getNewCardCandidates(Long lessonId, Integer limit);

    /**
     * Processes a flashcard review rating submission from the authenticated learner.
     * Delegates mathematical calculation to {@link com.elearning.service.srs.SrsCalculator},
     * updates {@link com.elearning.entity.CardProgress}, and appends an immutable
     * {@link com.elearning.entity.ReviewLog} within an atomic transaction.
     *
     * @param request review rating submission DTO
     * @return updated {@link DueCardResponse} with new interval and progress state
     */
    DueCardResponse reviewCard(ReviewCardRequest request);

    /**
     * Calculates current study and review session statistics for the authenticated learner.
     *
     * @return {@link StudyStatsResponse} with due cards, reviews completed today, and limits
     */
    StudyStatsResponse getStudyStats();
}
