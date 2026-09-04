package com.elearning.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary and detail report produced by {@link com.elearning.service.ExcelParserService}
 * during Step 1 of the Two-Step Excel Import Engine (Module 5C).
 * Conforms to API.md section 2.4 (POST /api/v1/creator/lessons/import).
 */
public class ImportValidationReport {

    private Boolean isValid;
    private Integer totalRows;
    private Integer validRowsCount;
    private Integer invalidRowsCount;
    private Integer newVocabCount;
    private Integer existingVocabCount;
    private String fileStatus;
    private String summaryMessage;
    private List<ParsedVocabularyItem> rows = new ArrayList<>();
    private List<RowValidationError> errors = new ArrayList<>();

    public ImportValidationReport() {
        this.isValid = true;
        this.totalRows = 0;
        this.validRowsCount = 0;
        this.invalidRowsCount = 0;
        this.newVocabCount = 0;
        this.existingVocabCount = 0;
        this.fileStatus = "VALID";
        this.summaryMessage = "";
    }

    public static ImportValidationReport fileLevelError(String status, String message, String errorCode) {
        ImportValidationReport report = new ImportValidationReport();
        report.setIsValid(false);
        report.setFileStatus(status);
        report.setSummaryMessage(message);
        report.addError(RowValidationError.fileError(errorCode, message));
        return report;
    }

    public void addRow(ParsedVocabularyItem item) {
        if (this.rows == null) {
            this.rows = new ArrayList<>();
        }
        this.rows.add(item);
    }

    public void addError(RowValidationError error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.isValid = false;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(Boolean valid) {
        isValid = valid;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getValidRowsCount() {
        return validRowsCount;
    }

    public void setValidRowsCount(Integer validRowsCount) {
        this.validRowsCount = validRowsCount;
    }

    public Integer getInvalidRowsCount() {
        return invalidRowsCount;
    }

    public void setInvalidRowsCount(Integer invalidRowsCount) {
        this.invalidRowsCount = invalidRowsCount;
    }

    public Integer getNewVocabCount() {
        return newVocabCount;
    }

    public void setNewVocabCount(Integer newVocabCount) {
        this.newVocabCount = newVocabCount;
    }

    public Integer getExistingVocabCount() {
        return existingVocabCount;
    }

    public void setExistingVocabCount(Integer existingVocabCount) {
        this.existingVocabCount = existingVocabCount;
    }

    public String getFileStatus() {
        return fileStatus;
    }

    public void setFileStatus(String fileStatus) {
        this.fileStatus = fileStatus;
    }

    public String getSummaryMessage() {
        return summaryMessage;
    }

    public void setSummaryMessage(String summaryMessage) {
        this.summaryMessage = summaryMessage;
    }

    public List<ParsedVocabularyItem> getRows() {
        return rows;
    }

    public void setRows(List<ParsedVocabularyItem> rows) {
        this.rows = rows;
    }

    public List<RowValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<RowValidationError> errors) {
        this.errors = errors;
    }
}
