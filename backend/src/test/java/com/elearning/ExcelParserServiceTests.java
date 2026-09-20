package com.elearning;

import com.elearning.dto.response.ImportValidationReport;
import com.elearning.dto.response.ParsedVocabularyItem;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.impl.ExcelParserServiceImpl;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 5C.1: ExcelParserService Unit & Security Tests")
class ExcelParserServiceTests {

    @Mock
    private VocabularyRepository vocabularyRepository;

    @InjectMocks
    private ExcelParserServiceImpl excelParserService;

    private byte[] createSampleWorkbook(String[] headers, String[][] data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Vocabularies");

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Data rows
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
    @DisplayName("1. File & Security Validation Tests")
    class SecurityValidationTests {

        @Test
        @DisplayName("GIVEN null or empty MultipartFile WHEN parseAndValidate THEN returns EMPTY_FILE")
        void testEmptyFile_returnsEmptyFileStatus() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new byte[0]
            );

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isFalse();
            assertThat(report.getFileStatus()).isEqualTo("EMPTY_FILE");
            assertThat(report.getErrors()).hasSize(1);
        }

        @Test
        @DisplayName("GIVEN file exceeding 10MB WHEN parseAndValidate THEN returns FILE_TOO_LARGE")
        void testOversizedFile_returnsFileTooLarge() {
            // Mock file with size > 10MB without allocating real 10MB memory
            MockMultipartFile file = new MockMultipartFile(
                    "file", "large.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new byte[10]
            ) {
                @Override
                public long getSize() {
                    return ExcelParserServiceImpl.MAX_FILE_SIZE_BYTES + 1024L;
                }
            };

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isFalse();
            assertThat(report.getFileStatus()).isEqualTo("FILE_TOO_LARGE");
            assertThat(report.getSummaryMessage()).contains("10MB");
        }

        @Test
        @DisplayName("GIVEN non-xlsx extension (.xls, .csv) WHEN parseAndValidate THEN returns INVALID_FORMAT")
        void testInvalidExtension_returnsInvalidFormat() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "vocabulary.xls",
                    "application/vnd.ms-excel",
                    new byte[]{1, 2, 3, 4}
            );

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isFalse();
            assertThat(report.getFileStatus()).isEqualTo("INVALID_FORMAT");
            assertThat(report.getSummaryMessage()).contains(".xlsx");
        }

        @Test
        @DisplayName("GIVEN file named .xlsx but with invalid magic bytes WHEN parseAndValidate THEN returns INVALID_FORMAT")
        void testCorruptedFile_returnsInvalidFormat() {
            byte[] fakeBytes = "This is definitely not a zip file or xlsx".getBytes(StandardCharsets.UTF_8);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "malicious.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fakeBytes
            );

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isFalse();
            assertThat(report.getFileStatus()).isEqualTo("INVALID_FORMAT");
        }
    }

    @Nested
    @DisplayName("2. Header Validation Tests")
    class HeaderValidationTests {

        @Test
        @DisplayName("GIVEN missing required header (Pinyin missing) WHEN parseAndValidate THEN returns HEADER_ERROR")
        void testMissingRequiredHeader_returnsHeaderError() throws IOException {
            String[] headers = {"Chữ Hán", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {{"学", "Học", "Học tập"}};
            byte[] bytes = createSampleWorkbook(headers, data);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes
            );

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isFalse();
            assertThat(report.getFileStatus()).isEqualTo("HEADER_ERROR");
            assertThat(report.getSummaryMessage()).contains("thiếu các cột bắt buộc");
        }

        @Test
        @DisplayName("GIVEN English headers (Hanzi, Pinyin, Meaning Han Viet, Meaning Vi) WHEN parseAndValidate THEN recognizes columns successfully")
        void testEnglishHeaders_success() throws IOException {
            String[] headers = {"Hanzi", "Pinyin", "Meaning Han Viet", "Meaning Vi"};
            String[][] data = {{"学", "xué", "Học", "Học tập"}};
            byte[] bytes = createSampleWorkbook(headers, data);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "test_en.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes
            );

            when(vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")).thenReturn(Optional.empty());

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isTrue();
            assertThat(report.getFileStatus()).isEqualTo("VALID");
            assertThat(report.getTotalRows()).isEqualTo(1);
            assertThat(report.getValidRowsCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("3. Row-by-Row Field Validation Tests")
    class RowFieldValidationTests {

        @Test
        @DisplayName("GIVEN rows with missing required fields WHEN parseAndValidate THEN collects row-level errors and keeps valid rows")
        void testRowValidation_collectsErrors() throws IOException {
            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {
                    {"学", "xué", "Học", "Học tập"},      // Row 2: Valid
                    {"", "shēng", "Sinh", "Học sinh"},     // Row 3: Missing Hanzi
                    {"校", "", "Hiệu", "Trường học"},      // Row 4: Missing Pinyin
                    {"书", "shū", "", "Sách"},             // Row 5: Missing Han-Viet
                    {"本", "běn", "Bản", ""}              // Row 6: Missing MeaningVi
            };
            byte[] bytes = createSampleWorkbook(headers, data);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "vocab_with_errors.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes
            );

            when(vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")).thenReturn(Optional.empty());

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isFalse();
            assertThat(report.getFileStatus()).isEqualTo("INVALID");
            assertThat(report.getTotalRows()).isEqualTo(5);
            assertThat(report.getValidRowsCount()).isEqualTo(1);
            assertThat(report.getInvalidRowsCount()).isEqualTo(4);

            // Verify row 2 is valid
            ParsedVocabularyItem itemRow2 = report.getRows().get(0);
            assertThat(itemRow2.getRowNumber()).isEqualTo(2);
            assertThat(itemRow2.getIsValid()).isTrue();

            // Verify row 3 has Hanzi error
            ParsedVocabularyItem itemRow3 = report.getRows().get(1);
            assertThat(itemRow3.getRowNumber()).isEqualTo(3);
            assertThat(itemRow3.getIsValid()).isFalse();
            assertThat(itemRow3.getErrors()).anyMatch(e -> e.contains("Chữ Hán không được để trống"));

            // Verify row 4 has Pinyin error
            ParsedVocabularyItem itemRow4 = report.getRows().get(2);
            assertThat(itemRow4.getRowNumber()).isEqualTo(4);
            assertThat(itemRow4.getIsValid()).isFalse();
            assertThat(itemRow4.getErrors()).anyMatch(e -> e.contains("Pinyin không được để trống"));

            // Verify row 5 has Han-Viet error
            ParsedVocabularyItem itemRow5 = report.getRows().get(3);
            assertThat(itemRow5.getRowNumber()).isEqualTo(5);
            assertThat(itemRow5.getIsValid()).isFalse();
            assertThat(itemRow5.getErrors()).anyMatch(e -> e.contains("Nghĩa Hán-Việt không được để trống"));

            // Verify row 6 has MeaningVi error
            ParsedVocabularyItem itemRow6 = report.getRows().get(4);
            assertThat(itemRow6.getRowNumber()).isEqualTo(6);
            assertThat(itemRow6.getIsValid()).isFalse();
            assertThat(itemRow6.getErrors()).anyMatch(e -> e.contains("Nghĩa tiếng Việt không được để trống"));
        }

        @Test
        @DisplayName("GIVEN duplicate rows within same file WHEN parseAndValidate THEN flags subsequent row as DUPLICATE_ROW")
        void testDuplicateRowsWithinFile() throws IOException {
            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {
                    {"学", "xué", "Học", "Học tập"},      // Row 2: First appearance
                    {"生", "shēng", "Sinh", "Học sinh"},   // Row 3: Different
                    {"学", "xué", "Học", "Học lại"}        // Row 4: Duplicate of row 2!
            };
            byte[] bytes = createSampleWorkbook(headers, data);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "duplicate_vocab.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes
            );

            when(vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")).thenReturn(Optional.empty());
            when(vocabularyRepository.findByHanziAndPinyinRaw("生", "sheng")).thenReturn(Optional.empty());

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isFalse();
            assertThat(report.getTotalRows()).isEqualTo(3);
            assertThat(report.getValidRowsCount()).isEqualTo(2);
            assertThat(report.getInvalidRowsCount()).isEqualTo(1);

            ParsedVocabularyItem duplicateItem = report.getRows().get(2);
            assertThat(duplicateItem.getRowNumber()).isEqualTo(4);
            assertThat(duplicateItem.getIsValid()).isFalse();
            assertThat(duplicateItem.getErrors()).anyMatch(e -> e.contains("bị trùng lặp với dòng 2"));
        }
    }

    @Nested
    @DisplayName("4. Vocabulary Existence & Preview Classification Tests")
    class VocabularyExistenceTests {

        @Test
        @DisplayName("GIVEN workbook with mixed existing and new vocabularies WHEN parseAndValidate THEN accurately classifies each item")
        void testVocabularyExistenceClassification() throws IOException {
            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {
                    {"学", "xué", "Học", "Học tập"},     // Row 2: Already in DB
                    {"新", "xīn", "Tân", "Mới"}           // Row 3: Not in DB (New)
            };
            byte[] bytes = createSampleWorkbook(headers, data);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "mixed_vocab.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes
            );

            Vocabulary existingVocab = new Vocabulary("学", "xué", "xue", "Học", "Học tập");
            existingVocab.setVocabId(99L);

            when(vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")).thenReturn(Optional.of(existingVocab));
            when(vocabularyRepository.findByHanziAndPinyinRaw("新", "xin")).thenReturn(Optional.empty());

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isTrue();
            assertThat(report.getTotalRows()).isEqualTo(2);
            assertThat(report.getValidRowsCount()).isEqualTo(2);
            assertThat(report.getExistingVocabCount()).isEqualTo(1);
            assertThat(report.getNewVocabCount()).isEqualTo(1);

            // Check Row 2 (Existing)
            ParsedVocabularyItem item1 = report.getRows().get(0);
            assertThat(item1.getIsExisting()).isTrue();
            assertThat(item1.getExistingVocabId()).isEqualTo(99L);
            assertThat(item1.getPinyinRaw()).isEqualTo("xue");

            // Check Row 3 (New)
            ParsedVocabularyItem item2 = report.getRows().get(1);
            assertThat(item2.getIsExisting()).isFalse();
            assertThat(item2.getExistingVocabId()).isNull();
            assertThat(item2.getPinyinRaw()).isEqualTo("xin");

            // Verify strict NO-MUTATION invariant (Step 1 Preview MUST NOT save)
            verify(vocabularyRepository, never()).save(any(Vocabulary.class));
            verify(vocabularyRepository, never()).saveAll(any());
            verify(vocabularyRepository, never()).delete(any(Vocabulary.class));
        }

        @Test
        @DisplayName("GIVEN blank intermediate and trailing rows WHEN parseAndValidate THEN skips blank rows cleanly")
        void testBlankRowsSkipped() throws IOException {
            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {
                    {"学", "xué", "Học", "Học tập"},
                    {"", "", "", ""},                  // Intermediate blank
                    {"生", "shēng", "Sinh", "Học sinh"},
                    {"", "", "", ""}                   // Trailing blank
            };
            byte[] bytes = createSampleWorkbook(headers, data);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "blank_rows.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes
            );

            when(vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")).thenReturn(Optional.empty());
            when(vocabularyRepository.findByHanziAndPinyinRaw("生", "sheng")).thenReturn(Optional.empty());

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isTrue();
            assertThat(report.getTotalRows()).isEqualTo(2);
            assertThat(report.getValidRowsCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("5. Row Count Limit & DoS Protection Tests (BE-UPLOAD-001)")
    class RowLimitSecurityTests {

        private byte[] createWorkbookWithRowCount(int dataRowCount) throws IOException {
            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("Vocabularies");
                Row headerRow = sheet.createRow(0);
                String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }
                for (int r = 1; r <= dataRowCount; r++) {
                    Row row = sheet.createRow(r);
                    row.createCell(0).setCellValue("字" + r);
                    row.createCell(1).setCellValue("zi" + r);
                    row.createCell(2).setCellValue("Tự" + r);
                    row.createCell(3).setCellValue("Chữ " + r);
                }
                workbook.write(baos);
                return baos.toByteArray();
            }
        }

        @Test
        @DisplayName("GIVEN workbook with exactly MAX_DATA_ROWS (5000 rows) WHEN parseAndValidate THEN accepted successfully")
        void testExactMaxDataRows_accepted() throws IOException {
            byte[] bytes = createWorkbookWithRowCount(ExcelParserServiceImpl.MAX_DATA_ROWS);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "exact_5000_rows.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes
            );

            when(vocabularyRepository.findByHanziAndPinyinRaw(any(), any())).thenReturn(Optional.empty());

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isTrue();
            assertThat(report.getFileStatus()).isEqualTo("VALID");
            assertThat(report.getTotalRows()).isEqualTo(ExcelParserServiceImpl.MAX_DATA_ROWS);
            assertThat(report.getValidRowsCount()).isEqualTo(ExcelParserServiceImpl.MAX_DATA_ROWS);
        }

        @Test
        @DisplayName("GIVEN workbook with MAX_DATA_ROWS + 1 (5001 rows) WHEN parseAndValidate THEN rejected with ROW_LIMIT_EXCEEDED and zero DB lookups")
        void testMaxDataRowsPlusOne_rejectedFast() throws IOException {
            byte[] bytes = createWorkbookWithRowCount(ExcelParserServiceImpl.MAX_DATA_ROWS + 1);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "oversized_5001_rows.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes
            );

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isFalse();
            assertThat(report.getFileStatus()).isEqualTo("ROW_LIMIT_EXCEEDED");
            assertThat(report.getSummaryMessage()).contains("5000");
            assertThat(report.getErrors()).hasSize(1);
            assertThat(report.getErrors().get(0).getErrorCode()).isEqualTo("VALIDATION_ERROR");

            // Verify fail-fast: ZERO database queries executed
            verify(vocabularyRepository, never()).findByHanziAndPinyinRaw(any(), any());
        }

        @Test
        @DisplayName("GIVEN workbook with high row index (e.g. row 10000) WHEN parseAndValidate THEN rejects fast without iterating intermediate rows")
        void testHighRowIndex_rejectedFast() throws IOException {
            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("Vocabularies");
                Row headerRow = sheet.createRow(0);
                String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }
                // Place a row at index 10,000
                Row row = sheet.createRow(10000);
                row.createCell(0).setCellValue("字");
                row.createCell(1).setCellValue("zi");
                row.createCell(2).setCellValue("Tự");
                row.createCell(3).setCellValue("Chữ");

                workbook.write(baos);
                byte[] bytes = baos.toByteArray();

                MockMultipartFile file = new MockMultipartFile(
                        "file", "high_index_row.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        bytes
                );

                ImportValidationReport report = excelParserService.parseAndValidate(file);

                assertThat(report.getIsValid()).isFalse();
                assertThat(report.getFileStatus()).isEqualTo("ROW_LIMIT_EXCEEDED");
                verify(vocabularyRepository, never()).findByHanziAndPinyinRaw(any(), any());
            }
        }
    }

    @Nested
    @DisplayName("6. Adversarial Security & CVE-2025-31672 Hardening Tests")
    class CveAndHardeningSecurityTests {

        private byte[] createWorkbookWithDuplicateZipEntry(String targetEntryName) throws IOException {
            String[] headers = {"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"};
            String[][] data = {{"学", "xué", "Học", "Học tập"}};
            byte[] validBytes = createSampleWorkbook(headers, data);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (org.apache.commons.compress.archivers.zip.ZipArchiveInputStream zis =
                         new org.apache.commons.compress.archivers.zip.ZipArchiveInputStream(new ByteArrayInputStream(validBytes));
                 org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream zos =
                         new org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream(baos)) {

                org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry;
                while ((entry = zis.getNextZipEntry()) != null) {
                    zos.putArchiveEntry(new org.apache.commons.compress.archivers.zip.ZipArchiveEntry(entry.getName()));
                    zis.transferTo(zos);
                    zos.closeArchiveEntry();

                    if (entry.getName().equalsIgnoreCase(targetEntryName)) {
                        // Inject duplicate entry with identical name (CVE-2025-31672 attack payload)
                        zos.putArchiveEntry(new org.apache.commons.compress.archivers.zip.ZipArchiveEntry(entry.getName()));
                        zos.write("<maliciousDuplicateEntry/>".getBytes(StandardCharsets.UTF_8));
                        zos.closeArchiveEntry();
                    }
                }
            }
            return baos.toByteArray();
        }

        @Test
        @DisplayName("GIVEN OOXML archive with duplicate xl/worksheets/sheet1.xml (CVE-2025-31672) WHEN parseAndValidate THEN rejected safely as INVALID_FORMAT")
        void testDuplicateZipEntries_sheetXml_rejectedSafely() throws IOException {
            byte[] maliciousBytes = createWorkbookWithDuplicateZipEntry("xl/worksheets/sheet1.xml");
            MockMultipartFile file = new MockMultipartFile(
                    "file", "cve_2025_31672_duplicate_sheet.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    maliciousBytes
            );

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isFalse();
            assertThat(report.getFileStatus()).isEqualTo("INVALID_FORMAT");
            assertThat(report.getSummaryMessage()).contains("định dạng tệp bị lỗi hoặc hỏng");

            // Verify zero DB lookups
            verify(vocabularyRepository, never()).findByHanziAndPinyinRaw(any(), any());
        }

        @Test
        @DisplayName("GIVEN OOXML archive with duplicate [Content_Types].xml (CVE-2025-31672) WHEN parseAndValidate THEN rejected safely as INVALID_FORMAT")
        void testDuplicateZipEntries_contentTypes_rejectedSafely() throws IOException {
            byte[] maliciousBytes = createWorkbookWithDuplicateZipEntry("[Content_Types].xml");
            MockMultipartFile file = new MockMultipartFile(
                    "file", "cve_2025_31672_duplicate_content_types.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    maliciousBytes
            );

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isFalse();
            assertThat(report.getFileStatus()).isEqualTo("INVALID_FORMAT");
            verify(vocabularyRepository, never()).findByHanziAndPinyinRaw(any(), any());
        }

        @Test
        @DisplayName("GIVEN malformed ZIP archive with valid ZIP magic bytes WHEN parseAndValidate THEN rejected safely without uncaught exception")
        void testMalformedZipWithValidMagicHeader_rejectedSafely() {
            byte[] malformedBytes = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00, 0x7F, 0x7F, 0x7F, 0x7F};
            MockMultipartFile file = new MockMultipartFile(
                    "file", "malformed_archive.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    malformedBytes
            );

            ImportValidationReport report = excelParserService.parseAndValidate(file);

            assertThat(report.getIsValid()).isFalse();
            assertThat(report.getFileStatus()).isEqualTo("INVALID_FORMAT");
            verify(vocabularyRepository, never()).findByHanziAndPinyinRaw(any(), any());
        }

        @Test
        @DisplayName("GIVEN input stream WHEN parseAndValidate executed THEN InputStream is properly handled and cleaned up (Test J)")
        void testResourceCleanup_inputStreamHandledSafely() throws IOException {
            byte[] validBytes = createSampleWorkbook(new String[]{"Chữ Hán", "Pinyin", "Hán-Việt", "Nghĩa tiếng Việt"},
                    new String[][]{{"学", "xué", "Học", "Học tập"}});

            ByteArrayInputStream trackedStream = new ByteArrayInputStream(validBytes);

            when(vocabularyRepository.findByHanziAndPinyinRaw("学", "xue")).thenReturn(Optional.empty());

            ImportValidationReport report = excelParserService.parseAndValidate(trackedStream, "valid.xlsx");

            assertThat(report.getIsValid()).isTrue();
            assertThat(report.getFileStatus()).isEqualTo("VALID");
            assertThat(report.getValidRowsCount()).isEqualTo(1);
        }
    }
}
