package com.elearning.service.impl;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.ApproveLessonRequest;
import com.elearning.dto.request.RejectLessonRequest;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.ModerationQueueResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.ModerationLog;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.ModerationLogRepository;
import com.elearning.service.ModerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link ModerationService} managing Content Moderation workflow.
 * Enforces state machine invariants, secure moderator resolution from SecurityContext,
 * immutable MODERATION_LOG audit trail persistence, and zero-entity-leak response mapping.
 */
@Service
@Transactional(readOnly = true)
public class ModerationServiceImpl implements ModerationService {

    private final LessonRepository lessonRepository;
    private final LessonVocabularyRepository lessonVocabularyRepository;
    private final AccountRepository accountRepository;
    private final ModerationLogRepository moderationLogRepository;

    public ModerationServiceImpl(LessonRepository lessonRepository,
                                 LessonVocabularyRepository lessonVocabularyRepository,
                                 AccountRepository accountRepository,
                                 ModerationLogRepository moderationLogRepository) {
        this.lessonRepository = lessonRepository;
        this.lessonVocabularyRepository = lessonVocabularyRepository;
        this.accountRepository = accountRepository;
        this.moderationLogRepository = moderationLogRepository;
    }

    @Override
    public Page<ModerationQueueResponse> getPendingLessons(Pageable pageable) {
        return lessonRepository.findPendingLessonSummaries(pageable);
    }

    @Override
    public LessonDetailResponse getPendingLessonById(Long lessonId) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ID bài học không được để trống");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        if (!"Pending".equalsIgnoreCase(lesson.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId);
        }

        List<LessonVocabulary> links = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        return LessonDetailResponse.fromEntity(lesson, links);
    }

    @Override
    @Transactional
    public LessonDetailResponse approveLesson(Long lessonId, ApproveLessonRequest request) {
        Account moderator = resolveCurrentModerator();

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        String currentStatus = lesson.getStatus();
        if ("Approved".equalsIgnoreCase(currentStatus)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Bài học đã được phê duyệt trước đó");
        }
        if (!"Pending".equalsIgnoreCase(currentStatus)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Không thể phê duyệt bài học đang ở trạng thái " + currentStatus);
        }

        lesson.setStatus("Approved");
        lesson = lessonRepository.save(lesson);

        // Record immutable audit trail in MODERATION_LOG
        ModerationLog log = new ModerationLog(lesson, moderator, "Approve", null, null);
        moderationLogRepository.save(log);

        List<LessonVocabulary> links = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        return LessonDetailResponse.fromEntity(lesson, links);
    }

    @Override
    @Transactional
    public LessonDetailResponse rejectLesson(Long lessonId, RejectLessonRequest request) {
        if (request == null || request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lý do từ chối không được để trống");
        }

        Account moderator = resolveCurrentModerator();

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        String currentStatus = lesson.getStatus();
        if ("Rejected".equalsIgnoreCase(currentStatus)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Bài học đã ở trạng thái Rejected");
        }
        if (!"Pending".equalsIgnoreCase(currentStatus)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Không thể từ chối bài học đang ở trạng thái " + currentStatus);
        }

        lesson.setStatus("Rejected");
        lesson = lessonRepository.save(lesson);

        // Record immutable audit trail in MODERATION_LOG
        ModerationLog log = new ModerationLog(lesson, moderator, "Reject", request.getRejectionReason(), request.getFlaggedFields());
        moderationLogRepository.save(log);

        List<LessonVocabulary> links = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        return LessonDetailResponse.fromEntity(lesson, links);
    }

    @Override
    public com.elearning.dto.response.PageResponse<com.elearning.dto.response.ModerationLogResponse> getModeratorHistory(
            String callerEmailOrPhone,
            Pageable pageable) {
        if (callerEmailOrPhone == null || callerEmailOrPhone.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Chưa xác thực hoặc phiên đăng nhập đã hết hạn");
        }

        Account caller = accountRepository.findByEmailOrPhone(callerEmailOrPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Tài khoản người dùng không tồn tại"));

        boolean isAdmin = caller.getRoles() != null && caller.getRoles().stream()
                .anyMatch(r -> "Admin".equalsIgnoreCase(r.getRoleName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getRoleName()));

        Page<ModerationLog> page;
        if (isAdmin) {
            page = moderationLogRepository.findAllByOrderByCreatedAtDescLogIdDesc(pageable);
        } else {
            page = moderationLogRepository.findByModerator_AccountIdOrderByCreatedAtDescLogIdDesc(caller.getAccountId(), pageable);
        }

        return com.elearning.dto.response.PageResponse.from(page.map(com.elearning.dto.response.ModerationLogResponse::fromEntity));
    }

    private Account resolveCurrentModerator() {
        Authentication authentication = SecurityContextHolder.getContext() != null ? SecurityContextHolder.getContext().getAuthentication() : null;
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Chưa xác thực hoặc phiên đăng nhập đã hết hạn");
        }
        String emailOrPhone = authentication.getName();
        return accountRepository.findByEmailOrPhone(emailOrPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Tài khoản người dùng không tồn tại"));
    }
}
