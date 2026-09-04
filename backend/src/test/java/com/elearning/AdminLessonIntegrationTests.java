package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.Role;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 8D.4: Admin Global Lesson Oversight Integration Tests")
class AdminLessonIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Account adminAccount;
    private Account learnerAccount;
    private String adminToken;
    private String learnerToken;

    @BeforeEach
    void setUp() {
        Role roleLearner = roleRepository.findByRoleName("Learner").orElseGet(() -> roleRepository.save(new Role(1, "Learner")));
        Role roleAdmin = roleRepository.findByRoleName("Admin").orElseGet(() -> roleRepository.save(new Role(4, "Admin")));

        adminAccount = accountRepository.findByEmailOrPhone("admin_8d4@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("admin_8d4@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashadmin8d41234567890123");
                    acc.setStatus("Active");
                    acc.addRole(roleAdmin);
                    return accountRepository.save(acc);
                });

        learnerAccount = accountRepository.findByEmailOrPhone("learner_8d4@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("learner_8d4@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashlearner8d412345678901");
                    acc.setStatus("Active");
                    acc.addRole(roleLearner);
                    return accountRepository.save(acc);
                });

        adminToken = jwtUtil.generateToken(adminAccount.getEmailOrPhone(), List.of("Admin"), 1L);
        learnerToken = jwtUtil.generateToken(learnerAccount.getEmailOrPhone(), List.of("Learner"), 1L);

        // Seed lessons in various statuses
        createLesson("Admin Lesson Draft", "Draft");
        createLesson("Admin Lesson Pending", "Pending");
        createLesson("Admin Lesson Approved", "Approved");
        createLesson("Admin Lesson Rejected", "Rejected");
    }

    private void createLesson(String title, String status) {
        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setStatus(status);
        lesson.setCreatedBy(adminAccount);
        lessonRepository.save(lesson);
    }

    @Nested
    @DisplayName("GET /api/v1/admin/lessons Tests")
    class GetAdminLessonsTests {

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN calling GET /api/v1/admin/lessons THEN returns 401 Unauthorized")
        void testGetAdminLessons_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/admin/lessons"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("GIVEN non-admin user WHEN calling GET /api/v1/admin/lessons THEN returns 403 Forbidden")
        void testGetAdminLessons_nonAdmin_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/admin/lessons")
                            .header("Authorization", "Bearer " + learnerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("GIVEN admin user without status filter WHEN calling GET /api/v1/admin/lessons THEN returns all lessons across all statuses")
        void testGetAdminLessons_allStatuses_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/admin/lessons")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(4)));
        }

        @Test
        @DisplayName("GIVEN admin user with status filter 'Draft' WHEN calling GET /api/v1/admin/lessons THEN returns only Draft lessons")
        void testGetAdminLessons_filteredDraft_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/admin/lessons")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("status", "Draft")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.items[0].status", is("Draft")));
        }
    }
}
