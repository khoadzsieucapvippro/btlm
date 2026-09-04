package com.elearning.service.impl;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.PersonalNoteRequest;
import com.elearning.dto.response.PageResponse;
import com.elearning.dto.response.PersonalNoteResponse;
import com.elearning.entity.PersonalNote;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.PersonalNoteRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.PersonalNoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of PersonalNoteService.
 * Enforces:
 * - Content length <= 500 characters (defense in depth).
 * - Unlimited notes per user per vocabulary (no 5-note cap).
 * - Ownership checks: Learner can only view, update, and delete their own notes.
 * - Invariant: vocab_id and created_at are immutable during update.
 * - Bounded pagination for collection retrieval (OWASP API4:2023).
 */
@Service
public class PersonalNoteServiceImpl implements PersonalNoteService {

    private static final Logger log = LoggerFactory.getLogger(PersonalNoteServiceImpl.class);
    private static final int MAX_CONTENT_LENGTH = 500;

    /**
     * Default page size when client does not specify size parameter.
     */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum allowed page size to prevent unbounded resource consumption.
     * Client cannot exceed this value server-side.
     */
    private static final int MAX_PAGE_SIZE = 100;

    private final PersonalNoteRepository personalNoteRepository;
    private final VocabularyRepository vocabularyRepository;
    private final UserProfileRepository userProfileRepository;

    public PersonalNoteServiceImpl(PersonalNoteRepository personalNoteRepository,
                                  VocabularyRepository vocabularyRepository,
                                  UserProfileRepository userProfileRepository) {
        this.personalNoteRepository = personalNoteRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.userProfileRepository = userProfileRepository;
    }

    private UserProfile resolveCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Người dùng chưa được xác thực");
        }
        return userProfileRepository.findByAccountEmailOrPhone(auth.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy hồ sơ người dùng"));
    }

    private void validateContent(String content) {
        if (content == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nội dung ghi chú không được để null");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nội dung ghi chú tối đa 500 ký tự (nhận được: " + content.length() + ")");
        }
    }

    /**
     * Creates a bounded Pageable from client parameters.
     * Enforces server-side maximum page size to prevent unbounded resource consumption.
     *
     * @param page requested page number (0-indexed)
     * @param size requested page size
     * @return bounded Pageable with stable ordering
     */
    private Pageable createBoundedPageable(Integer page, Integer size) {
        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveSize = DEFAULT_PAGE_SIZE;
        if (size != null) {
            if (size <= 0) {
                effectiveSize = DEFAULT_PAGE_SIZE;
            } else {
                effectiveSize = Math.min(size, MAX_PAGE_SIZE);
            }
        }
        return PageRequest.of(effectivePage, effectiveSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PersonalNoteResponse> getNotesByVocabulary(Long vocabId, Pageable pageable) {
        UserProfile user = resolveCurrentUserProfile();

        if (vocabId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID từ vựng không được để trống");
        }

        Vocabulary vocab = vocabularyRepository.findById(vocabId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng với ID: " + vocabId));

        // Enforce bounded pageable with stable ordering, handling unpaged/null safely
        Pageable boundedPageable;
        if (pageable == null || pageable.isUnpaged()) {
            boundedPageable = createBoundedPageable(0, DEFAULT_PAGE_SIZE);
        } else {
            boundedPageable = createBoundedPageable(pageable.getPageNumber(), pageable.getPageSize());
        }
        Page<PersonalNote> notePage = personalNoteRepository.findByUserAndVocabulary(user, vocab, boundedPageable);

        List<PersonalNoteResponse> responses = notePage.getContent().stream()
                .map(PersonalNoteResponse::fromEntity)
                .toList();

        return new PageResponse<>(
                notePage.getNumber(),
                notePage.getSize(),
                notePage.getTotalElements(),
                notePage.getTotalPages(),
                responses
        );
    }

    @Override
    @Transactional
    public PersonalNoteResponse createNote(Long vocabId, PersonalNoteRequest request) {
        UserProfile user = resolveCurrentUserProfile();

        if (vocabId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID từ vựng không được để trống");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Yêu cầu ghi chú không được để null");
        }

        validateContent(request.getContent());

        Vocabulary vocab = vocabularyRepository.findByIdWithLock(vocabId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng với ID: " + vocabId));

        PersonalNote note = new PersonalNote(user, vocab, request.getContent());
        PersonalNote savedNote = personalNoteRepository.save(note);
        log.info("Created personal note id={} on vocabId={} for user_id={}", savedNote.getNoteId(), vocabId, user.getUserId());

        return PersonalNoteResponse.fromEntity(savedNote);
    }

    @Override
    @Transactional
    public PersonalNoteResponse updateNote(Long noteId, PersonalNoteRequest request) {
        UserProfile user = resolveCurrentUserProfile();

        if (noteId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID ghi chú không được để trống");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Yêu cầu ghi chú không được để null");
        }

        validateContent(request.getContent());

        PersonalNote note = personalNoteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy ghi chú với ID: " + noteId));

        if (!note.getUser().getUserId().equals(user.getUserId())) {
            log.warn("User user_id={} attempted unauthorized update on note id={} owned by user_id={}",
                    user.getUserId(), noteId, note.getUser().getUserId());
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền chỉnh sửa ghi chú này");
        }

        note.setContent(request.getContent());
        PersonalNote updatedNote = personalNoteRepository.save(note);
        log.info("Updated personal note id={} for user_id={}", noteId, user.getUserId());

        return PersonalNoteResponse.fromEntity(updatedNote);
    }

    @Override
    @Transactional
    public void deleteNote(Long noteId) {
        UserProfile user = resolveCurrentUserProfile();

        if (noteId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID ghi chú không được để trống");
        }

        PersonalNote note = personalNoteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy ghi chú với ID: " + noteId));

        if (!note.getUser().getUserId().equals(user.getUserId())) {
            log.warn("User user_id={} attempted unauthorized delete on note id={} owned by user_id={}",
                    user.getUserId(), noteId, note.getUser().getUserId());
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền xóa ghi chú này");
        }

        personalNoteRepository.delete(note);
        log.info("Deleted personal note id={} for user_id={}", noteId, user.getUserId());
    }
}
