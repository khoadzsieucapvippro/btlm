package com.elearning.dto.response;

/**
 * Represents a validation error at a specific row and column of an imported Excel file.
 */
public class RowValidationError {

    private Integer rowNumber;
    private String columnName;
    private String errorCode;
    private String errorMessage;

    public RowValidationError() {
    }

    public RowValidationError(Integer rowNumber, String columnName, String errorCode, String errorMessage) {
        this.rowNumber = rowNumber;
        this.columnName = columnName;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static RowValidationError of(Integer rowNumber, String columnName, String errorCode, String errorMessage) {
        return new RowValidationError(rowNumber, columnName, errorCode, errorMessage);
    }

    public static RowValidationError fileError(String errorCode, String errorMessage) {
        return new RowValidationError(null, "FILE", errorCode, errorMessage);
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "RowValidationError{" +
                "rowNumber=" + rowNumber +
                ", columnName='" + columnName + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
