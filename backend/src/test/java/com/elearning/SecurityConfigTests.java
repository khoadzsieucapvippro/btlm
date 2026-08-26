package com.elearning;

import com.elearning.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfigTests.TestEndpointConfig.class)
@DisplayName("SecurityConfig & PasswordEncoder Tests")
class SecurityConfigTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.elearning.security.JwtUtil jwtUtil;

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestEndpointConfig {
        @RestController
        static class TestSecurityController {

            @PostMapping("/api/v1/auth/test-post")
            public String testAuthPost() {
                return "auth-post-ok";
            }

            @GetMapping("/api/v1/radicals/test-public")
            public String testRadicalsGet() {
                return "radicals-public-ok";
            }

            @PostMapping("/api/v1/radicals/test-post")
            public String testRadicalsPost() {
                return "radicals-post-ok";
            }

            @GetMapping("/api/v1/vocabulary/test-public")
            public String testVocabularyGet() {
                return "vocabulary-public-ok";
            }

            @GetMapping("/api/v1/lessons/test-public")
            public String testLessonsGet() {
                return "lessons-public-ok";
            }

            @GetMapping("/api/v1/protected/resource")
            public String testProtectedGet() {
                return "protected-ok";
            }
        }
    }

    @Nested
    @DisplayName("Context & Bean Definitions")
    class ContextAndBeanTests {

        @Test
        @DisplayName("GIVEN Spring Boot application WHEN context loads THEN SecurityFilterChain and PasswordEncoder beans exist")
        void testBeansLoaded() {
            assertThat(securityFilterChain).isNotNull();
            assertThat(passwordEncoder).isNotNull();
            assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
            assertThat(applicationContext.containsBean("securityFilterChain")).isTrue();
            assertThat(applicationContext.containsBean("passwordEncoder")).isTrue();
        }
    }

    @Nested
    @DisplayName("BCryptPasswordEncoder Tests")
    class PasswordEncoderTests {

        @Test
        @DisplayName("GIVEN raw password WHEN encoded THEN produces valid BCrypt hash differing from raw")
        void testBCryptEncoding() {
            String rawPassword = "P@ssw0rdSecure123!";
            String encoded = passwordEncoder.encode(rawPassword);

            assertThat(encoded).isNotNull();
            assertThat(encoded).isNotEqualTo(rawPassword);
            assertThat(encoded).startsWith("$2a$");
            assertThat(encoded).hasSize(60);
        }

        @Test
        @DisplayName("GIVEN encoded password WHEN matched with correct and wrong passwords THEN returns true and false")
        void testBCryptMatches() {
            String rawPassword = "CorrectPassword2026";
            String wrongPassword = "WrongPassword2026";
            String encoded = passwordEncoder.encode(rawPassword);

            assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
            assertThat(passwordEncoder.matches(wrongPassword, encoded)).isFalse();
        }

        @Test
        @DisplayName("GIVEN same raw password encoded twice WHEN compared THEN produces distinct hashes due to salts but both match")
        void testBCryptSaltUniqueness() {
            String rawPassword = "SamePasswordTwice";
            String hash1 = passwordEncoder.encode(rawPassword);
            String hash2 = passwordEncoder.encode(rawPassword);

            assertThat(hash1).isNotEqualTo(hash2);
            assertThat(passwordEncoder.matches(rawPassword, hash1)).isTrue();
            assertThat(passwordEncoder.matches(rawPassword, hash2)).isTrue();
        }
    }

    @Nested
    @DisplayName("Stateless Session & CSRF Tests")
    class StatelessAndCsrfTests {

        @Test
        @DisplayName("GIVEN stateless session configuration WHEN requests are made THEN no JSESSIONID cookie is set and session is not created")
        void testStatelessSessionBehavior() throws Exception {
            mockMvc.perform(get("/api/v1/radicals/test-public"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("Set-Cookie"));
        }

        @Test
        @DisplayName("GIVEN disabled CSRF for REST WHEN unsafe POST request made without CSRF token THEN request is not rejected with 403")
        void testCsrfDisabledForRest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/test-post")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Request Authorization Boundary Tests")
    class AuthorizationBoundaryTests {

        @Test
        @DisplayName("GIVEN unauthenticated request to /api/v1/auth/** WHEN executed THEN access is permitted")
        void testPublicAuthEndpointsPermitted() throws Exception {
            mockMvc.perform(post("/api/v1/auth/test-post")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GIVEN unauthenticated GET request to catalog endpoints WHEN executed THEN access is permitted")
        void testPublicCatalogGetPermitted() throws Exception {
            mockMvc.perform(get("/api/v1/radicals/test-public"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/vocabulary/test-public"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/lessons/test-public"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GIVEN unauthenticated request to protected endpoint WHEN executed THEN access is rejected by Spring Security")
        void testProtectedEndpointRejectedWhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/protected/resource"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GIVEN unauthenticated POST request to catalog endpoint WHEN executed THEN access is rejected as protected")
        void testCatalogNonGetRejectedWhenUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/radicals/test-post"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GIVEN valid JWT Bearer token WHEN accessing protected endpoint THEN access is granted")
        void testProtectedEndpointAllowedWithValidJwt() throws Exception {
            String token = jwtUtil.generateToken("authed-user@elearning.com", java.util.List.of("Learner"));

            mockMvc.perform(get("/api/v1/protected/resource")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GIVEN invalid JWT Bearer token WHEN accessing protected endpoint THEN access is rejected")
        void testProtectedEndpointRejectedWithInvalidJwt() throws Exception {
            mockMvc.perform(get("/api/v1/protected/resource")
                            .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isForbidden());
        }
    }
}
