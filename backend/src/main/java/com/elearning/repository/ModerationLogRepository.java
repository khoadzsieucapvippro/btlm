package com.elearning.repository;

import com.elearning.entity.Lesson;
import com.elearning.entity.ModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModerationLogRepository extends JpaRepository<ModerationLog, Long> {

    List<ModerationLog> findByLessonOrderByCreatedAtDesc(Lesson lesson);

    List<ModerationLog> findByLesson_LessonIdOrderByCreatedAtDesc(Long lessonId);

    List<ModerationLog> findByLesson_LessonIdOrderByCreatedAtDescLogIdDesc(Long lessonId);

    List<ModerationLog> findByLesson_LessonIdOrderByLogIdDesc(Long lessonId);

    org.springframework.data.domain.Page<ModerationLog> findByModerator_AccountIdOrderByCreatedAtDescLogIdDesc(
            Long moderatorId,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<ModerationLog> findAllByOrderByCreatedAtDescLogIdDesc(
            org.springframework.data.domain.Pageable pageable);

    boolean existsByLesson_LessonId(Long lessonId);
}
