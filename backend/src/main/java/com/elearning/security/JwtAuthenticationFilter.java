package com.elearning.security;

import com.elearning.repository.AccountAuthSummary;
import com.elearning.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filter that intercepts incoming HTTP requests, extracts Bearer JWT from
 * Authorization header, cryptographically validates the token via JwtUtil,
 * verifies current authoritative account active status and authorization version
 * via AccountRepository, and sets Authentication in SecurityContextHolder.
 *
 * Security hardening:
 * 1. BE-AUTH-001: Cryptographic validation of signature/expiration occurs first.
 *    Server-side account status is checked to reject disabled or banned accounts.
 * 2. BE-AUTH-004: Server-side authorization version is checked against JWT auth_ver claim
 *    for immediate authorization/role revocation upon role mutations without full entity loading.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final AccountRepository accountRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, AccountRepository accountRepository) {
        this.jwtUtil = jwtUtil;
        this.accountRepository = accountRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();

            if (!token.isEmpty() && jwtUtil.validateToken(token)) {
                String subject = jwtUtil.extractSubject(token);

                if (subject != null && !subject.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Long tokenAuthVer = jwtUtil.extractAuthorizationVersion(token);
                    Optional<AccountAuthSummary> authSummaryOpt = accountRepository.findAuthSummaryByEmailOrPhone(subject);

                    if (authSummaryOpt.isPresent()) {
                        AccountAuthSummary summary = authSummaryOpt.get();
                        if ("Active".equalsIgnoreCase(summary.status())
                                && tokenAuthVer != null
                                && tokenAuthVer.equals(summary.authorizationVersion())) {
                            List<String> roles = jwtUtil.extractRoles(token);
                            List<SimpleGrantedAuthority> authorities = (roles != null)
                                    ? roles.stream()
                                            .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                                            .toList()
                                    : List.of();

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(subject, null, authorities);
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
