package com.elearning.repository;

import com.elearning.entity.PersonalNote;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalNoteRepository extends JpaRepository<PersonalNote, Long> {

    List<PersonalNote> findByUserAndVocabularyOrderByCreatedAtDesc(UserProfile user, Vocabulary vocabulary);

    Optional<PersonalNote> findByNoteIdAndUser(Long noteId, UserProfile user);

    List<PersonalNote> findByUserOrderByCreatedAtDesc(UserProfile user);
}
