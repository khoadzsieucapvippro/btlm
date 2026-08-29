package com.elearning;

import com.elearning.dto.request.ApproveLessonRequest;
import com.elearning.dto.request.RejectLessonRequest;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.ModerationLog;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.ModerationLogRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-End MockMvc Integration Tests for Content Moderation REST Endpoints (Module 6B).
 * Verifies Checkpoint 6B:
 * - Moderator / Admin view pending queue, approve lessons, reject lessons with audit logging.
 * - RBAC Security Matrix: 401 for anonymous, 403 for Learner & Creator, 200 for Moderator & Admin.
 * - Database persistence & state machine mutations in MySQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 6B.1: ModeratorController MockMvc Integration & Checkpoint 6B Tests")
class ModeratorIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private ModerationLogRepository moderationLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Account moderatorAccount;
    private Account adminAccount;
    private Account creatorAccount;
    private Account learnerAccount;

    private String moderatorToken;
    private String adminToken;
    private String creatorToken;
    private String learnerToken;

    private Lesson pendingLesson;
    private Lesson draftLesson;
    private Lesson approvedLesson;
    private Lesson rejectedLesson;
    private Vocabulary vocabXue;

    @BeforeEach
    void setUp() {
        // 1. Setup accounts with respective roles
        moderatorAccount = accountRepository.findByEmailOrPhone("mod_6b1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("mod_6b1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashmod6b11234567890123456");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        adminAccount = accountRepository.findByEmailOrPhone("admin_6b1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("admin_6b1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashadmin6b112345678901234");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        creatorAccount = accountRepository.findByEmailOrPhone("creator_6b1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("creator_6b1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashcreator6b11234567890123");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        learnerAccount = accountRepository.findByEmailOrPhone("learner_6b1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("learner_6b1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashlearner6b1123456789012");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        // 2. Generate real JWT tokens
        moderatorToken = jwtUtil.generateToken(moderatorAccount.getEmailOrPhone(), List.of("ROLE_MODERATOR"));
        adminToken = jwtUtil.generateToken(adminAccount.getEmailOrPhone(), List.of("ROLE_ADMIN"));
        creatorToken = jwtUtil.generateToken(creatorAccount.getEmailOrPhone(), List.of("ROLE_CREATOR"));
        learnerToken = jwtUtil.generateToken(learnerAccount.getEmailOrPhone(), List.of("ROLE_LEARNER"));

        // 3. Setup test vocabulary
        vocabXue = vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")
                .orElseGet(() -> vocabularyRepository.save(new Vocabulary("学", "xué", "xue", "Học", "Học tập")));

        // 4. Setup lessons with various states
        pendingLesson = new Lesson("Bài Học Đang Chờ Duyệt 6B", creatorAccount);
        pendingLesson.setStatus("Pending");
        pendingLesson = lessonRepository.save(pendingLesson);
        lessonVocabularyRepository.save(new LessonVocabulary(pendingLesson, vocabXue, 1));

        draftLesson = new Lesson("Bài Học Nháp 6B", creatorAccount);
        draftLesson.setStatus("Draft");
        draftLesson = lessonRepository.save(draftLesson);

        approvedLesson = new Lesson("Bài Học Đã Duyệt 6B", creatorAccount);
        approvedLesson.setStatus("Approved");
        approvedLesson = lessonRepository.save(approvedLesson);

        rejectedLesson = new Lesson("Bài Học Bị Từ Chối 6B", creatorAccount);
        rejectedLesson.setStatus("Rejected");
        rejectedLesson = lessonRepository.save(rejectedLesson);
    }

    @Nested
    @DisplayName("GET /api/v1/moderator/lessons/pending - Queue Retrieval & Security")
    class GetPendingQueueTests {

        @Test
        @DisplayName("Moderator retrieves pending lessons queue (only Pending lessons returned)")
        void testModerator_getPendingQueue_success() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header("Authorization", "Bearer " + moderatorToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[?(@.title == 'Bài Học Đang Chờ Duyệt 6B')]").exists())
                    .andExpect(jsonPath("$.data.items[?(@.title == 'Bài Học Nháp 6B')]").doesNotExist())
                    .andExpect(jsonPath("$.data.items[?(@.title == 'Bài Học Đã Duyệt 6B')]").doesNotExist())
                    .andExpect(jsonPath("$.data.items[?(@.title == 'Bài Học Bị Từ Chối 6B')]").doesNotExist());
        }

        @Test
        @DisplayName("Admin retrieves pending lessons queue")
        void testAdmin_getPendingQueue_success() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header("Authorization", "Bearer " + adminToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")));
        }

        @Test
        @DisplayName("Learner calling pending queue is blocked with 403 Forbidden")
        void testLearner_getPendingQueue_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header("Authorization", "Bearer " + learnerToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("Creator calling pending queue is blocked with 403 Forbidden")
        void testCreator_getPendingQueue_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header("Authorization", "Bearer " + creatorToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("Anonymous calling pending queue is blocked with 401 Unauthorized")
        void testAnonymous_getPendingQueue_unauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/moderator/lessons/{id} - Lesson Detail Review")
    class GetPendingDetailTests {

        @Test
        @DisplayName("Moderator retrieves detail of pending lesson")
        void testModerator_getPendingDetail_success() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/{id}", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.lessonId", is(pendingLesson.getLessonId().intValue())))
                    .andExpect(jsonPath("$.data.title", is("Bài Học Đang Chờ Duyệt 6B")))
                    .andExpect(jsonPath("$.data.status", is("Pending")))
                    .andExpect(jsonPath("$.data.vocabularies", hasSize(1)))
                    .andExpect(jsonPath("$.data.vocabularies[0].hanzi", is("学")));
        }

        @Test
        @DisplayName("Moderator retrieves nonexistent lesson ID returns 404 NOT_FOUND")
        void testModerator_getPendingDetail_notFound() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/{id}", 999999L)
                            .header("Authorization", "Bearer " + moderatorToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        }

        @Test
        @DisplayName("Moderator retrieves Draft lesson via pending-detail returns 404 NOT_FOUND and zero data leak (BE-AUTHZ-001)")
        void testModerator_getPendingDetail_draft_returns404AndZeroLeak() throws Exception {
            long logCountBefore = moderationLogRepository.count();

            mockMvc.perform(get("/api/v1/moderator/lessons/{id}", draftLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.message", is("Không tìm thấy bài học với ID: " + draftLesson.getLessonId())));

            // Zero mutation assertion
            assertThat(moderationLogRepository.count()).isEqualTo(logCountBefore);
            assertThat(lessonRepository.findById(draftLesson.getLessonId()).orElseThrow().getStatus()).isEqualTo("Draft");
        }

        @Test
        @DisplayName("Moderator retrieves Approved lesson via pending-detail returns 404 NOT_FOUND and zero data leak (BE-AUTHZ-001)")
        void testModerator_getPendingDetail_approved_returns404AndZeroLeak() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/{id}", approvedLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.message", is("Không tìm thấy bài học với ID: " + approvedLesson.getLessonId())));
        }

        @Test
        @DisplayName("Moderator retrieves Rejected lesson via pending-detail returns 404 NOT_FOUND and zero data leak (BE-AUTHZ-001)")
        void testModerator_getPendingDetail_rejected_returns404AndZeroLeak() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/{id}", rejectedLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.message", is("Không tìm thấy bài học với ID: " + rejectedLesson.getLessonId())));
        }

        @Test
        @DisplayName("Admin retrieves Draft lesson via pending-detail returns 404 NOT_FOUND (BE-AUTHZ-001)")
        void testAdmin_getPendingDetail_draft_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/{id}", draftLesson.getLessonId())
                            .header("Authorization", "Bearer " + adminToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/moderator/lessons/{id}/approve - Lesson Approval & Audit")
    class ApproveLessonTests {

        @Test
        @DisplayName("Moderator approves Pending lesson -> 200 OK, DB status Approved, MODERATION_LOG saved")
        void testModerator_approvePendingLesson_success() throws Exception {
            ApproveLessonRequest request = new ApproveLessonRequest("Nội dung bài học rất tốt");

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.message", is("Phê duyệt bài học thành công")))
                    .andExpect(jsonPath("$.data.status", is("Approved")));

            // Verify database state
            Lesson persistedLesson = lessonRepository.findById(pendingLesson.getLessonId()).orElseThrow();
            assertThat(persistedLesson.getStatus()).isEqualTo("Approved");

            // Verify audit log in MODERATION_LOG
            List<ModerationLog> logs = moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDescLogIdDesc(pendingLesson.getLessonId());
            assertThat(logs).hasSize(1);
            ModerationLog log = logs.get(0);
            assertThat(log.getAction()).isEqualTo("Approve");
            assertThat(log.getModerator().getAccountId()).isEqualTo(moderatorAccount.getAccountId());
            assertThat(log.getLesson().getLessonId()).isEqualTo(pendingLesson.getLessonId());
            assertThat(log.getRejectionReason()).isNull();
            assertThat(log.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Admin approves Pending lesson -> 200 OK")
        void testAdmin_approvePendingLesson_success() throws Exception {
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.status", is("Approved")));
        }

        @Test
        @DisplayName("Moderator approving non-pending lesson returns 409 CONFLICT")
        void testModerator_approveDraftLesson_conflict() throws Exception {
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", draftLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")));
        }

        @Test
        @DisplayName("Learner attempting to approve lesson is blocked with 403 Forbidden")
        void testLearner_approveLesson_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("Creator attempting to approve lesson is blocked with 403 Forbidden")
        void testCreator_approveLesson_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + creatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("Anonymous attempting to approve lesson is blocked with 401 Unauthorized")
        void testAnonymous_approveLesson_unauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", pendingLesson.getLessonId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/moderator/lessons/{id}/reject - Lesson Rejection & Audit")
    class RejectLessonTests {

        @Test
        @DisplayName("Moderator rejects Pending lesson with reason -> 200 OK, DB status Rejected, MODERATION_LOG saved")
        void testModerator_rejectPendingLesson_success() throws Exception {
            RejectLessonRequest request = new RejectLessonRequest("Nghĩa tiếng Việt chưa chính xác", "[\"vocabularies[0].meaningVi\"]");

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.message", is("Từ chối bài học thành công")))
                    .andExpect(jsonPath("$.data.status", is("Rejected")));

            // Verify database state
            Lesson persistedLesson = lessonRepository.findById(pendingLesson.getLessonId()).orElseThrow();
            assertThat(persistedLesson.getStatus()).isEqualTo("Rejected");

            // Verify audit log in MODERATION_LOG
            List<ModerationLog> logs = moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDescLogIdDesc(pendingLesson.getLessonId());
            assertThat(logs).hasSize(1);
            ModerationLog log = logs.get(0);
            assertThat(log.getAction()).isEqualTo("Reject");
            assertThat(log.getModerator().getAccountId()).isEqualTo(moderatorAccount.getAccountId());
            assertThat(log.getLesson().getLessonId()).isEqualTo(pendingLesson.getLessonId());
            assertThat(log.getRejectionReason()).isEqualTo("Nghĩa tiếng Việt chưa chính xác");
            assertThat(log.getFlaggedFields()).isEqualTo("[\"vocabularies[0].meaningVi\"]");
            assertThat(log.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Admin rejects Pending lesson with reason -> 200 OK")
        void testAdmin_rejectPendingLesson_success() throws Exception {
            RejectLessonRequest request = new RejectLessonRequest("Admin từ chối bài này", null);

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.status", is("Rejected")));
        }

        @Test
        @DisplayName("Moderator rejecting without reason returns 400 VALIDATION_ERROR")
        void testModerator_rejectWithoutReason_validationError() throws Exception {
            RejectLessonRequest request = new RejectLessonRequest("", null);

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("Moderator rejecting non-pending lesson returns 409 CONFLICT")
        void testModerator_rejectApprovedLesson_conflict() throws Exception {
            RejectLessonRequest request = new RejectLessonRequest("Bài đã duyệt rồi", null);

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", approvedLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")));
        }

        @Test
        @DisplayName("Learner attempting to reject lesson is blocked with 403 Forbidden")
        void testLearner_rejectLesson_forbidden() throws Exception {
            RejectLessonRequest request = new RejectLessonRequest("Lý do", null);

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("Creator attempting to reject lesson is blocked with 403 Forbidden")
        void testCreator_rejectLesson_forbidden() throws Exception {
            RejectLessonRequest request = new RejectLessonRequest("Lý do", null);

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + creatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("Anonymous attempting to reject lesson is blocked with 401 Unauthorized")
        void testAnonymous_rejectLesson_unauthorized() throws Exception {
            RejectLessonRequest request = new RejectLessonRequest("Lý do", null);

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", pendingLesson.getLessonId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }
    }
}
