package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.ApproveLessonRequest;
import com.elearning.dto.request.RejectLessonRequest;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.ModerationQueueResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.ModerationLog;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.ModerationLogRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.ModerationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@DisplayName("Task 6A.2: ModerationService & Audit Trail Integration Tests")
class ModerationServiceIntegrationTests {

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private ModerationLogRepository moderationLogRepository;

    @SpyBean
    private ModerationLogRepository spyModerationLogRepository;

    @Nested
    @Transactional
    @DisplayName("Transactional Moderation Workflow & Audit Trail Tests")
    class ModerationWorkflowAndAuditTests {

        private Account moderatorAccount;
        private Account creatorAccount;
        private Vocabulary vocabXue;
        private Vocabulary vocabSheng;
        private Lesson pendingLesson1;
        private Lesson pendingLesson2;
        private Lesson draftLesson;
        private Lesson approvedLesson;
        private Lesson rejectedLesson;

        @BeforeEach
        void setUp() {
            moderatorAccount = accountRepository.findByEmailOrPhone("mod_6a2@example.com")
                    .orElseGet(() -> {
                        Account acc = new Account();
                        acc.setEmailOrPhone("mod_6a2@example.com");
                        acc.setPasswordHash("$2a$12$dummyhashmod6a21234567890123456");
                        acc.setStatus("Active");
                        return accountRepository.save(acc);
                    });

            creatorAccount = accountRepository.findByEmailOrPhone("creator_6a2@example.com")
                    .orElseGet(() -> {
                        Account acc = new Account();
                        acc.setEmailOrPhone("creator_6a2@example.com");
                        acc.setPasswordHash("$2a$12$dummyhashcreator6a21234567890123");
                        acc.setStatus("Active");
                        return accountRepository.save(acc);
                    });

            vocabXue = vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")
                    .orElseGet(() -> vocabularyRepository.save(new Vocabulary("学", "xué", "xue", "Học", "Học tập")));

            vocabSheng = vocabularyRepository.findByHanziAndPinyinRaw("生", "sheng")
                    .orElseGet(() -> vocabularyRepository.save(new Vocabulary("生", "shēng", "sheng", "Sinh", "Học sinh")));

            // 1. Pending lesson 1 (2 vocabularies)
            pendingLesson1 = new Lesson("Bài Học Chờ Duyệt 1", creatorAccount);
            pendingLesson1.setStatus("Pending");
            pendingLesson1 = lessonRepository.save(pendingLesson1);
            lessonVocabularyRepository.save(new LessonVocabulary(pendingLesson1, vocabXue, 1));
            lessonVocabularyRepository.save(new LessonVocabulary(pendingLesson1, vocabSheng, 2));

            // 2. Pending lesson 2 (1 vocabulary)
            pendingLesson2 = new Lesson("Bài Học Chờ Duyệt 2", creatorAccount);
            pendingLesson2.setStatus("Pending");
            pendingLesson2 = lessonRepository.save(pendingLesson2);
            lessonVocabularyRepository.save(new LessonVocabulary(pendingLesson2, vocabXue, 1));

            // 3. Draft lesson
            draftLesson = new Lesson("Bài Học Nháp", creatorAccount);
            draftLesson.setStatus("Draft");
            draftLesson = lessonRepository.save(draftLesson);

            // 4. Approved lesson
            approvedLesson = new Lesson("Bài Học Đã Duyệt", creatorAccount);
            approvedLesson.setStatus("Approved");
            approvedLesson = lessonRepository.save(approvedLesson);

            // 5. Rejected lesson
            rejectedLesson = new Lesson("Bài Học Bị Từ Chối", creatorAccount);
            rejectedLesson.setStatus("Rejected");
            rejectedLesson = lessonRepository.save(rejectedLesson);

            // Setup security context with moderator
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "mod_6a2@example.com",
                    "password",
                    List.of(new SimpleGrantedAuthority("ROLE_MODERATOR"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("getPendingLessons returns only Pending lessons with vocabularyCount and creator info")
        void testGetPendingLessons_returnsOnlyPending() {
            Page<ModerationQueueResponse> page = moderationService.getPendingLessons(PageRequest.of(0, 10));

            assertThat(page).isNotNull();
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent()).extracting(ModerationQueueResponse::getStatus)
                    .containsOnly("Pending");
            assertThat(page.getContent()).extracting(ModerationQueueResponse::getTitle)
                    .containsExactlyInAnyOrder("Bài Học Chờ Duyệt 1", "Bài Học Chờ Duyệt 2");

            ModerationQueueResponse item1 = page.getContent().stream()
                    .filter(i -> i.getTitle().equals("Bài Học Chờ Duyệt 1"))
                    .findFirst().orElseThrow();
            assertThat(item1.getVocabularyCount()).isEqualTo(2);
            assertThat(item1.getCreatorId()).isEqualTo(creatorAccount.getAccountId());
            assertThat(item1.getCreatorEmail()).isEqualTo("creator_6a2@example.com");

            ModerationQueueResponse item2 = page.getContent().stream()
                    .filter(i -> i.getTitle().equals("Bài Học Chờ Duyệt 2"))
                    .findFirst().orElseThrow();
            assertThat(item2.getVocabularyCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("getPendingLessonById returns full detail with sorted vocabularies")
        void testGetPendingLessonById_success() {
            LessonDetailResponse detail = moderationService.getPendingLessonById(pendingLesson1.getLessonId());

            assertThat(detail).isNotNull();
            assertThat(detail.getLessonId()).isEqualTo(pendingLesson1.getLessonId());
            assertThat(detail.getTitle()).isEqualTo("Bài Học Chờ Duyệt 1");
            assertThat(detail.getStatus()).isEqualTo("Pending");
            assertThat(detail.getVocabularyCount()).isEqualTo(2);
            assertThat(detail.getVocabularies()).hasSize(2);
            assertThat(detail.getVocabularies().get(0).getOrderIndex()).isEqualTo(1);
            assertThat(detail.getVocabularies().get(0).getHanzi()).isEqualTo("学");
            assertThat(detail.getVocabularies().get(1).getOrderIndex()).isEqualTo(2);
            assertThat(detail.getVocabularies().get(1).getHanzi()).isEqualTo("生");
        }

        @Test
        @DisplayName("approveLesson transitions status to Approved and persists audit log in MODERATION_LOG")
        void testApproveLesson_success_persistsAuditLog() {
            ApproveLessonRequest request = new ApproveLessonRequest("Đạt tiêu chuẩn xuất bản");
            LessonDetailResponse response = moderationService.approveLesson(pendingLesson1.getLessonId(), request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("Approved");

            // Verify persistence in MySQL
            Lesson persistedLesson = lessonRepository.findById(pendingLesson1.getLessonId()).orElseThrow();
            assertThat(persistedLesson.getStatus()).isEqualTo("Approved");

            // Verify audit log in MODERATION_LOG
            List<ModerationLog> logs = moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDesc(pendingLesson1.getLessonId());
            assertThat(logs).hasSize(1);
            ModerationLog log = logs.get(0);
            assertThat(log.getAction()).isEqualTo("Approve");
            assertThat(log.getModerator().getAccountId()).isEqualTo(moderatorAccount.getAccountId());
            assertThat(log.getLesson().getLessonId()).isEqualTo(pendingLesson1.getLessonId());
            assertThat(log.getRejectionReason()).isNull();
            assertThat(log.getFlaggedFields()).isNull();
            assertThat(log.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("rejectLesson transitions status to Rejected and persists reason and flaggedFields in MODERATION_LOG")
        void testRejectLesson_success_persistsAuditLog() {
            RejectLessonRequest request = new RejectLessonRequest("Thiếu nghĩa tiếng Việt chính xác", "[\"vocabularies[0].meaningVi\"]");
            LessonDetailResponse response = moderationService.rejectLesson(pendingLesson1.getLessonId(), request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("Rejected");

            // Verify persistence in MySQL
            Lesson persistedLesson = lessonRepository.findById(pendingLesson1.getLessonId()).orElseThrow();
            assertThat(persistedLesson.getStatus()).isEqualTo("Rejected");

            // Verify audit log in MODERATION_LOG
            List<ModerationLog> logs = moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDesc(pendingLesson1.getLessonId());
            assertThat(logs).hasSize(1);
            ModerationLog log = logs.get(0);
            assertThat(log.getAction()).isEqualTo("Reject");
            assertThat(log.getModerator().getAccountId()).isEqualTo(moderatorAccount.getAccountId());
            assertThat(log.getLesson().getLessonId()).isEqualTo(pendingLesson1.getLessonId());
            assertThat(log.getRejectionReason()).isEqualTo("Thiếu nghĩa tiếng Việt chính xác");
            assertThat(log.getFlaggedFields()).isEqualTo("[\"vocabularies[0].meaningVi\"]");
            assertThat(log.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Audit Trail Immutability: Multiple moderation events create append-only audit trail without overwriting")
        void testAuditTrailImmutability_multipleEvents() {
            // 1. Initial Reject
            RejectLessonRequest rejectReq = new RejectLessonRequest("Lý do ban đầu: sai pinyin", "[\"pinyin\"]");
            moderationService.rejectLesson(pendingLesson1.getLessonId(), rejectReq);

            // 2. Creator resubmits to Pending
            pendingLesson1.setStatus("Pending");
            lessonRepository.save(pendingLesson1);

            // 3. Moderator Approves
            ApproveLessonRequest approveReq = new ApproveLessonRequest("Đã sửa tốt");
            moderationService.approveLesson(pendingLesson1.getLessonId(), approveReq);

            // Verify audit trail contains exactly 2 distinct historical entries
            List<ModerationLog> logs = moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDescLogIdDesc(pendingLesson1.getLessonId());
            assertThat(logs).hasSize(2);

            // Most recent event (Approve)
            ModerationLog latestLog = logs.get(0);
            assertThat(latestLog.getAction()).isEqualTo("Approve");
            assertThat(latestLog.getRejectionReason()).isNull();

            // Earlier event (Reject) preserved unmodified
            ModerationLog earlierLog = logs.get(1);
            assertThat(earlierLog.getAction()).isEqualTo("Reject");
            assertThat(earlierLog.getRejectionReason()).isEqualTo("Lý do ban đầu: sai pinyin");
            assertThat(earlierLog.getFlaggedFields()).isEqualTo("[\"pinyin\"]");
        }

        @Test
        @DisplayName("approveLesson on non-pending lessons throws CONFLICT (409) and creates 0 audit logs")
        void testApproveNonPending_throwsConflict_noLogs() {
            ApproveLessonRequest req = new ApproveLessonRequest();
            long initialLogCount = moderationLogRepository.count();

            assertThatThrownBy(() -> moderationService.approveLesson(draftLesson.getLessonId(), req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

            assertThatThrownBy(() -> moderationService.approveLesson(approvedLesson.getLessonId(), req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

            assertThatThrownBy(() -> moderationService.approveLesson(rejectedLesson.getLessonId(), req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

            assertThat(moderationLogRepository.count()).isEqualTo(initialLogCount);
        }

        @Test
        @DisplayName("rejectLesson on non-pending lessons throws CONFLICT (409) and creates 0 audit logs")
        void testRejectNonPending_throwsConflict_noLogs() {
            RejectLessonRequest req = new RejectLessonRequest("Lý do từ chối");
            long initialLogCount = moderationLogRepository.count();

            assertThatThrownBy(() -> moderationService.rejectLesson(draftLesson.getLessonId(), req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

            assertThatThrownBy(() -> moderationService.rejectLesson(approvedLesson.getLessonId(), req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

            assertThatThrownBy(() -> moderationService.rejectLesson(rejectedLesson.getLessonId(), req))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

            assertThat(moderationLogRepository.count()).isEqualTo(initialLogCount);
        }
    }

    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Application Transaction Rollback Tests (Non-Test-Managed)")
    class TransactionRollbackTests {

        private Account testModerator;
        private Account testCreator;
        private Lesson testLesson;

        @BeforeEach
        void setUp() {
            testModerator = accountRepository.findByEmailOrPhone("tx_mod_6a2@example.com")
                    .orElseGet(() -> {
                        Account acc = new Account();
                        acc.setEmailOrPhone("tx_mod_6a2@example.com");
                        acc.setPasswordHash("$2a$12$dummyhashtxmod6a21234567890123");
                        acc.setStatus("Active");
                        return accountRepository.save(acc);
                    });

            testCreator = accountRepository.findByEmailOrPhone("tx_creator_6a2@example.com")
                    .orElseGet(() -> {
                        Account acc = new Account();
                        acc.setEmailOrPhone("tx_creator_6a2@example.com");
                        acc.setPasswordHash("$2a$12$dummyhashtxcreator6a212345678");
                        acc.setStatus("Active");
                        return accountRepository.save(acc);
                    });

            testLesson = new Lesson("Tx Rollback Lesson", testCreator);
            testLesson.setStatus("Pending");
            testLesson = lessonRepository.save(testLesson);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "tx_mod_6a2@example.com",
                    "password",
                    List.of(new SimpleGrantedAuthority("ROLE_MODERATOR"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
            Mockito.reset(spyModerationLogRepository);
            if (testLesson != null && testLesson.getLessonId() != null) {
                moderationLogRepository.deleteAll(moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDesc(testLesson.getLessonId()));
                lessonRepository.deleteById(testLesson.getLessonId());
            }
        }

        @Test
        @DisplayName("GIVEN failure during audit log insertion WHEN approveLesson fails THEN application transaction rolls back lesson status to Pending")
        void testApproveRollback_whenAuditLogFails() {
            // Failure injection: spy throws RuntimeException when saving ModerationLog
            doThrow(new RuntimeException("Simulated I/O database failure during audit log insertion"))
                    .when(spyModerationLogRepository).save(any(ModerationLog.class));

            ApproveLessonRequest request = new ApproveLessonRequest("Duyệt bài");

            assertThatThrownBy(() -> moderationService.approveLesson(testLesson.getLessonId(), request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Simulated I/O database failure");

            // Verify application transaction rollback in MySQL directly
            Lesson persisted = lessonRepository.findById(testLesson.getLessonId()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo("Pending");

            List<ModerationLog> logs = moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDesc(testLesson.getLessonId());
            assertThat(logs).isEmpty();
        }

        @Test
        @DisplayName("GIVEN failure during audit log insertion WHEN rejectLesson fails THEN application transaction rolls back lesson status to Pending")
        void testRejectRollback_whenAuditLogFails() {
            // Failure injection: spy throws RuntimeException when saving ModerationLog
            doThrow(new RuntimeException("Simulated I/O database failure during audit log insertion"))
                    .when(spyModerationLogRepository).save(any(ModerationLog.class));

            RejectLessonRequest request = new RejectLessonRequest("Lý do từ chối", "[\"title\"]");

            assertThatThrownBy(() -> moderationService.rejectLesson(testLesson.getLessonId(), request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Simulated I/O database failure");

            // Verify application transaction rollback in MySQL directly
            Lesson persisted = lessonRepository.findById(testLesson.getLessonId()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo("Pending");

            List<ModerationLog> logs = moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDesc(testLesson.getLessonId());
            assertThat(logs).isEmpty();
        }
    }
}
