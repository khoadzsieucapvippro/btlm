package com.elearning.service.impl;

import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.common.ErrorCode;
import com.elearning.exception.BusinessException;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.service.LessonService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link LessonService} providing public lesson exploration capabilities.
 * Guarantees zero entity leak, strict 'Approved' status filtering, and deterministic vocabulary ordering.
 */
@Service
@Transactional(readOnly = true)
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final LessonVocabularyRepository lessonVocabularyRepository;

    public LessonServiceImpl(LessonRepository lessonRepository,
                             LessonVocabularyRepository lessonVocabularyRepository) {
        this.lessonRepository = lessonRepository;
        this.lessonVocabularyRepository = lessonVocabularyRepository;
    }

    @Override
    public Page<LessonSummaryResponse> getApprovedLessons(Pageable pageable) {
        Pageable resolvedPageable = (pageable != null) ? pageable : PageRequest.of(0, 20);
        return lessonRepository.findApprovedLessonSummaries(resolvedPageable);
    }

    @Override
    public LessonDetailResponse getApprovedLessonById(Long lessonId) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ID bài học không được để trống");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        // Enforce public discovery rule: non-approved lessons (Draft, Pending, Rejected) are hidden (404)
        if (!"Approved".equalsIgnoreCase(lesson.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId);
        }

        // Fetch vocabularies with JOIN FETCH to prevent N+1 queries, sorted by order_index ASC
        List<LessonVocabulary> vocabularies = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);

        return LessonDetailResponse.fromEntity(lesson, vocabularies);
    }

    @Override
    public Page<LessonSummaryResponse> getAdminLessons(String status, Pageable pageable) {
        Pageable resolvedPageable = (pageable != null) ? pageable : PageRequest.of(0, 20);
        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            normalizedStatus = status.trim();
        }
        return lessonRepository.findAdminLessonSummaries(normalizedStatus, resolvedPageable);
    }
}
