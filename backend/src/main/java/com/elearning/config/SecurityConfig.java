package com.elearning.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import com.elearning.security.JwtAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 Configuration for stateless REST API.
 * Configures stateless session management, disables CSRF for REST,
 * configures CORS with explicit origin allowlist, registers JwtAuthenticationFilter,
 * and sets authorization boundaries for public vs protected routes.
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
                // Enable CORS with Spring-managed CorsConfigurationSource
                .cors(Customizer.withDefaults())
                // Disable CSRF since REST API uses stateless token-based authentication
                .csrf(AbstractHttpConfigurer::disable)
                // Set session management to stateless (no HttpSession created or used)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Configure security response headers: preserve Spring Security defaults and enforce Referrer-Policy
                .headers(headers -> headers
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                        ))
                )
                // Configure request authorization boundaries
                .authorizeHttpRequests(auth -> auth
                        // Public operational health probe
                        .requestMatchers("/actuator/health").permitAll()
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
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"UNAUTHORIZED\",\"message\":\"Chưa xác thực hoặc phiên đăng nhập đã hết hạn\",\"errors\":[],\"data\":null}"
                            );
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
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,http://127.0.0.1:3000}") List<String> allowedOrigins,
            @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}") List<String> allowedMethods,
            @Value("${app.cors.allowed-headers:Authorization,Content-Type,Accept,Origin,X-Requested-With}") List<String> allowedHeaders,
            @Value("${app.cors.allow-credentials:false}") boolean allowCredentials,
            @Value("${app.cors.max-age-seconds:3600}") long maxAgeSeconds) {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins.stream().map(String::trim).toList());
        configuration.setAllowedMethods(allowedMethods.stream().map(String::trim).toList());
        configuration.setAllowedHeaders(allowedHeaders.stream().map(String::trim).toList());
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAgeSeconds);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
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
