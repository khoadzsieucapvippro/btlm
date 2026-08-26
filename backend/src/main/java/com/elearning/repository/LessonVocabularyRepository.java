package com.elearning.repository;

import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.LessonVocabularyId;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
