package com.elearning.controller;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.LoginRequest;
import com.elearning.dto.request.RegisterRequest;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.AuthResponse;
import com.elearning.exception.BusinessException;
import com.elearning.security.LoginRateLimiter;
import com.elearning.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller providing public authentication endpoints:
 * - POST /api/v1/auth/register (201 Created)
 * - POST /api/v1/auth/login (200 OK)
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = resolveClientIp(servletRequest);
        if (!loginRateLimiter.tryAcquire(clientIp)) {
            throw new BusinessException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    "Quá nhiều yêu cầu đăng nhập, vui lòng thử lại sau"
            );
        }

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
