package com.elearning;

import com.elearning.dto.request.ApproveLessonRequest;
import com.elearning.dto.request.RegisterRequest;
import com.elearning.dto.request.RejectLessonRequest;
import com.elearning.dto.request.ReorderVocabRequest;
import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.entity.Account;
import com.elearning.entity.CardProgress;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.ModerationLog;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.CardProgressRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.ModerationLogRepository;
import com.elearning.repository.ReviewLogRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.security.JwtUtil;
import com.elearning.service.AuthService;
import com.elearning.service.CreatorLessonService;
import com.elearning.service.ModerationService;
import com.elearning.service.SrsService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

/**
 * Authoritative Master Verification Suite for BE-TEST-002:
 * Transaction Rollback Tests for Critical Multi-Step Operations.
 *
 * Exercises real Spring-managed declarative transaction boundaries (@Transactional on service proxies)
 * without test-managed transaction masking (@Transactional(propagation = Propagation.NOT_SUPPORTED))
 * on a real Testcontainers MySQL 8.4 database:
 * 1. Excel Lesson Import: Partial persistence failure rolls back Lesson, new Vocabulary, and LessonVocabulary.
 * 2. Lesson Moderation (Approve): Audit log failure rolls back Lesson status to Pending.
 * 3. Lesson Moderation (Reject): Audit log failure rolls back Lesson status to Pending.
 * 4. SRS Card Review: ReviewLog failure rolls back CardProgress state mutation.
 * 5. Account Registration: JWT generation failure rolls back Account, UserProfile, and Role associations.
 * 6. Creator Lesson Deletion: Lesson deletion failure rolls back LessonVocabulary deletions.
 */
@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("BE-TEST-002: Master Transaction Rollback & Atomicity Integration Tests")
class TransactionRollbackIntegrationTests {

    @Autowired
    private CreatorLessonService creatorLessonService;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private SrsService srsService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @SpyBean
    private LessonRepository lessonRepository;

    @SpyBean
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @SpyBean
    private ModerationLogRepository moderationLogRepository;

    @SpyBean
    private CardProgressRepository cardProgressRepository;

    @SpyBean
    private ReviewLogRepository reviewLogRepository;

    @Autowired
    private UserSrsSettingRepository userSrsSettingRepository;

    @SpyBean
    private JwtUtil jwtUtil;

    private List<Long> cleanupLessonIds = new ArrayList<>();
    private List<String> cleanupEmails = new ArrayList<>();
    private List<Long> cleanupVocabIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        Mockito.reset(
                lessonRepository,
                lessonVocabularyRepository,
                moderationLogRepository,
                cardProgressRepository,
                reviewLogRepository,
                jwtUtil
        );

        for (Long lessonId : cleanupLessonIds) {
            lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId));
            moderationLogRepository.deleteAll(moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDesc(lessonId));
            lessonRepository.findById(lessonId).ifPresent(lessonRepository::delete);
        }
        cleanupLessonIds.clear();

        for (String email : cleanupEmails) {
            accountRepository.findByEmailOrPhone(email).ifPresent(account -> {
                if (account.getUserProfile() != null) {
                    userProfileRepository.delete(account.getUserProfile());
                }
                accountRepository.delete(account);
            });
        }
        cleanupEmails.clear();

        for (Long vocabId : cleanupVocabIds) {
            vocabularyRepository.findById(vocabId).ifPresent(vocabularyRepository::delete);
        }
        cleanupVocabIds.clear();
    }

    private Account createTestAccount(String prefix, String roleName) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = prefix + "_" + suffix + "@elearning.com";
        cleanupEmails.add(email);

        Role role = roleRepository.findByRoleName(roleName).orElse(null);

        Account account = new Account();
        account.setEmailOrPhone(email);
        account.setPasswordHash("hashedPassword123");
        account.setStatus("Active");
        if (role != null) {
            account.getRoles().add(role);
        }
        account = accountRepository.save(account);

        UserProfile profile = new UserProfile();
        profile.setAccount(account);
        profile.setFullName(prefix + " FullName " + suffix);
        userProfileRepository.save(profile);

        return account;
    }

    private void authenticate(Account account, String role) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                account.getEmailOrPhone(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private byte[] createWorkbook(String[] headers, String[][] data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Vocabulary");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            for (int r = 0; r < data.length; r++) {
                Row dataRow = sheet.createRow(r + 1);
                for (int c = 0; c < data[r].length; c++) {
                    Cell cell = dataRow.createCell(c);
                    cell.setCellValue(data[r][c]);
                }
            }
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    // =========================================================================
    // 1. EXCEL IMPORT CONFIRMATION ROLLBACK
    // =========================================================================

    @Test
    @DisplayName("GIVEN mid-stream persistence failure during Excel confirm WHEN saving 2nd LessonVocabulary fails THEN Lesson, new Vocab, and LessonVocab are completely rolled back")
    void testExcelImportConfirmRollback_midStreamFailure_restoresDatabaseCleanly() throws Exception {
        Account creator = createTestAccount("creator_imp_rb", "Creator");
        authenticate(creator, "CREATOR");

        long lessonCountBefore = lessonRepository.count();
        long vocabCountBefore = vocabularyRepository.count();
        long lvCountBefore = lessonVocabularyRepository.count();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 6);
        String uniqueHanzi1 = "星" + uniqueSuffix;
        String uniqueHanzi2 = "河" + uniqueSuffix;

        String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
        String[][] data = {
                {uniqueHanzi1, "xīng", "Tinh", "Ngôi sao"}, // Row 1 (order 1)
                {uniqueHanzi2, "hé", "Hà", "Dòng sông"}     // Row 2 (order 2)
        };
        byte[] bytes = createWorkbook(headers, data);
        MockMultipartFile file = new MockMultipartFile("file", "import_rb.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        String lessonTitle = "Bài học Rollback Excel " + uniqueSuffix;

        // Injected Failure: Spy throws RuntimeException on saving 2nd LessonVocabulary
        doThrow(new RuntimeException("Simulated mid-stream DB I/O error during 2nd LessonVocabulary save"))
                .when(lessonVocabularyRepository)
                .save(argThat(lv -> lv != null && Integer.valueOf(2).equals(lv.getOrderIndex())));

        // Call Spring Service Bean Proxy
        assertThatThrownBy(() -> creatorLessonService.importLessonFromExcel(lessonTitle, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated mid-stream DB I/O error");

        // Database Verification: Total row counts match exact baseline
        assertThat(lessonRepository.count()).as("Lesson count must be unchanged").isEqualTo(lessonCountBefore);
        assertThat(vocabularyRepository.count()).as("Vocabulary count must be unchanged").isEqualTo(vocabCountBefore);
        assertThat(lessonVocabularyRepository.count()).as("LessonVocabulary count must be unchanged").isEqualTo(lvCountBefore);

        // Verify specific entities are absent from MySQL
        List<Lesson> lessons = lessonRepository.findByCreatedBy(creator, org.springframework.data.domain.Pageable.unpaged()).getContent();
        assertThat(lessons).noneMatch(l -> lessonTitle.equals(l.getTitle()));
        assertThat(vocabularyRepository.findByHanziAndPinyinRaw(uniqueHanzi1, "xing")).isEmpty();
        assertThat(vocabularyRepository.findByHanziAndPinyinRaw(uniqueHanzi2, "he")).isEmpty();
    }

    // =========================================================================
    // 2. MODERATION APPROVE ROLLBACK
    // =========================================================================

    @Test
    @DisplayName("GIVEN Pending Lesson WHEN saving ModerationLog fails during approveLesson THEN Lesson status change rolls back to Pending and 0 logs remain")
    void testModerationApproveRollback_auditLogFailure_restoresPendingState() {
        Account creator = createTestAccount("creator_mod_rb", "Creator");
        Account moderator = createTestAccount("mod_approve_rb", "Moderator");
        authenticate(moderator, "MODERATOR");

        Lesson lesson = new Lesson("Bài học Approve Rollback Test", creator);
        lesson.setStatus("Pending");
        lesson = lessonRepository.save(lesson);
        cleanupLessonIds.add(lesson.getLessonId());
        assertThat(lesson.getVersion()).isEqualTo(0L);

        // Injected Failure: Spy throws RuntimeException when persisting ModerationLog
        doThrow(new RuntimeException("Simulated I/O database failure during ModerationLog persist"))
                .when(moderationLogRepository)
                .save(any(ModerationLog.class));

        Long lessonId = lesson.getLessonId();
        ApproveLessonRequest request = new ApproveLessonRequest("Duyệt bài học kiểm thử rollback");

        // Call Spring Service Bean Proxy
        assertThatThrownBy(() -> moderationService.approveLesson(lessonId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated I/O database failure");

        // Database Verification: Fresh read from MySQL
        Lesson persistedLesson = lessonRepository.findById(lessonId).orElseThrow();
        assertThat(persistedLesson.getStatus()).as("Lesson status must remain Pending after rollback").isEqualTo("Pending");
        assertThat(persistedLesson.getVersion()).as("Lesson version must remain 0 after rollback").isEqualTo(0L);

        List<ModerationLog> logs = moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDesc(lessonId);
        assertThat(logs).as("ModerationLog must be completely rolled back (empty)").isEmpty();
    }

    // =========================================================================
    // 3. MODERATION REJECT ROLLBACK
    // =========================================================================

    @Test
    @DisplayName("GIVEN Pending Lesson WHEN saving ModerationLog fails during rejectLesson THEN Lesson status change rolls back to Pending and 0 logs remain")
    void testModerationRejectRollback_auditLogFailure_restoresPendingState() {
        Account creator = createTestAccount("creator_rej_rb", "Creator");
        Account moderator = createTestAccount("mod_reject_rb", "Moderator");
        authenticate(moderator, "MODERATOR");

        Lesson lesson = new Lesson("Bài học Reject Rollback Test", creator);
        lesson.setStatus("Pending");
        lesson = lessonRepository.save(lesson);
        cleanupLessonIds.add(lesson.getLessonId());
        assertThat(lesson.getVersion()).isEqualTo(0L);

        // Injected Failure: Spy throws RuntimeException when persisting ModerationLog
        doThrow(new RuntimeException("Simulated I/O database failure during ModerationLog reject persist"))
                .when(moderationLogRepository)
                .save(any(ModerationLog.class));

        Long lessonId = lesson.getLessonId();
        RejectLessonRequest request = new RejectLessonRequest("Lý do từ chối kiểm thử rollback", "[\"title\"]");

        // Call Spring Service Bean Proxy
        assertThatThrownBy(() -> moderationService.rejectLesson(lessonId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated I/O database failure");

        // Database Verification: Fresh read from MySQL
        Lesson persistedLesson = lessonRepository.findById(lessonId).orElseThrow();
        assertThat(persistedLesson.getStatus()).as("Lesson status must remain Pending after rollback").isEqualTo("Pending");
        assertThat(persistedLesson.getVersion()).as("Lesson version must remain 0 after rollback").isEqualTo(0L);

        List<ModerationLog> logs = moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDesc(lessonId);
        assertThat(logs).as("ModerationLog must be completely rolled back (empty)").isEmpty();
    }

    // =========================================================================
    // 4. SRS CARD REVIEW ROLLBACK
    // =========================================================================

    @Test
    @DisplayName("GIVEN existing CardProgress WHEN ReviewLog persistence fails during reviewCard THEN CardProgress state mutations roll back 100% on real MySQL")
    void testSrsReviewRollback_reviewLogFailure_restoresCardProgressState() {
        Account learner = createTestAccount("learner_srs_rb", "Learner");
        authenticate(learner, "LEARNER");

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary vocab = new Vocabulary("退" + suffix, "tuì", "tui" + suffix, "Thoái", "Rút lui");
        vocab = vocabularyRepository.save(vocab);
        cleanupVocabIds.add(vocab.getVocabId());

        UserProfile profile = userProfileRepository.findByAccount(learner).orElseThrow();

        LocalDateTime initialNextReview = LocalDateTime.now().minusHours(2);
        CardProgress progress = new CardProgress(
                profile,
                "VOCABULARY",
                vocab.getVocabId(),
                new BigDecimal("2.50"),
                6,
                2,
                initialNextReview
        );
        progress = cardProgressRepository.save(progress);
        Long progressId = progress.getProgressId();
        assertThat(progress.getVersion()).isEqualTo(0L);

        long logCountBefore = reviewLogRepository.count();

        // Injected Failure: Spy throws RuntimeException on ReviewLog save
        doThrow(new RuntimeException("Simulated I/O database failure on review_log insert"))
                .when(reviewLogRepository)
                .save(any(ReviewLog.class));

        ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 4);

        // Call Spring Service Bean Proxy
        assertThatThrownBy(() -> srsService.reviewCard(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated I/O database failure on review_log insert");

        // Database Verification: Fresh read from MySQL
        CardProgress verifiedProgress = cardProgressRepository.findById(progressId).orElseThrow();
        assertThat(verifiedProgress.getIntervalDays()).as("Interval must remain 6").isEqualTo(6);
        assertThat(verifiedProgress.getRepetitions()).as("Repetitions must remain 2").isEqualTo(2);
        assertThat(verifiedProgress.getEaseFactor()).as("EaseFactor must remain 2.50").isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(verifiedProgress.getVersion()).as("Version must remain 0").isEqualTo(0L);

        List<ReviewLog> logs = reviewLogRepository.findByUserAndItemTypeAndItemIdOrderByReviewedAtDesc(profile, "VOCABULARY", vocab.getVocabId());
        assertThat(logs).as("Zero ReviewLog rows should be persisted").isEmpty();
        assertThat(reviewLogRepository.count()).isEqualTo(logCountBefore);

        // Cleanup progress
        cardProgressRepository.delete(verifiedProgress);
    }

    // =========================================================================
    // 5. ACCOUNT REGISTRATION ROLLBACK
    // =========================================================================

    @Test
    @DisplayName("GIVEN registration request WHEN token generation fails after account persistence THEN Account, UserProfile, and Roles roll back completely")
    void testAuthRegistrationRollback_tokenGenerationFailure_restoresDatabaseCleanly() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String targetEmail = "reg_rb_" + suffix + "@elearning.com";
        cleanupEmails.add(targetEmail);

        long accountCountBefore = accountRepository.count();
        long profileCountBefore = userProfileRepository.count();

        // Injected Failure: Spy throws RuntimeException during JWT token generation after entity saves
        doThrow(new RuntimeException("Simulated cryptographic signing key failure"))
                .when(jwtUtil)
                .generateToken(eq(targetEmail), anyList(), any());

        RegisterRequest request = new RegisterRequest(targetEmail, "ValidPassword123!", "Learner " + suffix);

        // Call Spring Service Bean Proxy
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated cryptographic signing key failure");

        // Database Verification: MySQL must have zero trace of the failed registration
        assertThat(accountRepository.findByEmailOrPhone(targetEmail)).as("Account record must be rolled back").isEmpty();
        assertThat(accountRepository.count()).isEqualTo(accountCountBefore);
        assertThat(userProfileRepository.count()).isEqualTo(profileCountBefore);
    }

    // =========================================================================
    // 6. CREATOR LESSON DELETION ROLLBACK
    // =========================================================================

    @Test
    @DisplayName("GIVEN Draft Lesson with vocabularies WHEN lesson deletion fails after deleting associations THEN Lesson and LessonVocabulary associations are restored")
    void testCreatorLessonDeleteRollback_lessonDeleteFailure_restoresLessonVocabularyLinks() {
        Account creator = createTestAccount("creator_del_rb", "Creator");
        authenticate(creator, "CREATOR");

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary v1 = vocabularyRepository.save(new Vocabulary("删" + suffix, "shān", "shan" + suffix, "San", "Xóa"));
        Vocabulary v2 = vocabularyRepository.save(new Vocabulary("除" + suffix, "chú", "chu" + suffix, "Trừ", "Trừ bỏ"));
        cleanupVocabIds.add(v1.getVocabId());
        cleanupVocabIds.add(v2.getVocabId());

        Lesson lesson = new Lesson("Bài học Delete Rollback Test", creator);
        lesson.setStatus("Draft");
        lesson = lessonRepository.save(lesson);
        cleanupLessonIds.add(lesson.getLessonId());
        Long lessonId = lesson.getLessonId();

        LessonVocabulary lv1 = new LessonVocabulary(lesson, v1, 1);
        LessonVocabulary lv2 = new LessonVocabulary(lesson, v2, 2);
        lessonVocabularyRepository.save(lv1);
        lessonVocabularyRepository.save(lv2);

        // Injected Failure: Spy throws RuntimeException on lessonRepository.delete
        doThrow(new RuntimeException("Simulated database failure during lesson record delete"))
                .when(lessonRepository)
                .delete(argThat(l -> l != null && lessonId.equals(l.getLessonId())));

        // Call Spring Service Bean Proxy
        assertThatThrownBy(() -> creatorLessonService.deleteMyLesson(lessonId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated database failure during lesson record delete");

        // Database Verification: Both Lesson and all LessonVocabulary rows restored
        assertThat(lessonRepository.findById(lessonId)).isPresent();
        List<LessonVocabulary> restoredLinks = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        assertThat(restoredLinks).as("Both LessonVocabulary associations must be rolled back and restored").hasSize(2);
        assertThat(restoredLinks.get(0).getOrderIndex()).isEqualTo(1);
        assertThat(restoredLinks.get(1).getOrderIndex()).isEqualTo(2);
    }

    // =========================================================================
    // 7. CREATOR LESSON REORDER ROLLBACK
    // =========================================================================

    @Test
    @DisplayName("GIVEN Draft Lesson with 3 vocabularies WHEN reorder fails in Phase 2 THEN LessonVocabulary orderIndex mutations roll back to original [1, 2, 3]")
    void testCreatorLessonReorderRollback_phase2Failure_restoresOriginalOrderSequence() {
        Account creator = createTestAccount("creator_reord_rb", "Creator");
        authenticate(creator, "CREATOR");

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        Vocabulary v1 = vocabularyRepository.save(new Vocabulary("一" + suffix, "yī", "yi" + suffix, "Nhất", "Một"));
        Vocabulary v2 = vocabularyRepository.save(new Vocabulary("二" + suffix, "èr", "er" + suffix, "Nhị", "Hai"));
        Vocabulary v3 = vocabularyRepository.save(new Vocabulary("三" + suffix, "sān", "san" + suffix, "Tam", "Ba"));
        cleanupVocabIds.add(v1.getVocabId());
        cleanupVocabIds.add(v2.getVocabId());
        cleanupVocabIds.add(v3.getVocabId());

        Lesson lesson = new Lesson("Bài học Reorder Rollback Test", creator);
        lesson.setStatus("Draft");
        lesson = lessonRepository.save(lesson);
        cleanupLessonIds.add(lesson.getLessonId());
        Long lessonId = lesson.getLessonId();

        LessonVocabulary lv1 = new LessonVocabulary(lesson, v1, 1);
        LessonVocabulary lv2 = new LessonVocabulary(lesson, v2, 2);
        LessonVocabulary lv3 = new LessonVocabulary(lesson, v3, 3);
        lessonVocabularyRepository.save(lv1);
        lessonVocabularyRepository.save(lv2);
        lessonVocabularyRepository.save(lv3);

        // Reorder request: [v3, v1, v2]
        ReorderVocabRequest request = new ReorderVocabRequest(List.of(v3.getVocabId(), v1.getVocabId(), v2.getVocabId()));

        // Injected Failure: Spy throws RuntimeException when saveAll is called
        doThrow(new RuntimeException("Simulated database failure during LessonVocabulary saveAll"))
                .when(lessonVocabularyRepository).saveAll(any());

        // Call Spring Service Bean Proxy
        assertThatThrownBy(() -> creatorLessonService.reorderVocabulary(lessonId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated database failure during LessonVocabulary saveAll");

        // Database Verification: Original order indices 1, 2, 3 restored on MySQL
        List<LessonVocabulary> restoredLinks = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
        assertThat(restoredLinks).hasSize(3);
        assertThat(restoredLinks.get(0).getVocabulary().getVocabId()).isEqualTo(v1.getVocabId());
        assertThat(restoredLinks.get(0).getOrderIndex()).isEqualTo(1);
        assertThat(restoredLinks.get(1).getVocabulary().getVocabId()).isEqualTo(v2.getVocabId());
        assertThat(restoredLinks.get(1).getOrderIndex()).isEqualTo(2);
        assertThat(restoredLinks.get(2).getVocabulary().getVocabId()).isEqualTo(v3.getVocabId());
        assertThat(restoredLinks.get(2).getOrderIndex()).isEqualTo(3);
    }
}