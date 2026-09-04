package com.elearning;

import com.elearning.dto.request.LoginRequest;
import com.elearning.repository.AccountRepository;
import com.elearning.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Task R3.10: HTTP Security Headers & Reverse Proxy Forwarding Integration Tests")
class HttpSecurityHeadersIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private void printHeaders(String testName, MvcResult result) {
        System.out.println("=== HEADERS FOR: " + testName + " ===");
        Collection<String> headerNames = result.getResponse().getHeaderNames();
        for (String name : headerNames) {
            System.out.println("  " + name + ": " + result.getResponse().getHeader(name));
        }
        System.out.println("======================================");
    }

    @Nested
    @DisplayName("1. Default Security Headers on Public Endpoints")
    class PublicEndpointsHeadersTests {

        @Test
        @DisplayName("GIVEN direct GET /api/v1/radicals over HTTP WHEN executed THEN standard Spring Security defaults and Referrer-Policy are present")
        void testRadicalsPublicGetHeaders() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/radicals"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("X-XSS-Protection", "0"))
                    .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                    .andExpect(header().string("Pragma", "no-cache"))
                    .andExpect(header().string("Expires", "0"))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                    .andExpect(header().doesNotExist("Strict-Transport-Security"))
                    .andReturn();

            printHeaders("GET /api/v1/radicals (Direct HTTP)", result);
        }

        @Test
        @DisplayName("GIVEN POST /api/v1/auth/login invalid credentials over HTTP THEN security headers are preserved")
        void testLoginPostHeaders() throws Exception {
            LoginRequest loginRequest = new LoginRequest("nonexistent@test.com", "WrongPassword123");

            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("X-XSS-Protection", "0"))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                    .andExpect(header().doesNotExist("Strict-Transport-Security"))
                    .andReturn();

            printHeaders("POST /api/v1/auth/login (Direct HTTP)", result);
        }
    }

    @Nested
    @DisplayName("2. Error Responses Security Headers Preservation")
    class ErrorResponsesHeadersTests {

        @Test
        @DisplayName("GIVEN unauthenticated request to protected endpoint (401) THEN security headers are preserved")
        void test401UnauthorizedHeaders() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                    .andReturn();

            printHeaders("401 Unauthorized /api/v1/admin/users", result);
        }

        @Test
        @DisplayName("GIVEN authenticated user with Learner role accessing admin endpoint (403) THEN security headers are preserved")
        void test403ForbiddenHeaders() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/admin/users")
                            .with(user("learner").roles("Learner")))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                    .andReturn();

            printHeaders("403 Forbidden /api/v1/admin/users", result);
        }

        @Test
        @DisplayName("GIVEN non-existent endpoint (404) for authenticated user THEN security headers are preserved")
        void test404NotFoundHeaders() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/non-existent-endpoint-12345")
                            .with(user("learner").roles("Learner")))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                    .andReturn();

            printHeaders("404 Not Found", result);
        }

        @Test
        @DisplayName("GIVEN unsupported HTTP method triggering exception (500) THEN security headers are preserved")
        void testExceptionResponseHeadersPreserved() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/radicals")
                            .with(user("learner").roles("Learner")))
                    .andExpect(status().isInternalServerError())
                    .andReturn();

            printHeaders("POST /api/v1/radicals (500 Internal Server Error)", result);
            assertThat(result.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
            assertThat(result.getResponse().getHeader("X-Frame-Options")).isEqualTo("DENY");
            assertThat(result.getResponse().getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
            assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-cache");
        }
    }

    @Nested
    @DisplayName("3. Direct HTTPS Simulation & HSTS Behavior")
    class DirectHstsBehaviorTests {

        @Test
        @DisplayName("GIVEN direct request over HTTPS (.secure(true)) WHEN executed THEN Strict-Transport-Security header is present with max-age=31536000")
        void testHstsPresentOnDirectHttps() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/radicals").secure(true))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Strict-Transport-Security"))
                    .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"))
                    .andReturn();

            printHeaders("GET /api/v1/radicals (Direct HTTPS)", result);
        }

        @Test
        @DisplayName("GIVEN direct request over HTTP (.secure(false)) WHEN executed THEN Strict-Transport-Security header is NOT present")
        void testHstsAbsentOnDirectHttp() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/radicals").secure(false))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("Strict-Transport-Security"))
                    .andReturn();

            printHeaders("GET /api/v1/radicals (Direct HTTP - No HSTS)", result);
        }
    }

    @Nested
    @DisplayName("4. Content Security Policy (CSP) & Optional Headers Audit")
    class CspAndOptionalHeadersAuditTests {

        @Test
        @DisplayName("GIVEN API response WHEN inspected THEN verify presence of Referrer-Policy and documented absence of CSP")
        void testAuditOptionalHeaders() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/radicals"))
                    .andExpect(status().isOk())
                    .andReturn();

            // Referrer-Policy explicitly configured
            assertThat(result.getResponse().getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");

            // CSP is intentionally deferred to Phase 9 pending frontend resource graph
            assertThat(result.getResponse().getHeader("Content-Security-Policy")).isNull();
            assertThat(result.getResponse().getHeader("Content-Security-Policy-Report-Only")).isNull();

            // Unneeded headers for stateless REST API
            assertThat(result.getResponse().getHeader("Permissions-Policy")).isNull();
            assertThat(result.getResponse().getHeader("Cross-Origin-Opener-Policy")).isNull();
            assertThat(result.getResponse().getHeader("Cross-Origin-Resource-Policy")).isNull();
            assertThat(result.getResponse().getHeader("Cross-Origin-Embedder-Policy")).isNull();
        }
    }

    @Nested
    @DisplayName("5. Reverse Proxy Forwarded Headers & Scheme Recognition (framework strategy)")
    class ReverseProxyForwardedHeadersTests {

        @Test
        @DisplayName("GIVEN trusted proxy request with X-Forwarded-Proto: https WHEN forward-headers-strategy is framework THEN isSecure=true and HSTS is emitted")
        void testTrustedProxyHttpsEmitsHsts() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/radicals")
                            .header("X-Forwarded-Proto", "https")
                            .header("X-Forwarded-For", "203.0.113.195")
                            .header("X-Forwarded-Host", "elearning.example.com"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Strict-Transport-Security"))
                    .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"))
                    .andReturn();

            printHeaders("Proxy with X-Forwarded-Proto: https", result);
        }

        @Test
        @DisplayName("GIVEN trusted proxy request with X-Forwarded-Proto: http WHEN forward-headers-strategy is framework THEN isSecure=false and HSTS is NOT emitted")
        void testTrustedProxyHttpDoesNotEmitHsts() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/radicals")
                            .header("X-Forwarded-Proto", "http")
                            .header("X-Forwarded-For", "203.0.113.195")
                            .header("X-Forwarded-Host", "elearning.example.com"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("Strict-Transport-Security"))
                    .andReturn();

            printHeaders("Proxy with X-Forwarded-Proto: http", result);
        }

        @Test
        @DisplayName("GIVEN direct request without forwarded headers (localhost dev) THEN scheme is HTTP and HSTS is NOT emitted")
        void testDirectLocalhostDevBehaviorPreserved() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/radicals"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("Strict-Transport-Security"))
                    .andReturn();

            printHeaders("Direct request (no forwarded headers)", result);
        }

        @Test
        @DisplayName("GIVEN requests from distinct client IPs via X-Forwarded-For WHEN hitting login rate limit THEN rate limits are isolated per client IP")
        void testForwardedForIpIsolatedRateLimiting() throws Exception {
            String clientIpA = "198.51.100.1";
            String clientIpB = "198.51.100.2";
            LoginRequest req = new LoginRequest("nonexistent@test.com", "WrongPassword123");
            String body = objectMapper.writeValueAsString(req);

            // Client A exhausts all 10 allowed attempts
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .header("X-Forwarded-For", clientIpA)
                                .header("X-Forwarded-Proto", "https"))
                        .andExpect(status().isUnauthorized());
            }

            // 11th attempt from Client A is rejected with 429 Too Many Requests
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .header("X-Forwarded-For", clientIpA)
                            .header("X-Forwarded-Proto", "https"))
                    .andExpect(status().isTooManyRequests());

            // Client B has its own bucket and is NOT blocked by Client A's activity
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .header("X-Forwarded-For", clientIpB)
                            .header("X-Forwarded-Proto", "https"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
