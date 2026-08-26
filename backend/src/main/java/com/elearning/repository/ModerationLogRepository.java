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
}
