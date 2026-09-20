package com.elearning.repository;

import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.LessonVocabularyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for {@link LessonVocabulary} join entity.
 * Supports retrieving vocabulary associations for a lesson strictly sorted by display order (order_index ASC).
 */
@Repository
public interface LessonVocabularyRepository extends JpaRepository<LessonVocabulary, LessonVocabularyId> {

    /**
     * Finds all vocabulary associations for a given lesson entity, ordered by order_index ascending.
     *
     * @param lesson the parent lesson entity
     * @return List of LessonVocabulary associations sorted by order_index ASC
     */
    List<LessonVocabulary> findByLessonOrderByOrderIndexAsc(Lesson lesson);

    /**
     * Finds all vocabulary associations for a given lesson ID, ordered by order_index ascending.
     *
     * @param lessonId the parent lesson ID
     * @return List of LessonVocabulary associations sorted by order_index ASC
     */
    List<LessonVocabulary> findByLesson_LessonIdOrderByOrderIndexAsc(Long lessonId);

    /**
     * Checks if a vocabulary is already associated with a given lesson.
     *
     * @param lessonId the parent lesson ID
     * @param vocabId the vocabulary ID
     * @return true if association exists, false otherwise
     */
    boolean existsByLesson_LessonIdAndVocabulary_VocabId(Long lessonId, Long vocabId);

    /**
     * Counts the number of vocabulary items in a given lesson.
     *
     * @param lessonId the parent lesson ID
     * @return total count of vocabularies in the lesson
     */
    long countByLesson_LessonId(Long lessonId);

    /**
     * Finds all vocabulary associations for a given lesson ID with the vocabulary entity eagerly fetched,
     * strictly ordered by order_index ascending.
     * Prevents N+1 when mapping lesson vocabularies.
     *
     * @param lessonId the parent lesson ID
     * @return List of LessonVocabulary associations with Vocabulary loaded, sorted by order_index ASC
     */
    @Query("SELECT lv FROM LessonVocabulary lv JOIN FETCH lv.vocabulary WHERE lv.lesson.lessonId = :lessonId ORDER BY lv.orderIndex ASC")
    List<LessonVocabulary> findByLessonIdWithVocabularyOrderAsc(@Param("lessonId") Long lessonId);

    /**
     * Finds new-card candidate vocabulary items for a given lesson that have not yet been studied
     * (no existing CardProgress) by the specified user, strictly ordered by order_index ascending.
     * Enforces lesson Approved status directly in the query.
     *
     * @param lessonId the parent lesson ID
     * @param user the current authenticated learner profile
     * @param pageable pagination parameters (limiting candidates to remaining quota)
     * @return List of LessonVocabulary candidate associations with Vocabulary loaded, sorted by order_index ASC
     */
    @Query("SELECT lv FROM LessonVocabulary lv " +
           "JOIN FETCH lv.vocabulary v " +
           "WHERE lv.lesson.lessonId = :lessonId " +
           "  AND lv.lesson.status = 'Approved' " +
           "  AND NOT EXISTS (" +
           "      SELECT 1 FROM CardProgress cp " +
           "      WHERE cp.user = :user " +
           "        AND cp.itemType = 'VOCABULARY' " +
           "        AND cp.itemId = v.vocabId" +
           "  ) " +
           "ORDER BY lv.orderIndex ASC")
    List<LessonVocabulary> findNewCardCandidates(
            @Param("lessonId") Long lessonId,
            @Param("user") com.elearning.entity.UserProfile user,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Counts vocabulary items grouped by lesson ID for a given collection of lesson IDs.
     * Prevents N+1 queries when fetching summaries for multiple lessons (BE-PERF-002).
     *
     * @param lessonIds collection of parent lesson IDs
     * @return List of Object arrays where row[0] is Long lessonId and row[1] is Long vocabularyCount
     */
    @Query("SELECT lv.lesson.lessonId, COUNT(lv) FROM LessonVocabulary lv WHERE lv.lesson.lessonId IN :lessonIds GROUP BY lv.lesson.lessonId")
    List<Object[]> countVocabulariesByLessonIds(@Param("lessonIds") java.util.Collection<Long> lessonIds);

    /**
     * Checks whether a given vocabulary belongs to at least one lesson with status 'Approved'.
     * Used at mutation boundary to enforce new-card introduction eligibility (R2.1).
     *
     * @param vocabId the vocabulary ID
     * @return true if vocabulary is in at least one approved lesson, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(lv) > 0 THEN true ELSE false END FROM LessonVocabulary lv " +
           "WHERE lv.vocabulary.vocabId = :vocabId AND lv.lesson.status = 'Approved'")
    boolean existsApprovedLessonForVocabulary(@Param("vocabId") Long vocabId);

    /**
     * Checks whether a given vocabulary is linked to any lesson in the system (R3.1).
     * Used during vocabulary deletion to prevent breaking curriculum relationships.
     *
     * @param vocabId the vocabulary ID
     * @return true if vocabulary is referenced in any lesson, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(lv) > 0 THEN true ELSE false END FROM LessonVocabulary lv WHERE lv.vocabulary.vocabId = :vocabId")
    boolean existsByVocabulary_VocabId(@Param("vocabId") Long vocabId);
}

