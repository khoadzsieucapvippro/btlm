package com.elearning;

import com.elearning.security.JwtUtil;
import com.elearning.test.RbacTestControllers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verification and Security Tests for BE-CORS-001: CORS Configuration for Cross-Origin Clients.
 * Verifies that:
 * 1. Explicitly allowed frontend origins receive correct CORS headers.
 * 2. Disallowed origins receive no permissive Access-Control-Allow-Origin header and are rejected with 403 Forbidden.
 * 3. Preflight OPTIONS requests are processed before security filters without demanding authentication.
 * 4. Allowed methods and headers are strictly enforced.
 * 5. Bearer JWT authentication and RBAC authorization remain fully enforced for cross-origin requests.
 * 6. Same-origin requests without Origin headers function normally without regression.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({
        RbacTestControllers.AdminTestController.class,
        RbacTestControllers.CreatorTestController.class
})
@DisplayName("BE-CORS-001: CORS Security & Preflight Integration Tests")
class CorsSecurityIntegrationTests {

    private static final String ALLOWED_ORIGIN_5500 = "http://localhost:5500";
    private static final String ALLOWED_ORIGIN_IP_5500 = "http://127.0.0.1:5500";
    private static final String ALLOWED_ORIGIN_3000 = "http://localhost:3000";
    private static final String ALLOWED_ORIGIN_IP_3000 = "http://127.0.0.1:3000";
    private static final String DISALLOWED_ORIGIN_EVIL = "http://evil.com";
    private static final String DISALLOWED_ORIGIN_ATTACKER = "https://attacker.example.org";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.elearning.repository.AccountRepository accountRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        accountRepository.findByEmailOrPhone("creator@elearning.com")
                .orElseGet(() -> {
                    com.elearning.entity.Account acc = new com.elearning.entity.Account();
                    acc.setEmailOrPhone("creator@elearning.com");
                    acc.setPasswordHash("hash1234567890");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });

        accountRepository.findByEmailOrPhone("learner@elearning.com")
                .orElseGet(() -> {
                    com.elearning.entity.Account acc = new com.elearning.entity.Account();
                    acc.setEmailOrPhone("learner@elearning.com");
                    acc.setPasswordHash("hash1234567890");
                    acc.setStatus("Active");
                    return accountRepository.save(acc);
                });
    }

    @Nested
    @DisplayName("1. Allowed Origin Tests")
    class AllowedOriginTests {

        @Test
        @DisplayName("GIVEN request with allowed origin http://localhost:5500 WHEN GET public API THEN returns 200 and Access-Control-Allow-Origin header")
        void testAllowedOrigin_localhost5500() throws Exception {
            mockMvc.perform(get("/api/v1/radicals")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_5500))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_5500))
                    .andExpect(header().string(HttpHeaders.VARY, Matchers.containsString("Origin")));
        }

        @Test
        @DisplayName("GIVEN request with allowed origin http://127.0.0.1:5500 WHEN GET public API THEN returns 200 and Access-Control-Allow-Origin header")
        void testAllowedOrigin_127001_5500() throws Exception {
            mockMvc.perform(get("/api/v1/radicals")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_IP_5500))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_IP_5500));
        }

        @Test
        @DisplayName("GIVEN request with allowed origin http://localhost:3000 WHEN GET public API THEN returns 200 and Access-Control-Allow-Origin header")
        void testAllowedOrigin_localhost3000() throws Exception {
            mockMvc.perform(get("/api/v1/radicals")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_3000))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_3000));
        }

        @Test
        @DisplayName("GIVEN request with allowed origin http://127.0.0.1:3000 WHEN GET public API THEN returns 200 and Access-Control-Allow-Origin header")
        void testAllowedOrigin_127001_3000() throws Exception {
            mockMvc.perform(get("/api/v1/radicals")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_IP_3000))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_IP_3000));
        }
    }

    @Nested
    @DisplayName("2. Disallowed Origin Tests")
    class DisallowedOriginTests {

        @Test
        @DisplayName("GIVEN request with disallowed origin http://evil.com WHEN GET API THEN returns 403 Forbidden with NO Access-Control-Allow-Origin")
        void testDisallowedOrigin_rejectedWith403() throws Exception {
            mockMvc.perform(get("/api/v1/radicals")
                            .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN_EVIL))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }

        @Test
        @DisplayName("GIVEN preflight OPTIONS request from disallowed origin WHEN executed THEN returns 403 Forbidden without ACAO")
        void testDisallowedOrigin_preflightRejected() throws Exception {
            mockMvc.perform(options("/api/v1/auth/login")
                            .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN_EVIL)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

            mockMvc.perform(options("/api/v1/radicals")
                            .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN_ATTACKER)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    @Nested
    @DisplayName("3. Preflight (OPTIONS) Handling Tests")
    class PreflightTests {

        @Test
        @DisplayName("GIVEN valid preflight request with POST and Content-Type WHEN OPTIONS /api/v1/auth/login THEN returns 200 with allowed methods and headers")
        void testPreflight_postLogin() throws Exception {
            mockMvc.perform(options("/api/v1/auth/login")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_5500)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_5500))
                    .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                    .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
        }

        @Test
        @DisplayName("GIVEN valid preflight request with GET and Authorization header WHEN OPTIONS protected endpoint THEN succeeds without authentication")
        void testPreflight_protectedEndpointWithAuthorizationHeader() throws Exception {
            mockMvc.perform(options("/api/v1/creator/test")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_5500)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_5500))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization"));
        }

        @Test
        @DisplayName("GIVEN preflight with disallowed HTTP method (e.g. TRACE) WHEN executed THEN returns 403 Forbidden")
        void testPreflight_disallowedMethod() throws Exception {
            mockMvc.perform(options("/api/v1/radicals")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_5500)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "TRACE"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GIVEN preflight with disallowed header (e.g. X-Malicious-Header) WHEN executed THEN returns 403 Forbidden")
        void testPreflight_disallowedHeader() throws Exception {
            mockMvc.perform(options("/api/v1/radicals")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_5500)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Malicious-Header"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("4. Authentication & Authorization Independence Tests")
    class AuthAndAuthzIndependenceTests {

        @Test
        @DisplayName("GIVEN unauthenticated request from allowed origin to protected endpoint WHEN executed THEN returns 401 Unauthorized with CORS header")
        void testProtectedEndpoint_unauthenticatedReturns401WithCors() throws Exception {
            mockMvc.perform(get("/api/v1/creator/test")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_5500))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_5500));
        }

        @Test
        @DisplayName("GIVEN request with valid Creator JWT from allowed origin to Creator endpoint WHEN executed THEN returns 200 OK with CORS header")
        void testProtectedEndpoint_validJwtReturns200WithCors() throws Exception {
            String token = jwtUtil.generateToken("creator@elearning.com", List.of("Creator"));

            mockMvc.perform(get("/api/v1/creator/test")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_5500)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_5500));
        }

        @Test
        @DisplayName("GIVEN request with Learner JWT from allowed origin to Admin endpoint WHEN executed THEN returns 403 Forbidden with CORS header")
        void testAdminEndpoint_learnerJwtReturns403WithCors() throws Exception {
            String token = jwtUtil.generateToken("learner@elearning.com", List.of("Learner"));

            mockMvc.perform(get("/api/v1/admin/test")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_5500)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_5500));
        }
    }

    @Nested
    @DisplayName("5. Same-Origin & Non-CORS Request Tests")
    class SameOriginAndScopeTests {

        @Test
        @DisplayName("GIVEN same-origin request without Origin header WHEN GET public API THEN returns 200 with no ACAO header")
        void testSameOrigin_noOriginHeader() throws Exception {
            mockMvc.perform(get("/api/v1/radicals"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }
}
