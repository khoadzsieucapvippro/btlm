package com.elearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user account registration (/api/v1/auth/register).
 * Conforms to .agents/API.md section 2.1.
 */
public class RegisterRequest {

    @NotBlank(message = "Email or phone is required")
    @Size(max = 191, message = "Email or phone must not exceed 191 characters")
    private String emailOrPhone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    public RegisterRequest() {
    }

    public RegisterRequest(String emailOrPhone, String password, String fullName) {
        this.emailOrPhone = emailOrPhone;
        this.password = password;
        this.fullName = fullName;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "emailOrPhone='" + emailOrPhone + '\'' +
                ", fullName='" + fullName + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}
