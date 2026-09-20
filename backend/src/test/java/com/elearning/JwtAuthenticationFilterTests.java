package com.elearning;

import com.elearning.repository.AccountAuthSummary;
import com.elearning.repository.AccountRepository;
import com.elearning.security.JwtAuthenticationFilter;
import com.elearning.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("JwtAuthenticationFilter Unit / Mock Tests")
class JwtAuthenticationFilterTests {

    private static final String TEST_SECRET = "test-jwt-secret-key-for-filter-unit-tests-minimum-32-bytes";
    private static final long EXPIRATION_MS = 3600000L;

    private JwtUtil jwtUtil;
    private AccountRepository accountRepository;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, EXPIRATION_MS);
        accountRepository = Mockito.mock(AccountRepository.class);
        filter = new JwtAuthenticationFilter(jwtUtil, accountRepository);
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
        verify(accountRepository, never()).findAuthSummaryByEmailOrPhone(anyString());
    }

    @Test
    @DisplayName("GIVEN request with non-Bearer Authorization header WHEN filter executes THEN ignored and SecurityContext remains empty")
    void testNonBearerHeaderIgnored() throws Exception {
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Basic dXNlcjpwYXNzd29yZA==");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        verify(accountRepository, never()).findAuthSummaryByEmailOrPhone(anyString());
    }

    @Test
    @DisplayName("GIVEN request with empty Bearer prefix WHEN filter executes THEN chain continues and no authentication created")
    void testEmptyBearerHeader() throws Exception {
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer ");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        verify(accountRepository, never()).findAuthSummaryByEmailOrPhone(anyString());
    }

    @Test
    @DisplayName("GIVEN request with invalid JWT string WHEN filter executes THEN chain continues and no DB query executed")
    void testInvalidTokenDoesNotAuthenticate() throws Exception {
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer invalid.malformed.jwt");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        verify(accountRepository, never()).findAuthSummaryByEmailOrPhone(anyString());
    }

    @Test
    @DisplayName("GIVEN request with expired Bearer token WHEN filter executes THEN no DB query executed and no authentication set")
    void testExpiredTokenDoesNotAuthenticate() throws Exception {
        String expiredToken = jwtUtil.generateToken("expired@elearning.com", List.of("Learner"), 1L, -5000L);
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + expiredToken);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        verify(accountRepository, never()).findAuthSummaryByEmailOrPhone(anyString());
    }

    @Test
    @DisplayName("GIVEN request with missing issuer claim in JWT WHEN filter executes THEN SecurityContext remains empty and no DB query (BE-AUTH-003)")
    void testMissingIssuerTokenDoesNotAuthenticate() throws Exception {
        String tokenWithoutIssuer = io.jsonwebtoken.Jwts.builder()
                .subject("no-iss@elearning.com")
                .claim("roles", List.of("Learner"))
                .claim("auth_ver", 1L)
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 3600000L))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(TEST_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + tokenWithoutIssuer);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        verify(accountRepository, never()).findAuthSummaryByEmailOrPhone(anyString());
    }

    @Test
    @DisplayName("GIVEN request with wrong issuer claim in JWT WHEN filter executes THEN SecurityContext remains empty and no DB query (BE-AUTH-003)")
    void testWrongIssuerTokenDoesNotAuthenticate() throws Exception {
        JwtUtil foreignUtil = new JwtUtil(TEST_SECRET, EXPIRATION_MS, "rogue-issuer");
        String wrongIssuerToken = foreignUtil.generateToken("rogue@elearning.com", List.of("Learner"), 1L);

        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + wrongIssuerToken);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        verify(accountRepository, never()).findAuthSummaryByEmailOrPhone(anyString());
    }

    @Test
    @DisplayName("GIVEN request with valid Bearer token AND matching auth version AND Active account in DB WHEN filter executes THEN sets Authentication into SecurityContext")
    void testValidTokenWithActiveAccountAuthenticates() throws Exception {
        String validToken = jwtUtil.generateToken("student@elearning.com", List.of("Learner", "Creator"), 1L);
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + validToken);

        when(accountRepository.findAuthSummaryByEmailOrPhone("student@elearning.com"))
                .thenReturn(Optional.of(new AccountAuthSummary("Active", 1L)));

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
    @DisplayName("GIVEN token with missing auth_ver claim WHEN filter executes THEN SecurityContext remains null (BE-AUTH-004)")
    void testTokenWithMissingAuthVersionDoesNotAuthenticate() throws Exception {
        // Manually build token omitting auth_ver claim
        String tokenWithoutAuthVer = io.jsonwebtoken.Jwts.builder()
                .issuer("elearning-backend")
                .subject("legacy@elearning.com")
                .claim("roles", List.of("Learner"))
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 3600000L))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(TEST_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + tokenWithoutAuthVer);

        when(accountRepository.findAuthSummaryByEmailOrPhone("legacy@elearning.com"))
                .thenReturn(Optional.of(new AccountAuthSummary("Active", 1L)));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN token with stale auth_ver (1L) WHEN DB version is (2L) THEN SecurityContext remains null (BE-AUTH-004 Immediate Role Revocation)")
    void testTokenWithStaleAuthVersionDoesNotAuthenticate() throws Exception {
        String staleToken = jwtUtil.generateToken("revoked@elearning.com", List.of("Creator"), 1L);
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + staleToken);

        // Server-side account authorization_version has incremented to 2L
        when(accountRepository.findAuthSummaryByEmailOrPhone("revoked@elearning.com"))
                .thenReturn(Optional.of(new AccountAuthSummary("Active", 2L)));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN fresh token with updated auth_ver (2L) WHEN DB version is (2L) THEN sets Authentication into SecurityContext")
    void testFreshTokenWithUpdatedAuthVersionAuthenticates() throws Exception {
        String freshToken = jwtUtil.generateToken("promoted@elearning.com", List.of("Learner", "Creator"), 2L);
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + freshToken);

        when(accountRepository.findAuthSummaryByEmailOrPhone("promoted@elearning.com"))
                .thenReturn(Optional.of(new AccountAuthSummary("Active", 2L)));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("promoted@elearning.com");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_Learner", "ROLE_Creator");
    }

    @Test
    @DisplayName("GIVEN request with valid Bearer token BUT Disabled account in DB WHEN filter executes THEN SecurityContext remains null (BE-AUTH-001)")
    void testValidTokenWithDisabledAccountDoesNotAuthenticate() throws Exception {
        String validToken = jwtUtil.generateToken("disabled@elearning.com", List.of("Learner"), 1L);
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + validToken);

        when(accountRepository.findAuthSummaryByEmailOrPhone("disabled@elearning.com"))
                .thenReturn(Optional.of(new AccountAuthSummary("Disabled", 1L)));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN request with valid Bearer token BUT Banned/Locked account in DB WHEN filter executes THEN SecurityContext remains null")
    void testValidTokenWithBannedAccountDoesNotAuthenticate() throws Exception {
        String validToken = jwtUtil.generateToken("banned@elearning.com", List.of("Learner"), 1L);
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + validToken);

        when(accountRepository.findAuthSummaryByEmailOrPhone("banned@elearning.com"))
                .thenReturn(Optional.of(new AccountAuthSummary("Banned", 1L)));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN request with valid Bearer token BUT Nonexistent account in DB WHEN filter executes THEN SecurityContext remains null")
    void testValidTokenWithNonexistentAccountDoesNotAuthenticate() throws Exception {
        String validToken = jwtUtil.generateToken("ghost@elearning.com", List.of("Learner"), 1L);
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + validToken);

        when(accountRepository.findAuthSummaryByEmailOrPhone("ghost@elearning.com"))
                .thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GIVEN existing authentication in SecurityContext WHEN filter executes THEN does not overwrite existing authentication")
    void testDoesNotOverwriteExistingAuthentication() throws Exception {
        UsernamePasswordAuthenticationToken existingAuth =
                new UsernamePasswordAuthenticationToken("existing@elearning.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_Admin")));
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        String newToken = jwtUtil.generateToken("different@elearning.com", List.of("Learner"), 1L);
        request.addHeader(JwtAuthenticationFilter.AUTHORIZATION_HEADER, "Bearer " + newToken);

        filter.doFilter(request, response, filterChain);

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(currentAuth).isSameAs(existingAuth);
        assertThat(currentAuth.getName()).isEqualTo("existing@elearning.com");
        verify(accountRepository, never()).findAuthSummaryByEmailOrPhone(anyString());
    }
}
