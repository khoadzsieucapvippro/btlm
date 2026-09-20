package com.elearning.service;

import com.elearning.dto.response.ImportValidationReport;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Service interface for parsing and validating uploaded Excel files (Module 5C - Step 1).
 * Executes secure inspection, structural schema validation, row-by-row field validation,
 * and database vocabulary existence lookup without mutating the database state.
 */
public interface ExcelParserService {

    /**
     * Parses and validates an uploaded {@link MultipartFile}.
     *
     * @param file the uploaded Excel multipart file (.xlsx)
     * @return {@link ImportValidationReport} containing detailed preview and row-level validation results
     */
    ImportValidationReport parseAndValidate(MultipartFile file);

    /**
     * Parses and validates an Excel file provided as an {@link InputStream}.
     * Useful for programmatic calls, testing, or background streaming.
     *
     * @param inputStream      stream containing .xlsx binary content
     * @param originalFilename original name of the file for extension checking
     * @return {@link ImportValidationReport} containing preview and validation results
     */
    ImportValidationReport parseAndValidate(InputStream inputStream, String originalFilename);
}
