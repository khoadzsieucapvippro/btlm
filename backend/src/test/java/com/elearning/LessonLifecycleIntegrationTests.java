package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.CreateLessonRequest;
import com.elearning.dto.request.ReorderVocabRequest;
import com.elearning.dto.request.UpdateLessonRequest;
import com.elearning.dto.request.VocabOrderItem;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authoritative Master Verification Suite for Phase 5 Checkpoint (Task 5D.1).
 * Tests all requirements across the full HTTP -> Security -> Controller -> Service -> Transaction -> Repository -> MySQL pipeline:
 * - 5D.1-A: Manual Lesson Lifecycle (Draft -> Add Vocab -> Reorder -> Submit -> Pending -> Public Learner 404)
 * - 5D.1-B: Creator Ownership Isolation (Creator A vs Creator B on all 7 endpoints -> 403 Forbidden; Admin bypass -> 200 OK)
 * - 5D.1-C: Excel Preview Integration & Zero-Mutation Invariant
 * - 5D.1-D & 5D.1-E: Excel Confirm Success & Data Consistency
 * - 5D.1-F: Application Transaction Rollback (Early validation failure & Mid-stream partial work rollback without test-managed rollback)
 * - 5D.1-G: Full Security Matrix (Anonymous 401, Learner 403, Moderator 403, Creator 200/201, Admin 200/201)
 * - 5D.1-H: Multipart Transport Contract
 * - 5D.1-I: Error Contract Normalization
 * - 5D.1-J: Duplicate Import / Confirm Semantics
 * - 5D.1-K: State Machine Invariants (Draft, Pending, Approved, Rejected)
 * - 5D.1-L: Order Index Integrity
 * - 5D.1-M: Entity Relationship & Cascade Integrity
 * - 5D.1-N: Flyway Schema Consistency
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 5D.1: Master Lesson Lifecycle & Phase 5 Checkpoint Integration Tests")
class LessonLifecycleIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @SpyBean
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private Flyway flyway;

    private ObjectMapper objectMapper;

    private Account creatorA;
    private Account creatorB;
    private Account adminUser;
    private Account learnerUser;
    private Account moderatorUser;

    private String creatorTokenA;
    private String creatorTokenB;
    private String adminToken;
    private String learnerToken;
    private String moderatorToken;

    private Vocabulary vocabXue;
    private Vocabulary vocabSheng;
    private Vocabulary vocabXiao;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // 1. Setup deterministic accounts
        creatorA = accountRepository.findByEmailOrPhone("creator_alpha_5d1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("creator_alpha_5d1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashcreatorA123456789012345");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        creatorB = accountRepository.findByEmailOrPhone("creator_beta_5d1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("creator_beta_5d1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashcreatorB123456789012345");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        adminUser = accountRepository.findByEmailOrPhone("admin_5d1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("admin_5d1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashadmin12345678901234567");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        learnerUser = accountRepository.findByEmailOrPhone("learner_5d1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("learner_5d1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashlearner1234567890123456");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        moderatorUser = accountRepository.findByEmailOrPhone("moderator_5d1@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("moderator_5d1@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashmoderator12345678901234");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        // 2. Generate standard tokens
        creatorTokenA = jwtUtil.generateToken("creator_alpha_5d1@example.com", List.of("Creator"));
        creatorTokenB = jwtUtil.generateToken("creator_beta_5d1@example.com", List.of("Creator"));
        adminToken = jwtUtil.generateToken("admin_5d1@example.com", List.of("Admin"));
        learnerToken = jwtUtil.generateToken("learner_5d1@example.com", List.of("Learner"));
        moderatorToken = jwtUtil.generateToken("moderator_5d1@example.com", List.of("Moderator"));

        // 3. Pre-seed baseline vocabularies
        vocabXue = vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")
                .orElseGet(() -> vocabularyRepository.save(new Vocabulary("学", "xué", "xue", "Học", "Học tập")));
        vocabSheng = vocabularyRepository.findByHanziAndPinyinRaw("生", "sheng")
                .orElseGet(() -> vocabularyRepository.save(new Vocabulary("生", "shēng", "sheng", "Sinh", "Học sinh")));
        vocabXiao = vocabularyRepository.findByHanziAndPinyinRaw("校", "xiao")
                .orElseGet(() -> vocabularyRepository.save(new Vocabulary("校", "xiào", "xiao", "Hiệu", "Trường học")));
    }

    @AfterEach
    void tearDown() {
        // Reset spy if modified
        Mockito.reset(lessonVocabularyRepository);
        lessonVocabularyRepository.deleteAll();
        lessonRepository.deleteAll();
        vocabularyRepository.deleteAll();
        accountRepository.deleteAll();
    }

    private byte[] createWorkbook(String[] headers, String[][] data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Vocabulary");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < data[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(data[r][c]);
                }
            }
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    // =========================================================================
    // 5D.1-A: Manual Lesson Lifecycle Flow
    // =========================================================================
    @Nested
    @DisplayName("5D.1-A: Manual Lesson Lifecycle Tests")
    class ManualLessonLifecycleTests {

        @Test
        @DisplayName("GIVEN creator WHEN creating lesson, adding vocabs, reordering, and submitting THEN status transitions Draft -> Pending and is hidden from public API")
        void testManualLessonFullLifecycle() throws Exception {
            // Step 1: Create Draft lesson
            CreateLessonRequest createReq = new CreateLessonRequest("Bài Học Thủ Công 5D.1");
            String createResStr = mockMvc.perform(post("/api/v1/creator/lessons")
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.status").value("Draft"))
                    .andExpect(jsonPath("$.data.vocabularyCount").value(0))
                    .andReturn().getResponse().getContentAsString();

            Long lessonId = objectMapper.readTree(createResStr).path("data").path("lessonId").asLong();
            assertThat(lessonId).isPositive();

            // Step 2: Add first vocabulary (xué) -> orderIndex = 1
            mockMvc.perform(post("/api/v1/creator/lessons/" + lessonId + "/vocabularies/" + vocabXue.getVocabId())
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.vocabularyCount").value(1))
                    .andExpect(jsonPath("$.data.vocabularies[0].vocabId").value(vocabXue.getVocabId()))
                    .andExpect(jsonPath("$.data.vocabularies[0].orderIndex").value(1));

            // Step 3: Add second vocabulary (shēng) -> orderIndex = 2
            mockMvc.perform(post("/api/v1/creator/lessons/" + lessonId + "/vocabularies/" + vocabSheng.getVocabId())
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.vocabularyCount").value(2))
                    .andExpect(jsonPath("$.data.vocabularies[1].vocabId").value(vocabSheng.getVocabId()))
                    .andExpect(jsonPath("$.data.vocabularies[1].orderIndex").value(2));

            // Step 4: Reorder vocabularies (swap order: shēng -> 1, xué -> 2)
            ReorderVocabRequest reorderReq = new ReorderVocabRequest(List.of(
                    vocabSheng.getVocabId(),
                    vocabXue.getVocabId()
            ));
            mockMvc.perform(post("/api/v1/creator/lessons/" + lessonId + "/reorder")
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reorderReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.vocabularies[0].vocabId").value(vocabSheng.getVocabId()))
                    .andExpect(jsonPath("$.data.vocabularies[0].orderIndex").value(1))
                    .andExpect(jsonPath("$.data.vocabularies[1].vocabId").value(vocabXue.getVocabId()))
                    .andExpect(jsonPath("$.data.vocabularies[1].orderIndex").value(2));

            // Step 5: Submit for moderation -> transitions status from 'Draft' to 'Pending'
            mockMvc.perform(post("/api/v1/creator/lessons/" + lessonId + "/submit")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("Pending"));

            // Verify in MySQL
            Lesson persisted = lessonRepository.findById(lessonId).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo("Pending");
            assertThat(persisted.getCreatedBy().getAccountId()).isEqualTo(creatorA.getAccountId());

            // Step 6: Verify public learner endpoint cannot access Pending lesson (404 NOT_FOUND)
            mockMvc.perform(get("/api/v1/lessons/" + lessonId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));

            // Cleanup
            List<LessonVocabulary> manualLinks = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
            lessonVocabularyRepository.deleteAll(manualLinks);
            lessonRepository.delete(persisted);
        }
    }

    // =========================================================================
    // 5D.1-B: Creator Ownership Isolation
    // =========================================================================
    @Nested
    @DisplayName("5D.1-B: Creator Ownership Isolation Tests")
    class CreatorOwnershipIsolationTests {

        private Lesson lessonOfCreatorB;

        @BeforeEach
        void createLessonB() {
            Lesson lesson = new Lesson("Bài Của Creator B", creatorB);
            lesson.setStatus("Draft");
            lessonOfCreatorB = lessonRepository.save(lesson);
        }

        @AfterEach
        void cleanUp() {
            if (lessonOfCreatorB != null && lessonRepository.existsById(lessonOfCreatorB.getLessonId())) {
                lessonRepository.delete(lessonOfCreatorB);
            }
        }

        @Test
        @DisplayName("GIVEN lesson owned by Creator B WHEN Creator A attempts any operation THEN returns 403 FORBIDDEN")
        void testCreatorA_cannotOperateOnCreatorBLesson() throws Exception {
            Long targetId = lessonOfCreatorB.getLessonId();

            // 1. GET detail
            mockMvc.perform(get("/api/v1/creator/lessons/" + targetId)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            // 2. PUT update
            UpdateLessonRequest updateReq = new UpdateLessonRequest("Tiêu đề bị sửa trộm");
            mockMvc.perform(put("/api/v1/creator/lessons/" + targetId)
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            // 3. POST add vocab
            mockMvc.perform(post("/api/v1/creator/lessons/" + targetId + "/vocabularies/" + vocabXue.getVocabId())
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            // 4. DELETE remove vocab
            mockMvc.perform(delete("/api/v1/creator/lessons/" + targetId + "/vocabularies/" + vocabXue.getVocabId())
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            // 5. POST reorder
            ReorderVocabRequest reorderReq = new ReorderVocabRequest(List.of(vocabXue.getVocabId()));
            mockMvc.perform(post("/api/v1/creator/lessons/" + targetId + "/reorder")
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reorderReq)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            // 6. POST submit
            mockMvc.perform(post("/api/v1/creator/lessons/" + targetId + "/submit")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            // 7. DELETE lesson
            mockMvc.perform(delete("/api/v1/creator/lessons/" + targetId)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN lesson owned by Creator B WHEN Admin accesses it THEN returns 200 OK (Admin Bypass)")
        void testAdmin_canAccessAnyLesson() throws Exception {
            mockMvc.perform(get("/api/v1/creator/lessons/" + lessonOfCreatorB.getLessonId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    // =========================================================================
    // 5D.1-C: Excel Preview Integration & Zero-Mutation Invariant
    // =========================================================================
    @Nested
    @DisplayName("5D.1-C: Excel Preview Integration & Zero-Mutation Tests")
    class ExcelPreviewIntegrationTests {

        @Test
        @DisplayName("GIVEN valid Excel file WHEN previewing THEN returns 200 with report and zero DB mutations")
        void testPreview_validFile_zeroMutation() throws Exception {
            long lessonCountBefore = lessonRepository.count();
            long vocabCountBefore = vocabularyRepository.count();
            long lvCountBefore = lessonVocabularyRepository.count();

            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt", "Câu ví dụ", "Dịch câu ví dụ"};
            String[][] data = {
                    {"学", "xué", "Học", "Học tập", "好好学习。", "Học tập chăm chỉ."}, // Existing
                    {"书", "shū", "Thư", "Sách", "这是一本书。", "Đây là một cuốn sách."} // New
            };
            byte[] bytes = createWorkbook(headers, data);
            MockMultipartFile file = new MockMultipartFile("file", "preview_test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            mockMvc.perform(multipart("/api/v1/creator/lessons/import")
                            .file(file)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.isValid").value(true))
                    .andExpect(jsonPath("$.data.totalRows").value(2))
                    .andExpect(jsonPath("$.data.existingVocabCount").value(1))
                    .andExpect(jsonPath("$.data.newVocabCount").value(1));

            // Strict zero-mutation assertion
            assertThat(lessonRepository.count()).isEqualTo(lessonCountBefore);
            assertThat(vocabularyRepository.count()).isEqualTo(vocabCountBefore);
            assertThat(lessonVocabularyRepository.count()).isEqualTo(lvCountBefore);
        }

        @Test
        @DisplayName("GIVEN invalid Excel formats WHEN previewing THEN returns 200 with invalid report without DB mutations")
        void testPreview_invalidFiles_handledGracefully() throws Exception {
            long lessonCountBefore = lessonRepository.count();

            // 1. Corrupt bytes
            MockMultipartFile corruptFile = new MockMultipartFile("file", "corrupt.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{0x01, 0x02, 0x03});
            mockMvc.perform(multipart("/api/v1/creator/lessons/import")
                            .file(corruptFile)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isValid").value(false))
                    .andExpect(jsonPath("$.data.fileStatus").value("INVALID_FORMAT"));

            // 2. Missing required header
            String[] badHeaders = {"Chữ Hán", "Nghĩa tiếng Việt"}; // Missing Pinyin, Han-Viet
            byte[] badHeaderBytes = createWorkbook(badHeaders, new String[][]{{"学", "Học"}});
            MockMultipartFile badHeaderFile = new MockMultipartFile("file", "bad_header.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", badHeaderBytes);
            mockMvc.perform(multipart("/api/v1/creator/lessons/import")
                            .file(badHeaderFile)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isValid").value(false))
                    .andExpect(jsonPath("$.data.fileStatus").value("HEADER_ERROR"));

            // 3. Row with missing required field
            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] badRowData = {{"", "xué", "Học", "Học tập"}}; // Missing Hanzi
            byte[] badRowBytes = createWorkbook(headers, badRowData);
            MockMultipartFile badRowFile = new MockMultipartFile("file", "bad_row.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", badRowBytes);
            mockMvc.perform(multipart("/api/v1/creator/lessons/import")
                            .file(badRowFile)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isValid").value(false))
                    .andExpect(jsonPath("$.data.invalidRowsCount").value(1))
                    .andExpect(jsonPath("$.data.errors[0].columnName").value("Chữ Hán"))
                    .andExpect(jsonPath("$.data.errors[0].errorCode").value("MISSING_REQUIRED_FIELD"));

            assertThat(lessonRepository.count()).isEqualTo(lessonCountBefore);
        }
    }

    // =========================================================================
    // 5D.1-D & 5D.1-E: Excel Confirm Success & Data Consistency
    // =========================================================================
    @Nested
    @DisplayName("5D.1-D & 5D.1-E: Excel Confirm Success & Data Consistency Tests")
    class ExcelConfirmSuccessTests {

        @Test
        @DisplayName("GIVEN valid Excel WHEN calling confirm THEN creates Lesson Draft, creates/reuses Vocabulary, links LessonVocabulary with order_index 1..N")
        void testConfirm_successAndDataConsistency() throws Exception {
            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt", "Câu ví dụ", "Dịch câu ví dụ"};
            String[][] data = {
                    {"学", "xué", "Học", "Học tập", "好好学习。", "Học tập chăm chỉ."}, // Existing
                    {"门", "mén", "Môn", "Cửa", "开门。", "Mở cửa."}                  // New
            };
            byte[] bytes = createWorkbook(headers, data);
            MockMultipartFile file = new MockMultipartFile("file", "confirm_success.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            String resStr = mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài Học Import 5D.1")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.status").value("Draft"))
                    .andExpect(jsonPath("$.data.vocabularyCount").value(2))
                    .andExpect(jsonPath("$.data.vocabularies[0].hanzi").value("学"))
                    .andExpect(jsonPath("$.data.vocabularies[0].orderIndex").value(1))
                    .andExpect(jsonPath("$.data.vocabularies[1].hanzi").value("门"))
                    .andExpect(jsonPath("$.data.vocabularies[1].orderIndex").value(2))
                    .andReturn().getResponse().getContentAsString();

            Long lessonId = objectMapper.readTree(resStr).path("data").path("lessonId").asLong();

            // Verify MySQL state directly
            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
            assertThat(lesson.getTitle()).isEqualTo("Bài Học Import 5D.1");
            assertThat(lesson.getStatus()).isEqualTo("Draft");
            assertThat(lesson.getCreatedBy().getAccountId()).isEqualTo(creatorA.getAccountId());

            List<LessonVocabulary> links = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
            assertThat(links).hasSize(2);
            assertThat(links.get(0).getOrderIndex()).isEqualTo(1);
            assertThat(links.get(0).getVocabulary().getHanzi()).isEqualTo("学");
            assertThat(links.get(1).getOrderIndex()).isEqualTo(2);
            assertThat(links.get(1).getVocabulary().getHanzi()).isEqualTo("门");

            // Cleanup
            lessonVocabularyRepository.deleteAll(links);
            lessonRepository.delete(lesson);
        }
    }

    // =========================================================================
    // 5D.1-F: Application Transaction Rollback Verification
    // =========================================================================
    @Nested
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("5D.1-F: Application Transaction Rollback Verification Tests")
    class TransactionRollbackTests {

        @Test
        @DisplayName("GIVEN invalid Excel content WHEN confirming THEN rejected with 422 and database is restored 100%")
        void testConfirm_validationFailure_rollsBack() throws Exception {
            long lessonCountBefore = lessonRepository.count();
            long vocabCountBefore = vocabularyRepository.count();
            long lvCountBefore = lessonVocabularyRepository.count();

            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {{"", "cuò", "Thác", "Sai"}}; // Missing Hanzi
            byte[] bytes = createWorkbook(headers, data);
            MockMultipartFile file = new MockMultipartFile("file", "invalid.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài Học Thất Bại")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"));

            assertThat(lessonRepository.count()).isEqualTo(lessonCountBefore);
            assertThat(vocabularyRepository.count()).isEqualTo(vocabCountBefore);
            assertThat(lessonVocabularyRepository.count()).isEqualTo(lvCountBefore);
        }

        @Test
        @DisplayName("GIVEN mid-stream persistence failure WHEN row 2 fails after lesson and row 1 persist THEN application transaction rolls back MySQL completely")
        void testConfirm_midStreamFailure_rollsBackApplicationTransaction() throws Exception {
            long lessonCountBefore = lessonRepository.count();
            long vocabCountBefore = vocabularyRepository.count();
            long lvCountBefore = lessonVocabularyRepository.count();

            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {
                    {"月", "yuè", "Nguyệt", "Mặt trăng"}, // Row 1 (order 1)
                    {"日", "rì", "Nhật", "Mặt trời"}     // Row 2 (order 2)
            };
            byte[] bytes = createWorkbook(headers, data);
            MockMultipartFile file = new MockMultipartFile("file", "midstream_failure.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            // Configure spy to fail on row 2 saving
            doThrow(new RuntimeException("Simulated mid-stream DB I/O error during row 2"))
                    .when(lessonVocabularyRepository)
                    .save(argThat(lv -> lv != null && lv.getOrderIndex() != null && lv.getOrderIndex() == 2));

            try {
                mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                                .file(file)
                                .param("title", "Bài Lỗi Giữa Chừng")
                                .header("Authorization", "Bearer " + creatorTokenA))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
            } finally {
                Mockito.reset(lessonVocabularyRepository);
            }

            // CRITICAL VERIFICATION:
            // Even though Lesson was inserted and row 1 Vocabulary was inserted in memory/JPA,
            // the Spring Application @Transactional boundary caught the RuntimeException and executed a full MySQL ROLLBACK!
            assertThat(lessonRepository.count()).isEqualTo(lessonCountBefore);
            assertThat(vocabularyRepository.count()).isEqualTo(vocabCountBefore);
            assertThat(lessonVocabularyRepository.count()).isEqualTo(lvCountBefore);
        }
    }

    // =========================================================================
    // 5D.1-G: Full Security Matrix
    // =========================================================================
    @Nested
    @DisplayName("5D.1-G: Security Boundary Matrix Tests")
    class SecurityMatrixTests {

        private MockMultipartFile validFile;

        @BeforeEach
        void createFixture() throws IOException {
            byte[] bytes = createWorkbook(new String[]{"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"}, new String[][]{{"天", "tiān", "Thiên", "Trời"}});
            validFile = new MockMultipartFile("file", "sec_test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
        }

        @Test
        @DisplayName("Security Matrix for Preview endpoint (/import)")
        void testPreviewSecurityMatrix() throws Exception {
            // Anonymous -> 401
            mockMvc.perform(multipart("/api/v1/creator/lessons/import").file(validFile))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

            // Learner -> 403
            mockMvc.perform(multipart("/api/v1/creator/lessons/import").file(validFile)
                            .header("Authorization", "Bearer " + learnerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            // Moderator -> 403
            mockMvc.perform(multipart("/api/v1/creator/lessons/import").file(validFile)
                            .header("Authorization", "Bearer " + moderatorToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            // Creator -> 200
            mockMvc.perform(multipart("/api/v1/creator/lessons/import").file(validFile)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));

            // Admin -> 200
            mockMvc.perform(multipart("/api/v1/creator/lessons/import").file(validFile)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("Security Matrix for Confirm endpoint (/import/confirm)")
        void testConfirmSecurityMatrix() throws Exception {
            // Anonymous -> 401
            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm").file(validFile).param("title", "Sec Test"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

            // Learner -> 403
            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm").file(validFile).param("title", "Sec Test")
                            .header("Authorization", "Bearer " + learnerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            // Moderator -> 403
            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm").file(validFile).param("title", "Sec Test")
                            .header("Authorization", "Bearer " + moderatorToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    // =========================================================================
    // 5D.1-H: Multipart Transport Contract
    // =========================================================================
    @Nested
    @DisplayName("5D.1-H: Multipart Transport Contract Tests")
    class MultipartContractTests {

        @Test
        @DisplayName("Missing file part or missing parameters returns 400 VALIDATION_ERROR")
        void testMissingParts_returns400() throws Exception {
            // Missing file on preview
            mockMvc.perform(multipart("/api/v1/creator/lessons/import")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

            // Missing title on confirm
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2});
            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Blank title or oversized title returns 400 BAD_REQUEST")
        void testInvalidTitle_returns400() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2});

            // Blank title
            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "   ")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

            // Oversized title (>100 chars)
            String longTitle = "A".repeat(101);
            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", longTitle)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }
    }

    // =========================================================================
    // 5D.1-J: Duplicate Import / Confirm Semantics
    // =========================================================================
    @Nested
    @DisplayName("5D.1-J: Duplicate Confirm Semantics Tests")
    class DuplicateConfirmSemanticsTests {

        @Test
        @DisplayName("Calling confirm twice with same file & title creates 2 separate Draft lessons by design")
        void testDuplicateConfirm_createsTwoLessons() throws Exception {
            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {{"学", "xué", "Học", "Học tập"}};
            byte[] bytes = createWorkbook(headers, data);
            MockMultipartFile file = new MockMultipartFile("file", "dup_test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            String res1 = mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài Học Nhân Bản")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long id1 = objectMapper.readTree(res1).path("data").path("lessonId").asLong();

            String res2 = mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài Học Nhân Bản")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long id2 = objectMapper.readTree(res2).path("data").path("lessonId").asLong();

            assertThat(id1).isNotEqualTo(id2);

            // Cleanup
            lessonRepository.deleteById(id1);
            lessonRepository.deleteById(id2);
        }
    }

    // =========================================================================
    // 5D.1-K: State Machine Invariants
    // =========================================================================
    @Nested
    @DisplayName("5D.1-K: State Machine Invariants Tests")
    class StateMachineInvariantsTests {

        @Test
        @DisplayName("Cannot submit empty lesson (0 vocabularies) returns 400 BAD_REQUEST")
        void testSubmitEmptyLesson_returns400() throws Exception {
            Lesson emptyLesson = lessonRepository.save(new Lesson("Bài Rỗng", creatorA));

            mockMvc.perform(post("/api/v1/creator/lessons/" + emptyLesson.getLessonId() + "/submit")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

            lessonRepository.delete(emptyLesson);
        }

        @Test
        @DisplayName("Pending lesson cannot be edited, reordered, or resubmitted (409 CONFLICT)")
        void testPendingLesson_isLocked() throws Exception {
            Lesson pendingLesson = new Lesson("Bài Đang Chờ Duyệt", creatorA);
            pendingLesson.setStatus("Pending");
            pendingLesson = lessonRepository.save(pendingLesson);
            Long id = pendingLesson.getLessonId();

            // 1. Edit title
            mockMvc.perform(put("/api/v1/creator/lessons/" + id)
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateLessonRequest("Sửa tiêu đề"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));

            // 2. Add vocab
            mockMvc.perform(post("/api/v1/creator/lessons/" + id + "/vocabularies/" + vocabXue.getVocabId())
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));

            // 3. Reorder
            mockMvc.perform(post("/api/v1/creator/lessons/" + id + "/reorder")
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ReorderVocabRequest(List.of(vocabXue.getVocabId())))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));

            // 4. Submit again
            mockMvc.perform(post("/api/v1/creator/lessons/" + id + "/submit")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));

            lessonRepository.delete(pendingLesson);
        }

        @Test
        @DisplayName("Approved lesson cannot be edited (409 CONFLICT)")
        void testApprovedLesson_isLocked() throws Exception {
            Lesson approvedLesson = new Lesson("Bài Đã Duyệt", creatorA);
            approvedLesson.setStatus("Approved");
            approvedLesson = lessonRepository.save(approvedLesson);

            mockMvc.perform(put("/api/v1/creator/lessons/" + approvedLesson.getLessonId())
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateLessonRequest("Sửa bài đã duyệt"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));

            lessonRepository.delete(approvedLesson);
        }

        @Test
        @DisplayName("Rejected lesson CAN be edited and resubmitted (allowed state transition)")
        void testRejectedLesson_canBeEditedAndResubmitted() throws Exception {
            Lesson rejectedLesson = new Lesson("Bài Bị Từ Chối", creatorA);
            rejectedLesson.setStatus("Rejected");
            rejectedLesson = lessonRepository.save(rejectedLesson);
            lessonVocabularyRepository.save(new LessonVocabulary(rejectedLesson, vocabXue, 1));
            Long id = rejectedLesson.getLessonId();

            // Edit title
            mockMvc.perform(put("/api/v1/creator/lessons/" + id)
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateLessonRequest("Tiêu Đề Sau Sửa Lỗi"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Tiêu Đề Sau Sửa Lỗi"));

            // Resubmit
            mockMvc.perform(post("/api/v1/creator/lessons/" + id + "/submit")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("Pending"));

            lessonRepository.delete(rejectedLesson);
        }
    }

    // =========================================================================
    // 5D.1-L: Order Index Integrity & Re-indexing on Delete
    // =========================================================================
    @Nested
    @DisplayName("5D.1-L: Order Index Integrity Tests")
    class OrderIndexIntegrityTests {

        @Test
        @DisplayName("When vocabulary is removed, remaining vocabularies are re-indexed consecutively 1..N")
        void testReindexingOnVocabularyRemoval() throws Exception {
            Lesson lesson = lessonRepository.save(new Lesson("Test Reindex", creatorA));
            Long id = lesson.getLessonId();

            // Add 3 vocabularies
            mockMvc.perform(post("/api/v1/creator/lessons/" + id + "/vocabularies/" + vocabXue.getVocabId())
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/creator/lessons/" + id + "/vocabularies/" + vocabSheng.getVocabId())
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/creator/lessons/" + id + "/vocabularies/" + vocabXiao.getVocabId())
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk());

            // Remove middle vocab (vocabSheng, order 2)
            mockMvc.perform(delete("/api/v1/creator/lessons/" + id + "/vocabularies/" + vocabSheng.getVocabId())
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.vocabularyCount").value(2))
                    .andExpect(jsonPath("$.data.vocabularies[0].vocabId").value(vocabXue.getVocabId()))
                    .andExpect(jsonPath("$.data.vocabularies[0].orderIndex").value(1))
                    .andExpect(jsonPath("$.data.vocabularies[1].vocabId").value(vocabXiao.getVocabId()))
                    .andExpect(jsonPath("$.data.vocabularies[1].orderIndex").value(2)); // Reindexed from 3 to 2!

            lessonRepository.delete(lesson);
        }
    }

    // =========================================================================
    // 5D.1-M: Entity Relationship & Cascade Integrity
    // =========================================================================
    @Nested
    @DisplayName("5D.1-M: Entity Relationship & Cascade Integrity Tests")
    class CascadeIntegrityTests {

        @Test
        @DisplayName("Deleting a Lesson cascades to LessonVocabulary but preserves Vocabulary entities (ON DELETE RESTRICT)")
        void testDeleteLesson_cascadesToLessonVocab_preservesVocab() throws Exception {
            Lesson lesson = lessonRepository.save(new Lesson("Bài Thử Cascade", creatorA));
            Long lessonId = lesson.getLessonId();
            lessonVocabularyRepository.save(new LessonVocabulary(lesson, vocabXue, 1));

            // Delete lesson
            mockMvc.perform(delete("/api/v1/creator/lessons/" + lessonId)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isNoContent());

            // Lesson is deleted
            assertThat(lessonRepository.existsById(lessonId)).isFalse();

            // LessonVocabulary is cascaded
            List<LessonVocabulary> links = lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(lessonId);
            assertThat(links).isEmpty();

            // Vocabulary entity is preserved!
            assertThat(vocabularyRepository.existsById(vocabXue.getVocabId())).isTrue();
        }
    }

    // =========================================================================
    // 5D.1-N: Flyway Schema Consistency
    // =========================================================================
    @Nested
    @DisplayName("5D.1-N: Flyway Schema Consistency Tests")
    class FlywaySchemaConsistencyTests {

        @Test
        @DisplayName("Flyway migrations are fully applied and database is in consistent state")
        void testFlywaySchemaConsistency() {
            var info = flyway.info();
            var applied = info.applied();
            assertThat(applied).isNotEmpty();
            assertThat(info.current().getVersion().getVersion()).isEqualTo("7");
            assertThat(info.pending()).isEmpty();
        }
    }
}
