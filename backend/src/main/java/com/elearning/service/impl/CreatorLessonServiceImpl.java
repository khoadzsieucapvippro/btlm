package com.elearning.service.impl;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.CreateLessonRequest;
import com.elearning.dto.request.ReorderVocabRequest;
import com.elearning.dto.request.UpdateLessonRequest;
import com.elearning.dto.response.ImportValidationReport;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.dto.response.ParsedVocabularyItem;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.LessonVocabularyId;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.CreatorLessonService;
import com.elearning.service.ExcelParserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CreatorLessonService} managing Creator Lesson Studio operations.
 * Strictly enforces Creator ownership isolation and lifecycle transition invariants.
 */
@Service
public class CreatorLessonServiceImpl implements CreatorLessonService {

    private final LessonRepository lessonRepository;
    private final LessonVocabularyRepository lessonVocabularyRepository;
    private final AccountRepository accountRepository;
    private final VocabularyRepository vocabularyRepository;
    private final ExcelParserService excelParserService;
    private final com.elearning.repository.ModerationLogRepository moderationLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CreatorLessonServiceImpl(LessonRepository lessonRepository,
                                   LessonVocabularyRepository lessonVocabularyRepository,
                                   AccountRepository accountRepository,
                                   VocabularyRepository vocabularyRepository,
                                   ExcelParserService excelParserService,
                                   com.elearning.repository.ModerationLogRepository moderationLogRepository,
                                   EntityManager entityManager) {
        this.lessonRepository = lessonRepository;
        this.lessonVocabularyRepository = lessonVocabularyRepository;
        this.accountRepository = accountRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.excelParserService = excelParserService;
        this.moderationLogRepository = moderationLogRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public LessonDetailResponse createLesson(CreateLessonRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Tiêu đề bài học không được để trống");
        }

        Account currentAccount = resolveCurrentAccount();

        Lesson lesson = new Lesson(request.getTitle().trim(), currentAccount);
        lesson.setStatus("Draft");
        lesson.setExcelFileUrl(request.getExcelFileUrl());

        lesson = lessonRepository.save(lesson);

        // Add initial vocabulary items if provided
        if (request.getVocabularyIds() != null && !request.getVocabularyIds().isEmpty()) {
            int order = 1;
            for (Long vocabId : request.getVocabularyIds()) {
                Vocabulary vocabulary = vocabularyRepository.findByIdWithLock(vocabId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng với ID: " + vocabId));
                LessonVocabulary lv = new LessonVocabulary(lesson, vocabulary, order++);
                lessonVocabularyRepository.save(lv);
            }
        }

        List<LessonVocabulary> vocabularies = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lesson.getLessonId());
        return LessonDetailResponse.fromEntity(lesson, vocabularies);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonSummaryResponse> getMyLessons(Pageable pageable) {
        Account currentAccount = resolveCurrentAccount();
        Pageable resolved = (pageable != null) ? pageable : PageRequest.of(0, 20);

        Page<Lesson> lessonPage = lessonRepository.findByCreatedBy(currentAccount, resolved);
        if (lessonPage.isEmpty()) {
            return lessonPage.map(LessonSummaryResponse::fromEntity);
        }

        List<Long> lessonIds = lessonPage.getContent().stream()
                .map(Lesson::getLessonId)
                .toList();

        List<Object[]> countRows = lessonVocabularyRepository.countVocabulariesByLessonIds(lessonIds);
        Map<Long, Integer> countMap = new HashMap<>();
        for (Object[] row : countRows) {
            Long lessonId = (Long) row[0];
            Number count = (Number) row[1];
            countMap.put(lessonId, count != null ? count.intValue() : 0);
        }

        return lessonPage.map(lesson -> {
            int count = countMap.getOrDefault(lesson.getLessonId(), 0);
            return LessonSummaryResponse.fromEntity(lesson, count);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDetailResponse getMyLessonById(Long lessonId) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ID bài học không được để trống");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        Account currentAccount = resolveCurrentAccount();
        checkOwnership(lesson, currentAccount);

        List<LessonVocabulary> vocabularies = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        return LessonDetailResponse.fromEntity(lesson, vocabularies);
    }

    @Override
    @Transactional
    public LessonDetailResponse updateMyLesson(Long lessonId, UpdateLessonRequest request) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ID bài học không được để trống");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        Account currentAccount = resolveCurrentAccount();
        checkOwnership(lesson, currentAccount);
        checkEditableState(lesson, "chỉnh sửa");

        if (request != null) {
            if (request.getTitle() != null) {
                if (request.getTitle().trim().isEmpty()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "Tiêu đề bài học không được để trống");
                }
                lesson.setTitle(request.getTitle().trim());
            }
            if (request.getExcelFileUrl() != null) {
                lesson.setExcelFileUrl(request.getExcelFileUrl());
            }
        }

        lesson = lessonRepository.save(lesson);

        List<LessonVocabulary> vocabularies = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        return LessonDetailResponse.fromEntity(lesson, vocabularies);
    }

    @Override
    @Transactional
    public void deleteMyLesson(Long lessonId) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ID bài học không được để trống");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        Account currentAccount = resolveCurrentAccount();
        checkOwnership(lesson, currentAccount);
        checkEditableState(lesson, "xóa");

        if (moderationLogRepository.existsByLesson_LessonId(lessonId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Không thể xóa bài học đã có lịch sử kiểm duyệt (Moderation Log)");
        }

        // Delete associations first to ensure consistency in JPA context and DB
        List<LessonVocabulary> items = lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(lessonId);
        if (!items.isEmpty()) {
            lessonVocabularyRepository.deleteAll(items);
        }
        lessonRepository.delete(lesson);
        entityManager.flush();
    }

    @Override
    @Transactional
    public LessonDetailResponse addVocabularyToLesson(Long lessonId, Long vocabId) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ID bài học không được để trống");
        }
        if (vocabId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ID từ vựng không được để trống");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        Account currentAccount = resolveCurrentAccount();
        checkOwnership(lesson, currentAccount);
        checkEditableState(lesson, "thêm từ vựng");

        if (lessonVocabularyRepository.existsByLesson_LessonIdAndVocabulary_VocabId(lessonId, vocabId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Từ vựng đã tồn tại trong bài học");
        }

        Vocabulary vocabulary = vocabularyRepository.findByIdWithLock(vocabId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng với ID: " + vocabId));

        int nextOrderIndex = (int) lessonVocabularyRepository.countByLesson_LessonId(lessonId) + 1;

        LessonVocabulary lv = new LessonVocabulary(lesson, vocabulary, nextOrderIndex);
        lessonVocabularyRepository.save(lv);

        List<LessonVocabulary> vocabularies = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        return LessonDetailResponse.fromEntity(lesson, vocabularies);
    }

    @Override
    @Transactional
    public LessonDetailResponse removeVocabularyFromLesson(Long lessonId, Long vocabId) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ID bài học không được để trống");
        }
        if (vocabId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ID từ vựng không được để trống");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        Account currentAccount = resolveCurrentAccount();
        checkOwnership(lesson, currentAccount);
        checkEditableState(lesson, "xóa từ vựng");

        LessonVocabularyId id = new LessonVocabularyId(lessonId, vocabId);
        LessonVocabulary lv = lessonVocabularyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Từ vựng không tồn tại trong bài học này"));

        lessonVocabularyRepository.delete(lv);
        entityManager.flush();

        // Re-index remaining vocabulary items to ensure contiguous 1, 2, 3...
        List<LessonVocabulary> remaining = lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(lessonId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setOrderIndex(i + 1);
        }
        lessonVocabularyRepository.saveAll(remaining);
        entityManager.flush();

        List<LessonVocabulary> vocabularies = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        return LessonDetailResponse.fromEntity(lesson, vocabularies);
    }

    @Override
    @Transactional
    public LessonDetailResponse reorderVocabulary(Long lessonId, ReorderVocabRequest request) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ID bài học không được để trống");
        }
        if (request == null || request.getResolvedVocabIds() == null || request.getResolvedVocabIds().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Danh sách thứ tự từ vựng không được để trống");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        Account currentAccount = resolveCurrentAccount();
        checkOwnership(lesson, currentAccount);
        checkEditableState(lesson, "thay đổi thứ tự từ vựng");

        List<Long> orderedIds = request.getResolvedVocabIds();

        // Check for duplicate vocabulary IDs in request
        Set<Long> uniqueIds = new HashSet<>(orderedIds);
        if (uniqueIds.size() != orderedIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Danh sách từ vựng chứa ID bị trùng lặp");
        }

        List<LessonVocabulary> currentLvs = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        Set<Long> existingIds = currentLvs.stream()
                .map(lv -> lv.getVocabulary().getVocabId())
                .collect(Collectors.toSet());

        // Ensure exact membership match (no extra, no missing vocabularies)
        if (!uniqueIds.equals(existingIds)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Yêu cầu thay đổi thứ tự phải chứa đầy đủ và chính xác các từ vựng hiện có trong bài học");
        }

        Map<Long, LessonVocabulary> lvMap = currentLvs.stream()
                .collect(Collectors.toMap(lv -> lv.getVocabulary().getVocabId(), lv -> lv));

        // In MySQL, order_index is INT UNSIGNED, so negative values cause out-of-range truncation.
        // We use a large positive offset (1,000,000) for Phase 1 to avoid UK collision on uk_lesson_order_index.
        final int TEMP_OFFSET = 1_000_000;

        // Phase 1: Set temporary offset orderIndex
        for (int i = 0; i < orderedIds.size(); i++) {
            LessonVocabulary lv = lvMap.get(orderedIds.get(i));
            lv.setOrderIndex(TEMP_OFFSET + i + 1);
        }
        lessonVocabularyRepository.saveAll(currentLvs);
        entityManager.flush();

        // Phase 2: Set target positive orderIndex (1, 2, 3...)
        for (int i = 0; i < orderedIds.size(); i++) {
            LessonVocabulary lv = lvMap.get(orderedIds.get(i));
            lv.setOrderIndex(i + 1);
        }
        lessonVocabularyRepository.saveAll(currentLvs);
        entityManager.flush();

        List<LessonVocabulary> reloaded = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        return LessonDetailResponse.fromEntity(lesson, reloaded);
    }

    @Override
    @Transactional
    public LessonDetailResponse submitForModeration(Long lessonId) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ID bài học không được để trống");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: " + lessonId));

        Account currentAccount = resolveCurrentAccount();
        checkOwnership(lesson, currentAccount);

        String status = lesson.getStatus();
        if ("Pending".equalsIgnoreCase(status)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Bài học đã được nộp và đang chờ kiểm duyệt");
        }
        if ("Approved".equalsIgnoreCase(status)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Bài học đã được phê duyệt, không thể nộp lại");
        }
        if (!"Draft".equalsIgnoreCase(status) && !"Rejected".equalsIgnoreCase(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Trạng thái bài học '" + status + "' không hợp lệ để nộp kiểm duyệt");
        }

        long vocabCount = lessonVocabularyRepository.countByLesson_LessonId(lessonId);
        if (vocabCount == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Bài học phải chứa ít nhất một từ vựng trước khi nộp kiểm duyệt");
        }

        lesson.setStatus("Pending");
        lesson = lessonRepository.save(lesson);

        List<LessonVocabulary> vocabularies = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        return LessonDetailResponse.fromEntity(lesson, vocabularies);
    }

    @Override
    @Transactional
    public LessonDetailResponse importLessonFromExcel(String title, MultipartFile file) {
        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Tiêu đề bài học không được để trống");
        }
        if (title.trim().length() > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Tiêu đề bài học không được vượt quá 100 ký tự");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File tải lên không có dữ liệu");
        }

        // Parse and validate using ExcelParserService
        ImportValidationReport report = excelParserService.parseAndValidate(file);
        if (!Boolean.TRUE.equals(report.getIsValid())) {
            throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
                    "File Excel không hợp lệ hoặc chứa lỗi dữ liệu: " + report.getSummaryMessage());
        }

        List<ParsedVocabularyItem> rows = report.getRows();
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File Excel không chứa bất kỳ dòng từ vựng nào");
        }

        Account currentAccount = resolveCurrentAccount();

        // 1. Create Lesson in Draft state
        Lesson lesson = new Lesson(title.trim(), currentAccount);
        lesson.setStatus("Draft");
        lesson.setExcelFileUrl(file.getOriginalFilename());
        lesson = lessonRepository.save(lesson);

        // 2. Persist or retrieve vocabularies and associate with Lesson
        int order = 1;
        for (ParsedVocabularyItem item : rows) {
            Vocabulary vocabulary = null;
            if (Boolean.TRUE.equals(item.getIsExisting()) && item.getExistingVocabId() != null) {
                vocabulary = vocabularyRepository.findById(item.getExistingVocabId()).orElse(null);
            }

            if (vocabulary == null) {
                Optional<Vocabulary> existingOpt = vocabularyRepository.findByHanziAndPinyinRaw(
                        item.getHanzi(), item.getPinyinRaw());
                if (existingOpt.isPresent()) {
                    vocabulary = existingOpt.get();
                } else {
                    Vocabulary newVocab = new Vocabulary();
                    newVocab.setHanzi(item.getHanzi());
                    newVocab.setPinyin(item.getPinyin());
                    newVocab.setPinyinRaw(item.getPinyinRaw());
                    newVocab.setMeaningHanViet(item.getMeaningHanViet());
                    newVocab.setMeaningVi(item.getMeaningVi());
                    newVocab.setExampleSentence(item.getExampleSentence());
                    newVocab.setExampleTranslation(item.getExampleTranslation());
                    vocabulary = vocabularyRepository.save(newVocab);
                }
            }

            LessonVocabulary lv = new LessonVocabulary(lesson, vocabulary, order++);
            lessonVocabularyRepository.save(lv);
        }

        List<LessonVocabulary> vocabularies = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lesson.getLessonId());
        return LessonDetailResponse.fromEntity(lesson, vocabularies);
    }

    private Account resolveCurrentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Chưa xác thực hoặc phiên đăng nhập đã hết hạn");
        }
        String emailOrPhone = authentication.getName();
        return accountRepository.findByEmailOrPhone(emailOrPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Tài khoản người dùng không tồn tại"));
    }

    private void checkOwnership(Lesson lesson, Account currentAccount) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") || a.getAuthority().equalsIgnoreCase("Admin"));

        if (isAdmin) {
            return; // Admin can manage all creator lessons
        }

        if (lesson.getCreatedBy() == null || !lesson.getCreatedBy().getAccountId().equals(currentAccount.getAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền thao tác trên bài học này");
        }
    }

    private void checkEditableState(Lesson lesson, String operationName) {
        String status = lesson.getStatus();
        if ("Pending".equalsIgnoreCase(status)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Bài học đang trong hàng đợi kiểm duyệt, không thể " + operationName);
        }
        if ("Approved".equalsIgnoreCase(status)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Bài học đã được phê duyệt và xuất bản, không thể " + operationName);
        }
        if (!"Draft".equalsIgnoreCase(status) && !"Rejected".equalsIgnoreCase(status)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Trạng thái bài học '" + status + "' không cho phép " + operationName);
        }
    }
}
