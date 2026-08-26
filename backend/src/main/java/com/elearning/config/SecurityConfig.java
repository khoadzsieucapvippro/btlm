package com.elearning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.elearning.security.JwtAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 Configuration for stateless REST API.
 * Configures stateless session management, disables CSRF for REST,
 * registers JwtAuthenticationFilter, and sets authorization boundaries for public vs protected routes.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF since REST API uses stateless token-based authentication
                .csrf(AbstractHttpConfigurer::disable)
                // Set session management to stateless (no HttpSession created or used)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure request authorization boundaries
                .authorizeHttpRequests(auth -> auth
                        // Public authentication endpoints (register, login)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Public content lookup endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/radicals/**", "/api/v1/vocabulary/**", "/api/v1/lessons/**").permitAll()
                        // Allow Spring Boot error endpoint
                        .requestMatchers("/error").permitAll()
                        // Role-based authorization boundaries
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("Admin", "ADMIN")
                        .requestMatchers("/api/v1/moderator/**").hasAnyRole("Moderator", "Admin", "MODERATOR", "ADMIN")
                        .requestMatchers("/api/v1/creator/**").hasAnyRole("Creator", "Admin", "CREATOR", "ADMIN")
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                // Configure authentication entry point and access denied handler
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String uri = request.getRequestURI();
                            if (uri.startsWith("/api/v1/protected/") || uri.startsWith("/api/v1/radicals/")) {
                                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                            } else {
                                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                                response.setCharacterEncoding("UTF-8");
                                response.getWriter().write(
                                        "{\"code\":\"UNAUTHORIZED\",\"message\":\"Chưa xác thực hoặc phiên đăng nhập đã hết hạn\",\"errors\":[],\"data\":null}"
                                );
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"FORBIDDEN\",\"message\":\"Không có quyền truy cập tài nguyên này\",\"errors\":[],\"data\":null}"
                            );
                        })
                )
                // Register JWT authentication filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
