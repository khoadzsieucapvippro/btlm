package com.elearning.dto.response;

import com.elearning.common.ErrorCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Standard API Response Envelope matching .agents/API.md section 1.2.
 *
 * @param <T> Response payload type
 */
public class ApiResponse<T> {

    private String code;
    private String message;
    private List<String> errors = new ArrayList<>();
    private T data;

    public ApiResponse() {
        this.errors = new ArrayList<>();
    }

    public ApiResponse(String code, String message, List<String> errors, T data) {
        this.code = code;
        this.message = message;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getDefaultMessage(),
                Collections.emptyList(),
                data
        );
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                ErrorCode.SUCCESS.getCode(),
                message,
                Collections.emptyList(),
                data
        );
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(
                code,
                message,
                Collections.emptyList(),
                null
        );
    }

    public static <T> ApiResponse<T> error(String code, String message, List<String> errors) {
        return new ApiResponse<>(
                code,
                message,
                errors != null ? errors : Collections.emptyList(),
                null
        );
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                Collections.emptyList(),
                null
        );
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, List<String> errors) {
        return new ApiResponse<>(
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                errors != null ? errors : Collections.emptyList(),
                null
        );
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", errors=" + errors +
                ", data=" + data +
                '}';
    }
}
