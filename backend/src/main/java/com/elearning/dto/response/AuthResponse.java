package com.elearning.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response DTO returning authentication tokens and identity details (/api/v1/auth/login, /api/v1/auth/register).
 * Conforms to .agents/API.md section 2.1.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    @JsonAlias("accessToken")
    private String token;

    @JsonAlias("tokenType")
    private String type = "Bearer";

    private Long accountId;
    private String emailOrPhone;
    private String fullName;
    private List<String> roles = new ArrayList<>();

    public AuthResponse() {
        this.type = "Bearer";
        this.roles = new ArrayList<>();
    }

    public AuthResponse(String token, String type, Long accountId, String emailOrPhone, String fullName, List<String> roles) {
        this.token = token;
        this.type = type != null ? type : "Bearer";
        this.accountId = accountId;
        this.emailOrPhone = emailOrPhone;
        this.fullName = fullName;
        this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public void setEmailOrPhone(String emailOrPhone) {
        this.emailOrPhone = emailOrPhone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<String> getRoles() {
        return roles != null ? Collections.unmodifiableList(roles) : Collections.emptyList();
    }

    public void setRoles(List<String> roles) {
        this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "token='[PROTECTED]'" +
                ", type='" + type + '\'' +
                ", accountId=" + accountId +
                ", emailOrPhone='" + emailOrPhone + '\'' +
                ", fullName='" + fullName + '\'' +
                ", roles=" + roles +
                '}';
    }
}
