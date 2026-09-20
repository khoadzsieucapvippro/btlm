package com.elearning;

import com.elearning.dto.request.ApproveLessonRequest;
import com.elearning.dto.request.CreateLessonRequest;
import com.elearning.dto.request.RejectLessonRequest;
import com.elearning.dto.request.UpdateLessonRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Master Verification & Integration Test Suite for Content Publishing & Moderation Workflow (Phase 6 Checkpoint).
 * Proves the end-to-end publishing lifecycle:
 * Creator creates Draft -> submits to Pending -> Moderator A rejects -> Creator edits rejected lesson ->
 * Creator resubmits to Pending -> Moderator B approves -> Lesson immediately visible on Public Lesson API.
 * Verifies strict state machine invariants, audit trail immutability, RBAC matrix, and data consistency.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 6C.1: Moderation Workflow Verification & Phase 6 Checkpoint Tests")
class Phase6ModerationWorkflowIntegrationTests {

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

    private Account creatorA;
    private Account creatorB;
    private Account moderatorA;
    private Account moderatorB;
    private Account adminUser;
    private Account learnerUser;

    private String creatorAToken;
    private String creatorBToken;
    private String moderatorAToken;
    private String moderatorBToken;
    private String adminToken;
    private String learnerToken;

    private Vocabulary vocabXue;
    private Vocabulary vocabXi;

    @BeforeEach
    void setUp() {
        // 1. Setup Accounts
        creatorA = accountRepository.findByEmailOrPhone("creator_a_6c1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("creator_a_6c1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashcreatorA6c11234567890123");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        creatorB = accountRepository.findByEmailOrPhone("creator_b_6c1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("creator_b_6c1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashcreatorB6c11234567890123");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        moderatorA = accountRepository.findByEmailOrPhone("mod_a_6c1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("mod_a_6c1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashmodA6c11234567890123456");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        moderatorB = accountRepository.findByEmailOrPhone("mod_b_6c1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("mod_b_6c1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashmodB6c11234567890123456");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        adminUser = accountRepository.findByEmailOrPhone("admin_6c1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("admin_6c1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashadmin6c112345678901234");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        learnerUser = accountRepository.findByEmailOrPhone("learner_6c1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("learner_6c1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashlearner6c1123456789012");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        // 2. Generate Real JWT Tokens
        creatorAToken = jwtUtil.generateToken(creatorA.getEmailOrPhone(), List.of("ROLE_CREATOR"));
        creatorBToken = jwtUtil.generateToken(creatorB.getEmailOrPhone(), List.of("ROLE_CREATOR"));
        moderatorAToken = jwtUtil.generateToken(moderatorA.getEmailOrPhone(), List.of("ROLE_MODERATOR"));
        moderatorBToken = jwtUtil.generateToken(moderatorB.getEmailOrPhone(), List.of("ROLE_MODERATOR"));
        adminToken = jwtUtil.generateToken(adminUser.getEmailOrPhone(), List.of("ROLE_ADMIN"));
        learnerToken = jwtUtil.generateToken(learnerUser.getEmailOrPhone(), List.of("ROLE_LEARNER"));

        // 3. Setup Test Vocabularies
        vocabXue = vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")
                .orElseGet(() -> vocabularyRepository.save(new Vocabulary("学", "xué", "xue", "Học", "Học tập")));

        vocabXi = vocabularyRepository.findByHanziAndPinyinRaw("习", "xi")
                .orElseGet(() -> vocabularyRepository.save(new Vocabulary("习", "xí", "xi", "Tập", "Luyện tập")));
    }

    @Nested
    @DisplayName("Primary Acceptance Scenario: End-to-End Content Publishing & Moderation Lifecycle")
    class PrimaryAcceptanceLifecycleTests {

        @Test
        @DisplayName("FULL LIFECYCLE: Create Draft -> Submit -> Reject -> Edit -> Resubmit -> Approve -> Public API")
        void testFullContentPublishingWorkflow() throws Exception {
            // STEP 1: Creator A creates a Draft Lesson with 2 vocabularies
            CreateLessonRequest createRequest = new CreateLessonRequest();
            createRequest.setTitle("Bài Học Chu Trình Kiểm Duyệt 6C");
            createRequest.setVocabularyIds(List.of(vocabXue.getVocabId(), vocabXi.getVocabId()));

            String createRes = mockMvc.perform(post("/api/v1/creator/lessons")
                            .header("Authorization", "Bearer " + creatorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.status", is("Draft")))
                    .andExpect(jsonPath("$.data.vocabularies", hasSize(2)))
                    .andReturn().getResponse().getContentAsString();

            Long lessonId = objectMapper.readTree(createRes).path("data").path("lessonId").asLong();
            assertThat(lessonId).isNotNull();

            // PRE-APPROVAL CHECK 1 (Draft state): Public API must NOT expose the draft lesson
            mockMvc.perform(get("/api/v1/lessons/{id}", lessonId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));

            // Moderator Queue check (Draft state): must NOT appear in pending queue
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header("Authorization", "Bearer " + moderatorAToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[?(@.lessonId == " + lessonId + ")]").doesNotExist());

            // STEP 2: Creator A submits the Lesson for moderation (Draft -> Pending)
            mockMvc.perform(post("/api/v1/creator/lessons/{id}/submit", lessonId)
                            .header("Authorization", "Bearer " + creatorAToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.status", is("Pending")));

            Lesson lessonAfterSubmit = lessonRepository.findById(lessonId).orElseThrow();
            assertThat(lessonAfterSubmit.getStatus()).isEqualTo("Pending");
            assertThat(lessonAfterSubmit.getCreatedBy().getAccountId()).isEqualTo(creatorA.getAccountId());

            // PRE-APPROVAL CHECK 2 (Pending state): Public API must still hide the pending lesson
            mockMvc.perform(get("/api/v1/lessons/{id}", lessonId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));

            // STEP 3: Moderator A views pending queue and retrieves lesson detail
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header("Authorization", "Bearer " + moderatorAToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[?(@.lessonId == " + lessonId + ")]").exists());

            mockMvc.perform(get("/api/v1/moderator/lessons/{id}", lessonId)
                            .header("Authorization", "Bearer " + moderatorAToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.lessonId", is(lessonId.intValue())))
                    .andExpect(jsonPath("$.data.status", is("Pending")))
                    .andExpect(jsonPath("$.data.vocabularies", hasSize(2)));

            // STEP 4: Moderator A rejects the lesson with feedback and flagged fields
            RejectLessonRequest rejectRequest = new RejectLessonRequest(
                    "Pinyin từ vựng số 2 chưa chuẩn xác, vui lòng kiểm tra lại",
                    "[\"vocabularies[1].pinyin\"]"
            );

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", lessonId)
                            .header("Authorization", "Bearer " + moderatorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rejectRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.message", is("Từ chối bài học thành công")))
                    .andExpect(jsonPath("$.data.status", is("Rejected")));

            Lesson lessonAfterReject = lessonRepository.findById(lessonId).orElseThrow();
            assertThat(lessonAfterReject.getStatus()).isEqualTo("Rejected");

            // Verify Audit Log for Rejection
            List<ModerationLog> logsAfterReject = moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDescLogIdDesc(lessonId);
            assertThat(logsAfterReject).hasSize(1);
            ModerationLog rejectLog = logsAfterReject.get(0);
            assertThat(rejectLog.getAction()).isEqualTo("Reject");
            assertThat(rejectLog.getModerator().getAccountId()).isEqualTo(moderatorA.getAccountId());
            assertThat(rejectLog.getRejectionReason()).isEqualTo("Pinyin từ vựng số 2 chưa chuẩn xác, vui lòng kiểm tra lại");
            assertThat(rejectLog.getFlaggedFields()).isEqualTo("[\"vocabularies[1].pinyin\"]");
            Long rejectLogId = rejectLog.getLogId();

            // PRE-APPROVAL CHECK 3 (Rejected state): Public API must still hide rejected lesson
            mockMvc.perform(get("/api/v1/lessons/{id}", lessonId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));

            // STEP 5: Creator A retrieves own rejected lesson, sees status Rejected
            mockMvc.perform(get("/api/v1/creator/lessons/{id}", lessonId)
                            .header("Authorization", "Bearer " + creatorAToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status", is("Rejected")));

            // STEP 6: Creator A edits the rejected lesson (title modification)
            UpdateLessonRequest updateRequest = new UpdateLessonRequest();
            updateRequest.setTitle("Bài Học Chu Trình 6C Đã Chỉnh Sửa Hoàn Thiện");

            mockMvc.perform(put("/api/v1/creator/lessons/{id}", lessonId)
                            .header("Authorization", "Bearer " + creatorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.title", is("Bài Học Chu Trình 6C Đã Chỉnh Sửa Hoàn Thiện")))
                    .andExpect(jsonPath("$.data.status", is("Rejected")));

            Lesson lessonAfterEdit = lessonRepository.findById(lessonId).orElseThrow();
            assertThat(lessonAfterEdit.getTitle()).isEqualTo("Bài Học Chu Trình 6C Đã Chỉnh Sửa Hoàn Thiện");
            assertThat(lessonAfterEdit.getStatus()).isEqualTo("Rejected");
            assertThat(lessonAfterEdit.getCreatedBy().getAccountId()).isEqualTo(creatorA.getAccountId());

            // STEP 7: Creator A resubmits the edited lesson (Rejected -> Pending)
            mockMvc.perform(post("/api/v1/creator/lessons/{id}/submit", lessonId)
                            .header("Authorization", "Bearer " + creatorAToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.status", is("Pending")));

            Lesson lessonAfterResubmit = lessonRepository.findById(lessonId).orElseThrow();
            assertThat(lessonAfterResubmit.getStatus()).isEqualTo("Pending");

            // STEP 8: Moderator B retrieves queue and approves the resubmitted lesson
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header("Authorization", "Bearer " + moderatorBToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[?(@.lessonId == " + lessonId + ")]").exists());

            ApproveLessonRequest approveRequest = new ApproveLessonRequest("Nội dung đã được sửa chuẩn xác, duyệt xuất bản");

            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", lessonId)
                            .header("Authorization", "Bearer " + moderatorBToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approveRequest))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.message", is("Phê duyệt bài học thành công")))
                    .andExpect(jsonPath("$.data.status", is("Approved")));

            Lesson lessonAfterApprove = lessonRepository.findById(lessonId).orElseThrow();
            assertThat(lessonAfterApprove.getStatus()).isEqualTo("Approved");
            assertThat(lessonAfterApprove.getCreatedBy().getAccountId()).isEqualTo(creatorA.getAccountId());

            // STEP 9: Audit Immutability Check - 2 distinct records with correct actor attribution
            List<ModerationLog> allLogs = moderationLogRepository.findByLesson_LessonIdOrderByCreatedAtDescLogIdDesc(lessonId);
            assertThat(allLogs).hasSize(2);

            // Latest log: Approve by Moderator B
            ModerationLog latestApproveLog = allLogs.get(0);
            assertThat(latestApproveLog.getAction()).isEqualTo("Approve");
            assertThat(latestApproveLog.getModerator().getAccountId()).isEqualTo(moderatorB.getAccountId());
            assertThat(latestApproveLog.getRejectionReason()).isNull();

            // Older log: Original Reject by Moderator A (IMMUTABLE!)
            ModerationLog preservedRejectLog = allLogs.get(1);
            assertThat(preservedRejectLog.getLogId()).isEqualTo(rejectLogId);
            assertThat(preservedRejectLog.getAction()).isEqualTo("Reject");
            assertThat(preservedRejectLog.getModerator().getAccountId()).isEqualTo(moderatorA.getAccountId());
            assertThat(preservedRejectLog.getRejectionReason()).isEqualTo("Pinyin từ vựng số 2 chưa chuẩn xác, vui lòng kiểm tra lại");
            assertThat(preservedRejectLog.getFlaggedFields()).isEqualTo("[\"vocabularies[1].pinyin\"]");

            // STEP 10: POST-APPROVAL PUBLIC LESSON API VERIFICATION (Final Critical Acceptance)
            // 10.1 Detail endpoint GET /api/v1/lessons/{id}
            mockMvc.perform(get("/api/v1/lessons/{id}", lessonId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.lessonId", is(lessonId.intValue())))
                    .andExpect(jsonPath("$.data.title", is("Bài Học Chu Trình 6C Đã Chỉnh Sửa Hoàn Thiện")))
                    .andExpect(jsonPath("$.data.status", is("Approved")))
                    .andExpect(jsonPath("$.data.vocabularyCount", is(2)))
                    .andExpect(jsonPath("$.data.vocabularies[0].hanzi", is("学")))
                    .andExpect(jsonPath("$.data.vocabularies[0].orderIndex", is(1)))
                    .andExpect(jsonPath("$.data.vocabularies[1].hanzi", is("习")))
                    .andExpect(jsonPath("$.data.vocabularies[1].orderIndex", is(2)));

            // 10.2 Public Catalog list endpoint GET /api/v1/lessons
            mockMvc.perform(get("/api/v1/lessons")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.items[?(@.lessonId == " + lessonId + ")]").exists());
        }
    }

    @Nested
    @DisplayName("Validation Boundary & Failed Operation Tests")
    class ValidationBoundaryTests {

        private Lesson pendingLesson;

        @BeforeEach
        void initPendingLesson() {
            pendingLesson = new Lesson("Pending Lesson for Validation Test", creatorA);
            pendingLesson.setStatus("Pending");
            pendingLesson = lessonRepository.save(pendingLesson);
            lessonVocabularyRepository.save(new LessonVocabulary(pendingLesson, vocabXue, 1));
        }

        @Test
        @DisplayName("GIVEN blank rejectionReason WHEN rejecting THEN returns 400 VALIDATION_ERROR, no DB state change, no audit log")
        void testReject_blankReason_validationError() throws Exception {
            long initialLogCount = moderationLogRepository.count();

            RejectLessonRequest request = new RejectLessonRequest("   ", "[]");
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            Lesson persisted = lessonRepository.findById(pendingLesson.getLessonId()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo("Pending");
            assertThat(moderationLogRepository.count()).isEqualTo(initialLogCount);
        }

        @Test
        @DisplayName("GIVEN non-existent lesson ID WHEN approving THEN returns 404 NOT_FOUND")
        void testApprove_nonExistentLesson_notFound() throws Exception {
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", 999999L)
                            .header("Authorization", "Bearer " + moderatorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        }
    }

    @Nested
    @DisplayName("State Machine Invariants & Forbidden Lifecycle Transitions")
    class StateMachineInvariantTests {

        private Lesson draftLesson;
        private Lesson pendingLesson;
        private Lesson rejectedLesson;
        private Lesson approvedLesson;

        @BeforeEach
        void initLessons() {
            draftLesson = lessonRepository.save(new Lesson("Draft 6C", creatorA));
            lessonVocabularyRepository.save(new LessonVocabulary(draftLesson, vocabXue, 1));

            pendingLesson = new Lesson("Pending 6C", creatorA);
            pendingLesson.setStatus("Pending");
            pendingLesson = lessonRepository.save(pendingLesson);
            lessonVocabularyRepository.save(new LessonVocabulary(pendingLesson, vocabXue, 1));

            rejectedLesson = new Lesson("Rejected 6C", creatorA);
            rejectedLesson.setStatus("Rejected");
            rejectedLesson = lessonRepository.save(rejectedLesson);
            lessonVocabularyRepository.save(new LessonVocabulary(rejectedLesson, vocabXue, 1));

            approvedLesson = new Lesson("Approved 6C", creatorA);
            approvedLesson.setStatus("Approved");
            approvedLesson = lessonRepository.save(approvedLesson);
            lessonVocabularyRepository.save(new LessonVocabulary(approvedLesson, vocabXue, 1));
        }

        @Test
        @DisplayName("FORBIDDEN: Approving a Draft lesson directly returns 409 CONFLICT")
        void testApproveDraft_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", draftLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")));

            assertThat(lessonRepository.findById(draftLesson.getLessonId()).orElseThrow().getStatus()).isEqualTo("Draft");
        }

        @Test
        @DisplayName("FORBIDDEN: Rejecting a Draft lesson directly returns 409 CONFLICT")
        void testRejectDraft_forbidden() throws Exception {
            RejectLessonRequest request = new RejectLessonRequest("Lý do", null);
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", draftLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")));

            assertThat(lessonRepository.findById(draftLesson.getLessonId()).orElseThrow().getStatus()).isEqualTo("Draft");
        }

        @Test
        @DisplayName("FORBIDDEN: Approving a Rejected lesson directly without resubmission returns 409 CONFLICT")
        void testApproveRejected_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", rejectedLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")));

            assertThat(lessonRepository.findById(rejectedLesson.getLessonId()).orElseThrow().getStatus()).isEqualTo("Rejected");
        }

        @Test
        @DisplayName("FORBIDDEN: Rejecting an Approved lesson returns 409 CONFLICT")
        void testRejectApproved_forbidden() throws Exception {
            RejectLessonRequest request = new RejectLessonRequest("Lý do", null);
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", approvedLesson.getLessonId())
                            .header("Authorization", "Bearer " + moderatorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")));

            assertThat(lessonRepository.findById(approvedLesson.getLessonId()).orElseThrow().getStatus()).isEqualTo("Approved");
        }

        @Test
        @DisplayName("FORBIDDEN: Double-submitting a Pending lesson returns 409 CONFLICT")
        void testDoubleSubmitPending_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/creator/lessons/{id}/submit", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + creatorAToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")));

            assertThat(lessonRepository.findById(pendingLesson.getLessonId()).orElseThrow().getStatus()).isEqualTo("Pending");
        }

        @Test
        @DisplayName("FORBIDDEN: Editing a Pending lesson while under moderation returns 409 CONFLICT")
        void testEditPending_forbidden() throws Exception {
            UpdateLessonRequest request = new UpdateLessonRequest();
            request.setTitle("Sửa tiêu đề khi đang pending");

            mockMvc.perform(put("/api/v1/creator/lessons/{id}", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + creatorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")));
        }

        @Test
        @DisplayName("FORBIDDEN: Editing an Approved published lesson returns 409 CONFLICT")
        void testEditApproved_forbidden() throws Exception {
            UpdateLessonRequest request = new UpdateLessonRequest();
            request.setTitle("Sửa tiêu đề khi đã approved");

            mockMvc.perform(put("/api/v1/creator/lessons/{id}", approvedLesson.getLessonId())
                            .header("Authorization", "Bearer " + creatorAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")));
        }
    }

    @Nested
    @DisplayName("Creator Ownership Isolation & Multi-Actor Security")
    class OwnershipIsolationTests {

        private Lesson creatorALesson;

        @BeforeEach
        void initLesson() {
            creatorALesson = lessonRepository.save(new Lesson("Bài Học Của Creator A", creatorA));
            lessonVocabularyRepository.save(new LessonVocabulary(creatorALesson, vocabXue, 1));
        }

        @Test
        @DisplayName("Creator B cannot submit Creator A's lesson -> 403 FORBIDDEN")
        void testCreatorB_submitCreatorA_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/creator/lessons/{id}/submit", creatorALesson.getLessonId())
                            .header("Authorization", "Bearer " + creatorBToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("Creator B cannot edit Creator A's lesson -> 403 FORBIDDEN")
        void testCreatorB_editCreatorA_forbidden() throws Exception {
            UpdateLessonRequest request = new UpdateLessonRequest();
            request.setTitle("Creator B cố sửa");

            mockMvc.perform(put("/api/v1/creator/lessons/{id}", creatorALesson.getLessonId())
                            .header("Authorization", "Bearer " + creatorBToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("Admin can submit/manage any creator's lesson -> 200 OK")
        void testAdmin_manageCreatorA_allowed() throws Exception {
            mockMvc.perform(post("/api/v1/creator/lessons/{id}/submit", creatorALesson.getLessonId())
                            .header("Authorization", "Bearer " + adminToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.status", is("Pending")));

            // Ensure ownership is still Creator A
            Lesson persisted = lessonRepository.findById(creatorALesson.getLessonId()).orElseThrow();
            assertThat(persisted.getCreatedBy().getAccountId()).isEqualTo(creatorA.getAccountId());
        }
    }

    @Nested
    @DisplayName("RBAC Security Boundary Matrix across Moderation Endpoints")
    class RbacSecurityMatrixTests {

        private Lesson pendingLesson;

        @BeforeEach
        void initPendingLesson() {
            pendingLesson = new Lesson("Pending for RBAC Security Test", creatorA);
            pendingLesson.setStatus("Pending");
            pendingLesson = lessonRepository.save(pendingLesson);
            lessonVocabularyRepository.save(new LessonVocabulary(pendingLesson, vocabXue, 1));
        }

        @Test
        @DisplayName("RBAC: Anonymous request on /pending queue returns 401 UNAUTHORIZED")
        void testAnonymous_pendingQueue_unauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("RBAC: Learner request on /pending queue returns 403 FORBIDDEN")
        void testLearner_pendingQueue_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header("Authorization", "Bearer " + learnerToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("RBAC: Creator request on /pending queue returns 403 FORBIDDEN")
        void testCreator_pendingQueue_forbidden() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/lessons/pending")
                            .header("Authorization", "Bearer " + creatorAToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("RBAC: Learner request on /approve returns 403 FORBIDDEN")
        void testLearner_approve_forbidden() throws Exception {
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/approve", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("RBAC: Learner request on /reject returns 403 FORBIDDEN")
        void testLearner_reject_forbidden() throws Exception {
            RejectLessonRequest request = new RejectLessonRequest("Lý do", null);
            mockMvc.perform(post("/api/v1/moderator/lessons/{id}/reject", pendingLesson.getLessonId())
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }
    }
}
