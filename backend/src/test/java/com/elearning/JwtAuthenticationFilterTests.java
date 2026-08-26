package com.elearning;

import com.elearning.security.JwtAuthenticationFilter;
import com.elearning.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtAuthenticationFilter Unit / Mock Tests")
class JwtAuthenticationFilterTests {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000L;

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, EXPIRATION_MS);
        filter = new JwtAuthenticationFilter(jwtUtil);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GIVEN request without Authorization header WHEN filter executes THEN chain continues and SecurityContext remains empty")
    void testNoAuthorizationHeader() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN request with non-Bearer Authorization header WHEN filter executes THEN ignored and SecurityContext remains empty")
    void testNonBearerHeaderIgnored() throws Exception {
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Basic dXNlcjpwYXNzd29yZA==");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN request with empty Bearer prefix WHEN filter executes THEN chain continues and no authentication created")
    void testEmptyBearerHeader() throws Exception {
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer ");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN request with invalid JWT string WHEN filter executes THEN chain continues and no authentication created")
    void testInvalidTokenDoesNotAuthenticate() throws Exception {
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer invalid.malformed.jwt");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN request with expired Bearer token WHEN filter executes THEN no authentication is set into SecurityContext")
    void testExpiredTokenDoesNotAuthenticate() throws Exception {
        String expiredToken = jwtUtil.generateToken("expired@elearning.com", List.of("Learner"), -5000L);
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + expiredToken);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN request with valid Bearer token WHEN filter executes THEN sets Authentication with subject and roles into SecurityContext")
    void testValidTokenAuthenticates() throws Exception {
        String validToken = jwtUtil.generateToken("student@elearning.com", List.of("Learner", "Creator"));
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + validToken);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("student@elearning.com");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_Learner", "ROLE_Creator");
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN existing authentication in SecurityContext WHEN filter executes THEN does not overwrite existing authentication")
    void testDoesNotOverwriteExistingAuthentication() throws Exception {
        UsernamePasswordAuthenticationToken existingAuth =
                new UsernamePasswordAuthenticationToken("existing@elearning.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_Admin")));
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        String newToken = jwtUtil.generateToken("different@elearning.com", List.of("Learner"));
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + newToken);

        filter.doFilter(request, response, filterChain);

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(currentAuth).isSameAs(existingAuth);
        assertThat(currentAuth.getName()).isEqualTo("existing@elearning.com");
    }
}
