package com.elearning.exception;

import com.elearning.common.ErrorCode;
import com.elearning.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized Global Exception Handler transforming exceptions into standard {@link ApiResponse} envelopes.
 * Conforms to .agents/API.md specifications.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation exceptions from Jakarta Validation on @Valid @RequestBody arguments.
     * Returns HTTP 400 with VALIDATION_ERROR code and detailed error messages list.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();

        if (ex.getBindingResult() != null) {
            for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
                errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
            }
            for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
                errors.add(globalError.getObjectName() + ": " + globalError.getDefaultMessage());
            }
        }

        log.warn("Validation error encountered: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.VALIDATION_ERROR.getCode(),
                        ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
                        errors
                ));
    }

    /**
     * Handles custom business exceptions, preserving dynamic ErrorCode and HTTP status.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = (errorCode != null && errorCode.getHttpStatus() != null)
                ? errorCode.getHttpStatus()
                : HttpStatus.BAD_REQUEST;
        String code = (errorCode != null) ? errorCode.getCode() : ErrorCode.BAD_REQUEST.getCode();
        String message = ex.getMessage();

        log.warn("Business exception occurred [code={}]: {}", code, message);

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(code, message));
    }

    /**
     * Fallback handler for all uncaught unexpected exceptions.
     * Returns HTTP 500 with generic safe message, hiding internal stack trace/details from clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception caught by fallback handler: ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_ERROR.getCode(),
                        ErrorCode.INTERNAL_ERROR.getDefaultMessage()
                ));
    }
}
