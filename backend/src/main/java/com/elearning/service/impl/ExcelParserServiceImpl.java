package com.elearning.service.impl;

import com.elearning.common.ErrorCode;
import com.elearning.dto.response.ImportValidationReport;
import com.elearning.dto.response.ParsedVocabularyItem;
import com.elearning.dto.response.RowValidationError;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.ExcelParserService;
import org.apache.poi.EmptyFileException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link ExcelParserService} for Step 1 of the Two-Step Excel Import Engine.
 * Handles secure file validation, streaming inspection, header mapping, row-by-row validation,
 * duplicate detection, and read-only vocabulary existence lookup without modifying database state.
 */
@Service
@Transactional(readOnly = true)
public class ExcelParserServiceImpl implements ExcelParserService {

    private static final Logger log = LoggerFactory.getLogger(ExcelParserServiceImpl.class);

    /**
     * Maximum allowed file size for Excel imports (10MB in bytes).
     */
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;

    /**
     * Maximum allowed data rows for Excel imports (excluding header row).
     * Enforces bounded resource consumption and prevents DoS per OWASP API4:2023.
     */
    public static final int MAX_DATA_ROWS = 5000;

    /**
     * Standard ZIP file magic header bytes (PK\x03\x04).
     */
    private static final byte[] ZIP_HEADER = {0x50, 0x4B, 0x03, 0x04};

    private final VocabularyRepository vocabularyRepository;

    public ExcelParserServiceImpl(VocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
    }

    @Override
    public ImportValidationReport parseAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ImportValidationReport.fileLevelError(
                    "EMPTY_FILE",
                    "File tải lên không có dữ liệu",
                    ErrorCode.FILE_TYPE_INVALID.getCode()
            );
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return ImportValidationReport.fileLevelError(
                    "FILE_TOO_LARGE",
                    "Dung lượng file vượt quá giới hạn tối đa 10MB",
                    ErrorCode.FILE_TOO_LARGE.getCode()
            );
        }

        String originalFilename = file.getOriginalFilename();
        try (InputStream is = file.getInputStream()) {
            return parseAndValidate(is, originalFilename);
        } catch (IOException e) {
            log.error("Failed to read uploaded file input stream: {}", e.getMessage());
            return ImportValidationReport.fileLevelError(
                    "INVALID_FORMAT",
                    "Không thể đọc file tải lên: lỗi I/O",
                    ErrorCode.FILE_TYPE_INVALID.getCode()
            );
        }
    }

    @Override
    public ImportValidationReport parseAndValidate(InputStream inputStream, String originalFilename) {
        if (inputStream == null) {
            return ImportValidationReport.fileLevelError(
                    "EMPTY_FILE",
                    "Luồng dữ liệu file rỗng hoặc không tồn tại",
                    ErrorCode.FILE_TYPE_INVALID.getCode()
            );
        }

        // 1. Extension validation (defense-in-depth)
        if (originalFilename != null && !originalFilename.isBlank()) {
            String lowerName = originalFilename.toLowerCase();
            if (!lowerName.endsWith(".xlsx")) {
                return ImportValidationReport.fileLevelError(
                        "INVALID_FORMAT",
                        "Chỉ hỗ trợ file Excel định dạng .xlsx",
                        ErrorCode.FILE_TYPE_INVALID.getCode()
                );
            }
        }

        // 2. Buffer stream for magic byte verification and POI parsing
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        bis.mark(16);
        byte[] headerBytes = new byte[4];
        int readBytes;
        try {
            readBytes = bis.read(headerBytes, 0, 4);
            bis.reset();
        } catch (IOException e) {
            return ImportValidationReport.fileLevelError(
                    "INVALID_FORMAT",
                    "Không thể kiểm tra định dạng file",
                    ErrorCode.FILE_TYPE_INVALID.getCode()
            );
        }

        if (readBytes < 4 || !isZipHeader(headerBytes)) {
            return ImportValidationReport.fileLevelError(
                    "INVALID_FORMAT",
                    "File không phải định dạng Office Open XML (.xlsx) hợp lệ",
                    ErrorCode.FILE_TYPE_INVALID.getCode()
            );
        }

        // 3. Open workbook safely with try-with-resources
        try (Workbook workbook = WorkbookFactory.create(bis)) {
            if (workbook.getNumberOfSheets() == 0) {
                return ImportValidationReport.fileLevelError(
                        "EMPTY_FILE",
                        "File Excel không chứa bất kỳ trang tính (sheet) nào",
                        ErrorCode.FILE_TYPE_INVALID.getCode()
                );
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return ImportValidationReport.fileLevelError(
                        "EMPTY_FILE",
                        "File Excel không chứa bất kỳ dòng dữ liệu nào",
                        ErrorCode.FILE_TYPE_INVALID.getCode()
                );
            }

            return processSheet(sheet);

        } catch (EmptyFileException e) {
            return ImportValidationReport.fileLevelError(
                    "EMPTY_FILE",
                    "File Excel không có nội dung dữ liệu",
                    ErrorCode.FILE_TYPE_INVALID.getCode()
            );
        } catch (NotOfficeXmlFileException e) {
            return ImportValidationReport.fileLevelError(
                    "INVALID_FORMAT",
                    "File không phải định dạng Office Open XML (.xlsx) hợp lệ",
                    ErrorCode.FILE_TYPE_INVALID.getCode()
            );
        } catch (EncryptedDocumentException e) {
            return ImportValidationReport.fileLevelError(
                    "INVALID_FORMAT",
                    "File Excel được bảo vệ bằng mật khẩu, vui lòng mở khóa trước khi import",
                    ErrorCode.FILE_TYPE_INVALID.getCode()
            );
        } catch (Exception e) {
            log.error("Error parsing Excel workbook: {}", e.getMessage());
            return ImportValidationReport.fileLevelError(
                    "INVALID_FORMAT",
                    "Không thể xử lý file Excel: định dạng tệp bị lỗi hoặc hỏng",
                    ErrorCode.FILE_TYPE_INVALID.getCode()
            );
        }
    }

    /**
     * Processes worksheet rows, validates header schema, and evaluates rows.
     */
    private ImportValidationReport processSheet(Sheet sheet) {
        ImportValidationReport report = new ImportValidationReport();
        DataFormatter dataFormatter = new DataFormatter();

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return ImportValidationReport.fileLevelError(
                    "HEADER_ERROR",
                    "File Excel thiếu dòng tiêu đề (Header row)",
                    ErrorCode.VALIDATION_ERROR.getCode()
            );
        }

        // 4. Header inspection and column mapping
        HeaderMapping mapping = resolveHeaderMapping(headerRow, dataFormatter);
        if (!mapping.isValid()) {
            return ImportValidationReport.fileLevelError(
                    "HEADER_ERROR",
                    "File Excel thiếu các cột bắt buộc: Chữ Hán, Pinyin, Hán-Việt, Nghĩa tiếng Việt",
                    ErrorCode.VALIDATION_ERROR.getCode()
            );
        }

        int lastRowNum = sheet.getLastRowNum();

        // 5. Early Row-Count Guard (OWASP API4:2023 - Unrestricted Resource Consumption)
        // Row 0 is the header. Valid data rows span from index 1 to MAX_DATA_ROWS.
        // If the last row index exceeds MAX_DATA_ROWS, reject immediately to prevent unbounded parsing and DB lookups.
        if (lastRowNum > MAX_DATA_ROWS) {
            return ImportValidationReport.fileLevelError(
                    "ROW_LIMIT_EXCEEDED",
                    String.format("File Excel vượt quá giới hạn tối đa %d dòng dữ liệu", MAX_DATA_ROWS),
                    ErrorCode.VALIDATION_ERROR.getCode()
            );
        }

        Map<String, Integer> seenKeys = new HashMap<>();

        // 6. Iterate data rows (row index 1 to lastRowNum)
        for (int r = 1; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row, dataFormatter)) {
                continue; // Gracefully ignore blank rows
            }

            int excelRowNumber = r + 1; // 1-based row number for user display
            ParsedVocabularyItem item = parseRow(row, excelRowNumber, mapping, dataFormatter, seenKeys, report);
            report.addRow(item);
        }

        if (report.getRows().isEmpty()) {
            return ImportValidationReport.fileLevelError(
                    "EMPTY_FILE",
                    "File Excel chỉ có dòng tiêu đề mà không có dữ liệu từ vựng",
                    ErrorCode.FILE_TYPE_INVALID.getCode()
            );
        }

        // 6. Aggregate report metrics
        int totalRows = report.getRows().size();
        int validRows = 0;
        int invalidRows = 0;
        int existingCount = 0;
        int newCount = 0;

        for (ParsedVocabularyItem item : report.getRows()) {
            if (Boolean.TRUE.equals(item.getIsValid())) {
                validRows++;
                if (Boolean.TRUE.equals(item.getIsExisting())) {
                    existingCount++;
                } else {
                    newCount++;
                }
            } else {
                invalidRows++;
            }
        }

        report.setTotalRows(totalRows);
        report.setValidRowsCount(validRows);
        report.setInvalidRowsCount(invalidRows);
        report.setExistingVocabCount(existingCount);
        report.setNewVocabCount(newCount);

        if (invalidRows == 0) {
            report.setIsValid(true);
            report.setFileStatus("VALID");
            report.setSummaryMessage(String.format(
                    "Kiểm tra thành công: %d dòng hợp lệ (%d từ mới, %d từ đã có trong từ điển)",
                    totalRows, newCount, existingCount
            ));
        } else {
            report.setIsValid(false);
            report.setFileStatus("INVALID");
            report.setSummaryMessage(String.format(
                    "Phát hiện lỗi: %d/%d dòng không hợp lệ, %d dòng hợp lệ",
                    invalidRows, totalRows, validRows
            ));
        }

        return report;
    }

    /**
     * Parses and validates a single data row.
     */
    private ParsedVocabularyItem parseRow(Row row, int rowNumber, HeaderMapping mapping,
                                          DataFormatter dataFormatter, Map<String, Integer> seenKeys,
                                          ImportValidationReport report) {
        String hanzi = getCellValue(row, mapping.hanziCol, dataFormatter);
        String pinyin = getCellValue(row, mapping.pinyinCol, dataFormatter);
        String meaningHanViet = getCellValue(row, mapping.meaningHanVietCol, dataFormatter);
        String meaningVi = getCellValue(row, mapping.meaningViCol, dataFormatter);
        String exampleSentence = getCellValue(row, mapping.exampleSentenceCol, dataFormatter);
        String exampleTranslation = getCellValue(row, mapping.exampleTranslationCol, dataFormatter);

        ParsedVocabularyItem item = new ParsedVocabularyItem();
        item.setRowNumber(rowNumber);
        item.setHanzi(hanzi);
        item.setPinyin(pinyin);
        item.setMeaningHanViet(meaningHanViet);
        item.setMeaningVi(meaningVi);
        item.setExampleSentence(exampleSentence);
        item.setExampleTranslation(exampleTranslation);
        item.setIsValid(true);

        // Validation rule: Hanzi required, max 50
        if (hanzi == null || hanzi.isBlank()) {
            addError(item, report, rowNumber, "Chữ Hán", "MISSING_REQUIRED_FIELD", "Chữ Hán không được để trống");
        } else if (hanzi.length() > 50) {
            addError(item, report, rowNumber, "Chữ Hán", "INVALID_LENGTH", "Chữ Hán không được vượt quá 50 ký tự");
        }

        // Validation rule: Pinyin required, max 100
        if (pinyin == null || pinyin.isBlank()) {
            addError(item, report, rowNumber, "Pinyin", "MISSING_REQUIRED_FIELD", "Pinyin không được để trống");
        } else if (pinyin.length() > 100) {
            addError(item, report, rowNumber, "Pinyin", "INVALID_LENGTH", "Pinyin không được vượt quá 100 ký tự");
        }

        // Validation rule: MeaningHanViet required, max 100
        if (meaningHanViet == null || meaningHanViet.isBlank()) {
            addError(item, report, rowNumber, "Hán-Việt", "MISSING_REQUIRED_FIELD", "Nghĩa Hán-Việt không được để trống");
        } else if (meaningHanViet.length() > 100) {
            addError(item, report, rowNumber, "Hán-Việt", "INVALID_LENGTH", "Nghĩa Hán-Việt không được vượt quá 100 ký tự");
        }

        // Validation rule: MeaningVi required, max 255
        if (meaningVi == null || meaningVi.isBlank()) {
            addError(item, report, rowNumber, "Nghĩa tiếng Việt", "MISSING_REQUIRED_FIELD", "Nghĩa tiếng Việt không được để trống");
        } else if (meaningVi.length() > 255) {
            addError(item, report, rowNumber, "Nghĩa tiếng Việt", "INVALID_LENGTH", "Nghĩa tiếng Việt không được vượt quá 255 ký tự");
        }

        // Optional: Example sentence max 500
        if (exampleSentence != null && exampleSentence.length() > 500) {
            addError(item, report, rowNumber, "Câu ví dụ", "INVALID_LENGTH", "Câu ví dụ không được vượt quá 500 ký tự");
        }

        // Optional: Example translation max 500
        if (exampleTranslation != null && exampleTranslation.length() > 500) {
            addError(item, report, rowNumber, "Dịch câu ví dụ", "INVALID_LENGTH", "Dịch câu ví dụ không được vượt quá 500 ký tự");
        }

        // Derive toneless pinyinRaw
        String pinyinRaw = toPinyinRaw(pinyin);
        item.setPinyinRaw(pinyinRaw);

        // Check for duplicate row within the same file (composite key: hanzi + pinyinRaw)
        if (hanzi != null && !hanzi.isBlank() && pinyinRaw != null && !pinyinRaw.isBlank()) {
            String duplicateKey = hanzi.trim() + "|" + pinyinRaw.trim();
            if (seenKeys.containsKey(duplicateKey)) {
                int firstRow = seenKeys.get(duplicateKey);
                addError(item, report, rowNumber, "Từ vựng", "DUPLICATE_ROW",
                        String.format("Từ vựng '%s' (%s) bị trùng lặp với dòng %d trong file", hanzi, pinyin, firstRow));
            } else {
                seenKeys.put(duplicateKey, rowNumber);
            }
        }

        // Vocabulary existence lookup (Read-only check against DB, strictly NO mutations)
        if (Boolean.TRUE.equals(item.getIsValid()) && hanzi != null && pinyinRaw != null) {
            Optional<Vocabulary> existingOpt = vocabularyRepository.findByHanziAndPinyinRaw(hanzi.trim(), pinyinRaw.trim());
            if (existingOpt.isPresent()) {
                item.setIsExisting(true);
                item.setExistingVocabId(existingOpt.get().getVocabId());
            } else {
                item.setIsExisting(false);
                item.setExistingVocabId(null);
            }
        } else {
            item.setIsExisting(false);
            item.setExistingVocabId(null);
        }

        return item;
    }

    private void addError(ParsedVocabularyItem item, ImportValidationReport report,
                          int rowNumber, String column, String code, String message) {
        item.addError(message);
        report.addError(RowValidationError.of(rowNumber, column, code, message));
    }

    /**
     * Resolves column index mappings based on header text.
     */
    private HeaderMapping resolveHeaderMapping(Row headerRow, DataFormatter dataFormatter) {
        HeaderMapping mapping = new HeaderMapping();
        int lastCellNum = headerRow.getLastCellNum();

        for (int c = 0; c < lastCellNum; c++) {
            Cell cell = headerRow.getCell(c);
            if (cell == null) continue;
            String headerText = normalizeHeader(dataFormatter.formatCellValue(cell));

            if (matchesHeader(headerText, "hanzi", "chuhan", "tuvung", "chinese", "chuhantu")) {
                mapping.hanziCol = c;
            } else if (matchesHeader(headerText, "pinyin", "phienam")) {
                mapping.pinyinCol = c;
            } else if (matchesHeader(headerText, "meaninghanviet", "hanviet", "amhanviet", "nghiahanviet")) {
                mapping.meaningHanVietCol = c;
            } else if (matchesHeader(headerText, "meaningvi", "meaning", "nghia", "nghiatiengviet", "giainghia", "vietnamesemeaning")) {
                mapping.meaningViCol = c;
            } else if (matchesHeader(headerText, "examplesentence", "example", "cauvidu", "vidu")) {
                mapping.exampleSentenceCol = c;
            } else if (matchesHeader(headerText, "exampletranslation", "translation", "dichnghia", "dichvidu", "dichcauvidu")) {
                mapping.exampleTranslationCol = c;
            }
        }

        return mapping;
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        String normalized = Normalizer.normalize(header, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();
    }

    private boolean matchesHeader(String target, String... candidates) {
        for (String c : candidates) {
            if (target.equalsIgnoreCase(c)) {
                return true;
            }
        }
        return false;
    }

    private String getCellValue(Row row, int colIndex, DataFormatter dataFormatter) {
        if (colIndex < 0) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        String val = dataFormatter.formatCellValue(cell);
        return val != null ? val.trim() : null;
    }

    private boolean isRowEmpty(Row row, DataFormatter dataFormatter) {
        int lastCell = row.getLastCellNum();
        if (lastCell <= 0) return true;
        for (int c = 0; c < lastCell; c++) {
            Cell cell = row.getCell(c);
            if (cell != null) {
                String val = dataFormatter.formatCellValue(cell);
                if (val != null && !val.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isZipHeader(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        return bytes[0] == ZIP_HEADER[0] &&
                bytes[1] == ZIP_HEADER[1] &&
                bytes[2] == ZIP_HEADER[2] &&
                bytes[3] == ZIP_HEADER[3];
    }

    /**
     * Converts pinyin with tones (e.g. xué, nǐ, xiū) to raw toneless pinyin (xue, ni, xiu).
     */
    public static String toPinyinRaw(String pinyin) {
        if (pinyin == null || pinyin.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(pinyin, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .replace("ü", "u")
                .replace("Ü", "u")
                .replace("v", "u")
                .toLowerCase()
                .trim();
    }

    private static class HeaderMapping {
        int hanziCol = -1;
        int pinyinCol = -1;
        int meaningHanVietCol = -1;
        int meaningViCol = -1;
        int exampleSentenceCol = -1;
        int exampleTranslationCol = -1;

        boolean isValid() {
            return hanziCol >= 0 && pinyinCol >= 0 && meaningHanVietCol >= 0 && meaningViCol >= 0;
        }
    }
}
