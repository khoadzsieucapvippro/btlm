package com.elearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user authentication (/api/v1/auth/login).
 * Conforms to .agents/API.md section 2.1.
 */
public class LoginRequest {

    @NotBlank(message = "Email or phone is required")
    @Size(max = 191, message = "Email or phone must not exceed 191 characters")
    private String emailOrPhone;

    @NotBlank(message = "Password is required")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String emailOrPhone, String password) {
        this.emailOrPhone = emailOrPhone;
        this.password = password;
    }

    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public void setEmailOrPhone(String emailOrPhone) {
        this.emailOrPhone = emailOrPhone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "emailOrPhone='" + emailOrPhone + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}
