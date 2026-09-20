package com.elearning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "server.forward-headers-strategy=none")
@AutoConfigureMockMvc
@DisplayName("Task R3.10: Forwarded Headers Disabled (none) Strategy Verification Tests")
class ForwardedHeadersDisabledStrategyIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GIVEN forward-headers-strategy=none WHEN client sends spoofed X-Forwarded-Proto: https THEN application ignores header and does NOT emit HSTS")
    void testSpoofedForwardedProtoIgnoredWhenStrategyIsNone() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/radicals")
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-For", "203.0.113.195")
                        .header("X-Forwarded-Host", "attacker.evil.com"))
                .andExpect(status().isOk())
                // In NONE mode, ForwardedHeaderFilter is NOT active, so request.isSecure() remains false
                // and Spring Security's HstsHeaderWriter does NOT attach Strict-Transport-Security
                .andExpect(header().doesNotExist("Strict-Transport-Security"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andReturn();

        System.out.println("=== NONE STRATEGY TEST (Spoofed X-Forwarded-Proto Ignored) ===");
        for (String name : result.getResponse().getHeaderNames()) {
            System.out.println("  " + name + ": " + result.getResponse().getHeader(name));
        }
        System.out.println("=============================================================");
    }
}
