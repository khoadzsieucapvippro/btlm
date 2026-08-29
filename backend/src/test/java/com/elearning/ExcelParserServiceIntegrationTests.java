package com.elearning;

import com.elearning.dto.response.ImportValidationReport;
import com.elearning.dto.response.ParsedVocabularyItem;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.ExcelParserService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real MySQL & Spring Boot Integration Tests for {@link ExcelParserService} (Module 5C).
 * Verifies:
 * - Real database existence lookup against MySQL.
 * - Strict read-only preview invariant (Vocabulary, Lesson, and LessonVocabulary counts unchanged).
 * - Row-level validation reports on real fixtures.
 */
@SpringBootTest
@Transactional
@DisplayName("Task 5C.1: ExcelParserService Integration & Database Invariant Tests")
class ExcelParserServiceIntegrationTests {

    @Autowired
    private ExcelParserService excelParserService;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    private Vocabulary preExistingVocab;

    @BeforeEach
    void setUp() {
        preExistingVocab = vocabularyRepository.save(
                new Vocabulary("家", "jiā", "jia", "Gia", "Gia đình, nhà")
        );
    }

    private byte[] createWorkbook(String[] headers, String[][] data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("VocabularySheet");

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

    @Test
    @DisplayName("GIVEN Excel with pre-existing and new words WHEN parseAndValidate THEN detects existing in MySQL and preserves zero DB mutations")
    void testRealDatabaseLookupAndNoMutation() throws IOException {
        long vocabCountBefore = vocabularyRepository.count();
        long lessonCountBefore = lessonRepository.count();
        long lessonVocabCountBefore = lessonVocabularyRepository.count();

        String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt", "Câu ví dụ", "Dịch câu ví dụ"};
        String[][] data = {
                {"家", "jiā", "Gia", "Gia đình, nhà", "我家在北京。", "Nhà tôi ở Bắc Kinh."},  // Pre-existing in DB
                {"朋", "péng", "Bằng", "Bạn bè", "朋友很好。", "Bạn bè rất tốt."}                 // New word
        };
        byte[] bytes = createWorkbook(headers, data);

        MockMultipartFile file = new MockMultipartFile(
                "file", "vocab_import_test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes
        );

        ImportValidationReport report = excelParserService.parseAndValidate(file);

        // Verification 1: Report correctness
        assertThat(report.getIsValid()).isTrue();
        assertThat(report.getFileStatus()).isEqualTo("VALID");
        assertThat(report.getTotalRows()).isEqualTo(2);
        assertThat(report.getValidRowsCount()).isEqualTo(2);
        assertThat(report.getExistingVocabCount()).isEqualTo(1);
        assertThat(report.getNewVocabCount()).isEqualTo(1);

        ParsedVocabularyItem item1 = report.getRows().get(0);
        assertThat(item1.getHanzi()).isEqualTo("家");
        assertThat(item1.getIsExisting()).isTrue();
        assertThat(item1.getExistingVocabId()).isEqualTo(preExistingVocab.getVocabId());
        assertThat(item1.getPinyinRaw()).isEqualTo("jia");
        assertThat(item1.getExampleSentence()).isEqualTo("我家在北京。");

        ParsedVocabularyItem item2 = report.getRows().get(1);
        assertThat(item2.getHanzi()).isEqualTo("朋");
        assertThat(item2.getIsExisting()).isFalse();
        assertThat(item2.getExistingVocabId()).isNull();
        assertThat(item2.getPinyinRaw()).isEqualTo("peng");

        // Verification 2: Strict No Database Mutation Invariant (Preview step must not persist anything)
        long vocabCountAfter = vocabularyRepository.count();
        long lessonCountAfter = lessonRepository.count();
        long lessonVocabCountAfter = lessonVocabularyRepository.count();

        assertThat(vocabCountAfter).isEqualTo(vocabCountBefore);
        assertThat(lessonCountAfter).isEqualTo(lessonCountBefore);
        assertThat(lessonVocabCountAfter).isEqualTo(lessonVocabCountBefore);
    }

    @Test
    @DisplayName("GIVEN Excel with row-level validation errors WHEN parseAndValidate THEN returns detailed errors and keeps DB intact")
    void testRowValidationErrorsIntegration() throws IOException {
        long vocabCountBefore = vocabularyRepository.count();

        String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
        String[][] data = {
                {"", "hǎo", "Hảo", "Tốt"},               // Missing Hanzi (row 2)
                {"友", "yǒu", "Hữu", "Bạn bè"}            // Valid (row 3)
        };
        byte[] bytes = createWorkbook(headers, data);

        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid_rows.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes
        );

        ImportValidationReport report = excelParserService.parseAndValidate(file);

        assertThat(report.getIsValid()).isFalse();
        assertThat(report.getFileStatus()).isEqualTo("INVALID");
        assertThat(report.getTotalRows()).isEqualTo(2);
        assertThat(report.getValidRowsCount()).isEqualTo(1);
        assertThat(report.getInvalidRowsCount()).isEqualTo(1);
        assertThat(report.getErrors()).hasSize(1);
        assertThat(report.getErrors().get(0).getRowNumber()).isEqualTo(2);
        assertThat(report.getErrors().get(0).getColumnName()).isEqualTo("Chữ Hán");

        assertThat(vocabularyRepository.count()).isEqualTo(vocabCountBefore);
    }
}
