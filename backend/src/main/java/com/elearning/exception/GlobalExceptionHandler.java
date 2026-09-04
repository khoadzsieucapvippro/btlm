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
     * Handles Spring Security authentication failures (BadCredentialsException, DisabledException, etc.).
     * Returns HTTP 401 with UNAUTHORIZED code and safe generic message preventing account enumeration.
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        log.warn("Authentication failure: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        ErrorCode.UNAUTHORIZED.getCode(),
                        "Email/số điện thoại hoặc mật khẩu không chính xác"
                ));
    }

    /**
     * Handles database constraint violations (duplicate keys, unique constraints).
     * Returns HTTP 409 with CONFLICT code, preventing raw SQL details from leaking to clients.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Database data integrity violation: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        ErrorCode.CONFLICT.getCode(),
                        "Dữ liệu đã tồn tại hoặc vi phạm ràng buộc toàn vẹn"
                ));
    }

    /**
     * Handles optimistic locking failures / concurrent modification conflicts (HTTP 409 Conflict).
     * Maps to ErrorCode.CONFLICT according to .agents/API.md specifications (BE-CONC-001, BE-CONC-003).
     */
    @ExceptionHandler({
            org.springframework.dao.ConcurrencyFailureException.class,
            jakarta.persistence.OptimisticLockException.class,
            org.hibernate.StaleObjectStateException.class,
            org.hibernate.exception.LockAcquisitionException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailureException(Exception ex) {
        log.warn("Concurrency / optimistic locking conflict during modification: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        ErrorCode.CONFLICT.getCode(),
                        "Dữ liệu đã bị thay đổi bởi một phiên làm việc khác, vui lòng thử lại"
                ));
    }

    /**
     * Handles missing routes/static resources (HTTP 404).
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getDefaultMessage()
                ));
    }

    /**
     * Handles file upload size limit violations (HTTP 413 Payload Too Large).
     * Maps to ErrorCode.FILE_TOO_LARGE according to .agents/API.md specifications.
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded limit: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(
                        ErrorCode.FILE_TOO_LARGE.getCode(),
                        ErrorCode.FILE_TOO_LARGE.getDefaultMessage()
                ));
    }

    /**
     * Handles missing request parameters or multipart parts (HTTP 400 Bad Request).
     */
    @ExceptionHandler({
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestParameterOrPartException(Exception ex) {
        log.warn("Missing required request parameter or part: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.VALIDATION_ERROR.getCode(),
                        "Tham số yêu cầu không hợp lệ hoặc bị thiếu"
                ));
    }

    /**
     * Handles malformed JSON or unreadable HTTP message payloads (HTTP 400 Bad Request).
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed HTTP request body: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.VALIDATION_ERROR.getCode(),
                        "Định dạng dữ liệu yêu cầu không hợp lệ"
                ));
    }

    /**
     * Handles Bean Validation constraint violations on parameters or collections (HTTP 400 Bad Request).
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(jakarta.validation.ConstraintViolationException ex) {
        List<String> errors = new ArrayList<>();
        if (ex.getConstraintViolations() != null) {
            for (jakarta.validation.ConstraintViolation<?> violation : ex.getConstraintViolations()) {
                errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
            }
        }
        log.warn("Constraint violation encountered: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.VALIDATION_ERROR.getCode(),
                        ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
                        errors
                ));
    }

    /**
     * Handles illegal argument exceptions safely without exposing internal details (HTTP 400 Bad Request).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument encountered: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ErrorCode.BAD_REQUEST.getCode(),
                        "Tham số yêu cầu không hợp lệ"
                ));
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
