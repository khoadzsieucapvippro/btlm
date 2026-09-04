package com.elearning;

import com.elearning.controller.AdminRoleController;
import com.elearning.dto.request.UpdateAccountRolesRequest;
import com.elearning.dto.response.AccountResponse;
import com.elearning.dto.response.RoleResponse;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.service.AdminRoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 8D.2: AdminRoleController Unit Contract Tests")
class AdminRoleControllerTests {

    @Mock
    private AdminRoleService adminRoleService;

    @InjectMocks
    private AdminRoleController adminRoleController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminRoleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GIVEN roles exist WHEN calling GET /api/v1/admin/roles THEN returns 200 with list of roles")
    void testGetRoles_success() throws Exception {
        List<RoleResponse> roles = List.of(
                new RoleResponse(1, "Learner"),
                new RoleResponse(2, "Creator"),
                new RoleResponse(3, "Moderator"),
                new RoleResponse(4, "Admin")
        );
        when(adminRoleService.getRoles()).thenReturn(roles);

        mockMvc.perform(get("/api/v1/admin/roles")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].roleName").value("Learner"))
                .andExpect(jsonPath("$.data[3].roleName").value("Admin"));
    }

    @Test
    @DisplayName("GIVEN valid roles payload WHEN calling PUT /api/v1/admin/accounts/{id}/roles THEN returns 200 with updated AccountResponse")
    void testUpdateAccountRoles_success() throws Exception {
        AccountResponse acc = new AccountResponse(1L, "user@example.com", "Test User", "Active", List.of("Learner", "Creator"), LocalDateTime.now(), LocalDateTime.now());
        when(adminRoleService.updateAccountRoles(eq(1L), any(UpdateAccountRolesRequest.class), any())).thenReturn(acc);

        UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of("Learner", "Creator"));

        mockMvc.perform(put("/api/v1/admin/accounts/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Phân quyền tài khoản thành công"))
                .andExpect(jsonPath("$.data.roles[0]").value("Learner"))
                .andExpect(jsonPath("$.data.roles[1]").value("Creator"));
    }
}
