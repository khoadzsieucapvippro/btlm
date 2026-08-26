package com.elearning.exception;

import com.elearning.common.ErrorCode;

/**
 * Base custom business exception for domain and business rule violations.
 * Encapsulates standard {@link ErrorCode} and preserves dynamic error codes/messages.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode != null ? errorCode.getDefaultMessage() : "Business error occurred");
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
