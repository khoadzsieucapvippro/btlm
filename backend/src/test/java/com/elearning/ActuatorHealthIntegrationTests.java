package com.elearning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Task 8D.7: Actuator Health Probe Integration Tests")
class ActuatorHealthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GIVEN unauthenticated request to /actuator/health WHEN executed THEN returns 200 OK with status UP")
    void testActuatorHealth_publicAccess_returns200Up() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("GIVEN unauthenticated request to unexposed actuator endpoint WHEN executed THEN rejected")
    void testUnexposedActuatorEndpoints_rejected() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }
}
