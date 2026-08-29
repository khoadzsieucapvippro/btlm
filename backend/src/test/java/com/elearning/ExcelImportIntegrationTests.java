package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.Role;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.security.JwtUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring Security & Database Integration Tests for Task 5C.2 (Excel Import Controller).
 * Validates:
 * - Security matrix (401 unauthenticated, 403 Learner/Moderator, 200/201 Creator/Admin).
 * - Step 1 Preview read-only invariant (zero DB mutation).
 * - Step 2 Confirm atomic transactional persistence (Lesson, Vocabulary, LessonVocabulary).
 * - Rollback on invalid import.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 5C.2: Excel Import Controller Integration Tests")
class ExcelImportIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String creatorToken;
    private String adminToken;
    private String learnerToken;
    private String moderatorToken;

    private Account creatorAccount;
    private Vocabulary existingVocab;

    @BeforeEach
    void setUp() {
        creatorAccount = new Account();
        creatorAccount.setEmailOrPhone("import_creator@example.com");
        creatorAccount.setPasswordHash("$2a$12$dummyhashcreator1234567890123");
        creatorAccount.setStatus("Active");
        creatorAccount = accountRepository.save(creatorAccount);

        for (String email : List.of("import_admin@example.com", "import_learner@example.com", "import_moderator@example.com")) {
            accountRepository.findByEmailOrPhone(email)
                    .orElseGet(() -> {
                        Account acc = new Account();
                        acc.setEmailOrPhone(email);
                        acc.setPasswordHash("$2a$12$dummyhashother12345678901234");
                        acc.setStatus("Active");
                        return accountRepository.save(acc);
                    });
        }

        creatorToken = jwtUtil.generateToken("import_creator@example.com", List.of("Creator"));
        adminToken = jwtUtil.generateToken("import_admin@example.com", List.of("Admin"));
        learnerToken = jwtUtil.generateToken("import_learner@example.com", List.of("Learner"));
        moderatorToken = jwtUtil.generateToken("import_moderator@example.com", List.of("Moderator"));

        // Pre-seed an existing vocabulary in DB
        existingVocab = vocabularyRepository.findByHanziAndPinyinRaw("中", "zhong")
                .orElseGet(() -> vocabularyRepository.save(new Vocabulary("中", "zhōng", "zhong", "Trung", "Ở giữa, trung tâm")));
    }

    private byte[] createWorkbook(String[] headers, String[][] data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("ImportSheet");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            if (data != null) {
                for (int r = 0; r < data.length; r++) {
                    Row dataRow = sheet.createRow(r + 1);
                    for (int c = 0; c < data[r].length; c++) {
                        Cell cell = dataRow.createCell(c);
                        cell.setCellValue(data[r][c]);
                    }
                }
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    @Nested
    @DisplayName("1. Security Boundary Tests")
    class SecurityBoundaryTests {

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN POST /import THEN returns 401 UNAUTHORIZED")
        void testImportPreview_unauthenticated_returns401() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/v1/creator/lessons/import").file(file))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GIVEN Learner or Moderator token WHEN POST /import THEN returns 403 FORBIDDEN")
        void testImportPreview_insufficientRole_returns403() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

            // Learner
            mockMvc.perform(multipart("/api/v1/creator/lessons/import")
                            .file(file)
                            .header("Authorization", "Bearer " + learnerToken))
                    .andExpect(status().isForbidden());

            // Moderator
            mockMvc.perform(multipart("/api/v1/creator/lessons/import")
                            .file(file)
                            .header("Authorization", "Bearer " + moderatorToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN POST /import/confirm THEN returns 401 UNAUTHORIZED")
        void testImportConfirm_unauthenticated_returns401() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài học mới"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GIVEN Learner or Moderator token WHEN POST /import/confirm THEN returns 403 FORBIDDEN")
        void testImportConfirm_insufficientRole_returns403() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

            // Learner
            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài học mới")
                            .header("Authorization", "Bearer " + learnerToken))
                    .andExpect(status().isForbidden());

            // Moderator
            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài học mới")
                            .header("Authorization", "Bearer " + moderatorToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("2. Step 1: Preview Read-Only & Zero Mutation Invariant Tests")
    class PreviewWorkflowTests {

        @Test
        @DisplayName("GIVEN valid Excel file WHEN Creator calls POST /import THEN returns 200 with report and zero DB mutations")
        void testPreview_zeroDbMutations() throws Exception {
            long lessonCountBefore = lessonRepository.count();
            long vocabCountBefore = vocabularyRepository.count();
            long lessonVocabCountBefore = lessonVocabularyRepository.count();

            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {
                    {"中", "zhōng", "Trung", "Ở giữa"},           // Already in DB
                    {"文", "wén", "Văn", "Ngôn ngữ, văn học"}       // New
            };
            byte[] bytes = createWorkbook(headers, data);
            MockMultipartFile file = new MockMultipartFile("file", "preview_test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            mockMvc.perform(multipart("/api/v1/creator/lessons/import")
                            .file(file)
                            .header("Authorization", "Bearer " + creatorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.isValid").value(true))
                    .andExpect(jsonPath("$.data.totalRows").value(2))
                    .andExpect(jsonPath("$.data.validRowsCount").value(2))
                    .andExpect(jsonPath("$.data.existingVocabCount").value(1))
                    .andExpect(jsonPath("$.data.newVocabCount").value(1));

            // Verify strict zero-mutation invariant:
            assertThat(lessonRepository.count()).isEqualTo(lessonCountBefore);
            assertThat(vocabularyRepository.count()).isEqualTo(vocabCountBefore);
            assertThat(lessonVocabularyRepository.count()).isEqualTo(lessonVocabCountBefore);
        }

        @Test
        @DisplayName("GIVEN Admin token WHEN POST /import THEN allowed and succeeds")
        void testPreview_adminAllowed() throws Exception {
            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {{"中", "zhōng", "Trung", "Ở giữa"}};
            byte[] bytes = createWorkbook(headers, data);
            MockMultipartFile file = new MockMultipartFile("file", "admin_preview.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            mockMvc.perform(multipart("/api/v1/creator/lessons/import")
                            .file(file)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.isValid").value(true));
        }
    }

    @Nested
    @DisplayName("3. Step 2: Confirm Transactional Persistence Tests")
    class ConfirmWorkflowTests {

        @Test
        @DisplayName("GIVEN valid Excel file and title WHEN Creator calls POST /import/confirm THEN creates Lesson, Vocabs, and links in MySQL")
        void testConfirm_success_persistsAtomically() throws Exception {
            long lessonCountBefore = lessonRepository.count();
            long vocabCountBefore = vocabularyRepository.count();

            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt", "Câu ví dụ", "Dịch câu ví dụ"};
            String[][] data = {
                    {"中", "zhōng", "Trung", "Ở giữa", "中国很大。", "Trung Quốc rất lớn."},      // Pre-existing
                    {"国", "guó", "Quốc", "Đất nước", "国家。", "Quốc gia."}                    // New
            };
            byte[] bytes = createWorkbook(headers, data);
            MockMultipartFile file = new MockMultipartFile("file", "confirm_test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài 1: Quốc gia")
                            .header("Authorization", "Bearer " + creatorToken))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Import bài học từ Excel thành công"))
                    .andExpect(jsonPath("$.data.lessonId").isNumber())
                    .andExpect(jsonPath("$.data.title").value("Bài 1: Quốc gia"))
                    .andExpect(jsonPath("$.data.status").value("Draft"))
                    .andExpect(jsonPath("$.data.vocabularies").isArray())
                    .andExpect(jsonPath("$.data.vocabularies[0].hanzi").value("中"))
                    .andExpect(jsonPath("$.data.vocabularies[0].orderIndex").value(1))
                    .andExpect(jsonPath("$.data.vocabularies[1].hanzi").value("国"))
                    .andExpect(jsonPath("$.data.vocabularies[1].orderIndex").value(2));

            // Verify DB state:
            assertThat(lessonRepository.count()).isEqualTo(lessonCountBefore + 1);
            assertThat(vocabularyRepository.count()).isEqualTo(vocabCountBefore + 1); // Only 1 new vocab created ("国")

            // Verify creator ownership
            Lesson created = lessonRepository.findAll().stream()
                    .filter(l -> "Bài 1: Quốc gia".equals(l.getTitle()))
                    .findFirst()
                    .orElseThrow();
            assertThat(created.getCreatedBy().getAccountId()).isEqualTo(creatorAccount.getAccountId());
            assertThat(created.getStatus()).isEqualTo("Draft");
        }

        @Test
        @DisplayName("GIVEN invalid Excel data WHEN Creator calls POST /import/confirm THEN returns 422 and rolls back completely")
        void testConfirm_invalidData_rollsBack() throws Exception {
            long lessonCountBefore = lessonRepository.count();
            long vocabCountBefore = vocabularyRepository.count();
            long lessonVocabCountBefore = lessonVocabularyRepository.count();

            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {
                    {"", "cuò", "Thác", "Sai"}, // Missing Hanzi! Invalid row!
            };
            byte[] bytes = createWorkbook(headers, data);
            MockMultipartFile file = new MockMultipartFile("file", "invalid_import.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài học lỗi")
                            .header("Authorization", "Bearer " + creatorToken))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"));

            // Verify strict rollback: zero changes to Lesson, Vocabulary, LessonVocabulary
            assertThat(lessonRepository.count()).isEqualTo(lessonCountBefore);
            assertThat(vocabularyRepository.count()).isEqualTo(vocabCountBefore);
            assertThat(lessonVocabularyRepository.count()).isEqualTo(lessonVocabCountBefore);
        }

        @Test
        @DisplayName("GIVEN oversized Excel (5001 rows) WHEN Creator calls POST /import preview THEN returns 200 with ROW_LIMIT_EXCEEDED and zero mutations")
        void testPreview_oversizedWorkbook_returnsRowLimitExceeded() throws Exception {
            long lessonCountBefore = lessonRepository.count();
            long vocabCountBefore = vocabularyRepository.count();
            long lessonVocabCountBefore = lessonVocabularyRepository.count();

            // Create workbook with 5001 rows
            byte[] bytes;
            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("ImportSheet");
                Row headerRow = sheet.createRow(0);
                String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }
                for (int r = 1; r <= 5001; r++) {
                    Row row = sheet.createRow(r);
                    row.createCell(0).setCellValue("字" + r);
                    row.createCell(1).setCellValue("zi" + r);
                    row.createCell(2).setCellValue("Tự" + r);
                    row.createCell(3).setCellValue("Chữ " + r);
                }
                workbook.write(baos);
                bytes = baos.toByteArray();
            }

            MockMultipartFile file = new MockMultipartFile("file", "oversized_5001.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            mockMvc.perform(multipart("/api/v1/creator/lessons/import")
                            .file(file)
                            .header("Authorization", "Bearer " + creatorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.isValid").value(false))
                    .andExpect(jsonPath("$.data.fileStatus").value("ROW_LIMIT_EXCEEDED"))
                    .andExpect(jsonPath("$.data.summaryMessage").value(org.hamcrest.Matchers.containsString("5000")));

            assertThat(lessonRepository.count()).isEqualTo(lessonCountBefore);
            assertThat(vocabularyRepository.count()).isEqualTo(vocabCountBefore);
            assertThat(lessonVocabularyRepository.count()).isEqualTo(lessonVocabCountBefore);
        }

        @Test
        @DisplayName("GIVEN oversized Excel (5001 rows) WHEN Creator calls POST /import/confirm THEN returns 422 and persists nothing")
        void testConfirm_oversizedWorkbook_returns422AndZeroPersistence() throws Exception {
            long lessonCountBefore = lessonRepository.count();
            long vocabCountBefore = vocabularyRepository.count();
            long lessonVocabCountBefore = lessonVocabularyRepository.count();

            // Create workbook with 5001 rows
            byte[] bytes;
            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("ImportSheet");
                Row headerRow = sheet.createRow(0);
                String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }
                for (int r = 1; r <= 5001; r++) {
                    Row row = sheet.createRow(r);
                    row.createCell(0).setCellValue("字" + r);
                    row.createCell(1).setCellValue("zi" + r);
                    row.createCell(2).setCellValue("Tự" + r);
                    row.createCell(3).setCellValue("Chữ " + r);
                }
                workbook.write(baos);
                bytes = baos.toByteArray();
            }

            MockMultipartFile file = new MockMultipartFile("file", "oversized_5001.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài học quá tải")
                            .header("Authorization", "Bearer " + creatorToken))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("5000")));

            assertThat(lessonRepository.count()).isEqualTo(lessonCountBefore);
            assertThat(vocabularyRepository.count()).isEqualTo(vocabCountBefore);
            assertThat(lessonVocabularyRepository.count()).isEqualTo(lessonVocabCountBefore);
        }
    }
}
