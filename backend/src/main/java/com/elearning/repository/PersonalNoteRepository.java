package com.elearning.repository;

import com.elearning.entity.PersonalNote;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalNoteRepository extends JpaRepository<PersonalNote, Long> {

    List<PersonalNote> findByUserAndVocabularyOrderByCreatedAtDesc(UserProfile user, Vocabulary vocabulary);

    /**
     * Paginated retrieval of personal notes for a specific user and vocabulary.
     * Enforces bounded result sets to prevent unbounded resource consumption (OWASP API4:2023).
     *
     * @param user     the authenticated user
     * @param vocabulary the vocabulary item
     * @param pageable pagination parameters (page, size, sort)
     * @return Page of PersonalNote entities
     */
    Page<PersonalNote> findByUserAndVocabulary(UserProfile user, Vocabulary vocabulary, Pageable pageable);

    Optional<PersonalNote> findByNoteIdAndUser(Long noteId, UserProfile user);

    List<PersonalNote> findByUserOrderByCreatedAtDesc(UserProfile user);

    /**
     * Checks whether any personal notes exist for a given vocabulary (R3.1).
     * Used during vocabulary deletion to prevent accidental destruction of learner study notes.
     *
     * @param vocabId the vocabulary ID
     * @return true if personal notes exist for this vocabulary, false otherwise
     */
    @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(pn) > 0 THEN true ELSE false END FROM PersonalNote pn WHERE pn.vocabulary.vocabId = :vocabId")
    boolean existsByVocabulary_VocabId(@org.springframework.data.repository.query.Param("vocabId") Long vocabId);
}
