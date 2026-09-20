package com.elearning.common;

import org.springframework.http.HttpStatus;

/**
 * Standard system and business error codes matching .agents/API.md specifications.
 */
public enum ErrorCode {

    SUCCESS("SUCCESS", "Thao tác thành công", HttpStatus.OK),
    CREATED("CREATED", "Tài nguyên mới được tạo thành công", HttpStatus.CREATED),
    VALIDATION_ERROR("VALIDATION_ERROR", "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("BAD_REQUEST", "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "Chưa xác thực hoặc phiên đăng nhập đã hết hạn", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "Không có quyền truy cập tài nguyên này", HttpStatus.FORBIDDEN),
    NOT_FOUND("NOT_FOUND", "Không tìm thấy tài nguyên yêu cầu", HttpStatus.NOT_FOUND),
    CONFLICT("CONFLICT", "Dữ liệu đã tồn tại hoặc xảy ra xung đột", HttpStatus.CONFLICT),
    UNPROCESSABLE_ENTITY("UNPROCESSABLE_ENTITY", "Vi phạm quy tắc nghiệp vụ", HttpStatus.UNPROCESSABLE_ENTITY),
    FILE_TOO_LARGE("FILE_TOO_LARGE", "File import vượt quá dung lượng cho phép", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_TYPE_INVALID("FILE_TYPE_INVALID", "File không đúng định dạng cho phép", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", "Quá nhiều yêu cầu đăng nhập, vui lòng thử lại sau", HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR("INTERNAL_ERROR", "Lỗi nội bộ hệ thống", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
