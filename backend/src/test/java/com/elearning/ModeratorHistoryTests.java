package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.ModerationLog;
import com.elearning.entity.Role;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.ModerationLogRepository;
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
@DisplayName("Task 8D.3: Moderator History Integration Tests")
class ModeratorHistoryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ModerationLogRepository moderationLogRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Account mod1;
    private Account mod2;
    private Account admin;
    private Account learner;

    private String mod1Token;
    private String adminToken;
    private String learnerToken;

    @BeforeEach
    void setUp() {
        Role roleLearner = roleRepository.findByRoleName("Learner").orElseGet(() -> roleRepository.save(new Role(1, "Learner")));
        Role roleModerator = roleRepository.findByRoleName("Moderator").orElseGet(() -> roleRepository.save(new Role(3, "Moderator")));
        Role roleAdmin = roleRepository.findByRoleName("Admin").orElseGet(() -> roleRepository.save(new Role(4, "Admin")));

        mod1 = accountRepository.findByEmailOrPhone("mod1_8d3@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("mod1_8d3@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashmod18d31234567890123");
                    acc.setStatus("Active");
                    acc.addRole(roleModerator);
                    return accountRepository.save(acc);
                });

        mod2 = accountRepository.findByEmailOrPhone("mod2_8d3@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("mod2_8d3@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashmod28d31234567890123");
                    acc.setStatus("Active");
                    acc.addRole(roleModerator);
                    return accountRepository.save(acc);
                });

        admin = accountRepository.findByEmailOrPhone("admin_8d3@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("admin_8d3@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashadmin8d3123456789012");
                    acc.setStatus("Active");
                    acc.addRole(roleAdmin);
                    return accountRepository.save(acc);
                });

        learner = accountRepository.findByEmailOrPhone("learner_8d3@example.com")
                .orElseGet(() -> {
                    Account acc = new Account();
                    acc.setEmailOrPhone("learner_8d3@example.com");
                    acc.setPasswordHash("$2a$12$dummyhashlearner8d31234567890");
                    acc.setStatus("Active");
                    acc.addRole(roleLearner);
                    return accountRepository.save(acc);
                });

        mod1Token = jwtUtil.generateToken(mod1.getEmailOrPhone(), List.of("Moderator"), 1L);
        adminToken = jwtUtil.generateToken(admin.getEmailOrPhone(), List.of("Admin"), 1L);
        learnerToken = jwtUtil.generateToken(learner.getEmailOrPhone(), List.of("Learner"), 1L);

        Lesson lesson = new Lesson();
        lesson.setTitle("History Test Lesson");
        lesson.setStatus("Approved");
        lesson.setCreatedBy(admin);
        lesson = lessonRepository.save(lesson);

        // Seed logs: 1 by mod1, 1 by mod2
        ModerationLog log1 = new ModerationLog(lesson, mod1, "Approve", null, null);
        moderationLogRepository.save(log1);

        ModerationLog log2 = new ModerationLog(lesson, mod2, "Reject", "Inappropriate content", "title");
        moderationLogRepository.save(log2);
    }

    @Nested
    @DisplayName("GET /api/v1/moderator/history Tests")
    class GetHistoryTests {

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN calling GET history THEN returns 401 Unauthorized")
        void testGetHistory_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/history"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("GIVEN learner request WHEN calling GET history THEN returns 403 Forbidden")
        void testGetHistory_learner_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/history")
                            .header("Authorization", "Bearer " + learnerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")));
        }

        @Test
        @DisplayName("GIVEN moderator request WHEN calling GET history THEN returns only caller's personal moderation logs")
        void testGetHistory_moderator_returnsOwnLogsOnly() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/history")
                            .header("Authorization", "Bearer " + mod1Token)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.items.length()", is(1)))
                    .andExpect(jsonPath("$.data.items[0].moderatorEmail", is("mod1_8d3@example.com")))
                    .andExpect(jsonPath("$.data.items[0].action", is("Approve")));
        }

        @Test
        @DisplayName("GIVEN admin request WHEN calling GET history THEN returns global moderation logs from all moderators")
        void testGetHistory_admin_returnsGlobalLogs() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/history")
                            .header("Authorization", "Bearer " + adminToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(2)));
        }
    }
}
