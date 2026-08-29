package com.elearning;

import com.elearning.dto.request.CreateLessonRequest;
import com.elearning.dto.request.ReorderVocabRequest;
import com.elearning.dto.request.UpdateLessonRequest;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-End MockMvc Integration Tests for Creator Lesson Studio (Module 5B).
 * Verifies Checkpoint 5B:
 * - Creator lifecycle: create draft lesson -> add vocab -> reorder -> submit for moderation.
 * - Creator Ownership Isolation: Creator A modifying Creator B's lesson is blocked with 403 Forbidden.
 * - Role-based security boundary: 401 for anonymous, 403 for Learner/Moderator, 200/201 for Creator/Admin.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 5B.2: CreatorLessonController MockMvc Integration & Checkpoint 5B Tests")
class CreatorLessonIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    private ObjectMapper objectMapper;

    private Account creatorAccountA;
    private Account creatorAccountB;
    private Vocabulary vocab1;
    private Vocabulary vocab2;
    private Vocabulary vocab3;

    private String creatorTokenA;
    private String creatorTokenB;
    private String learnerToken;
    private String moderatorToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        creatorAccountA = new Account();
        creatorAccountA.setEmailOrPhone("creator_alpha@example.com");
        creatorAccountA.setPasswordHash("$2a$12$dummyhashcreatorAlpha1234567890");
        creatorAccountA.setStatus("Active");
        creatorAccountA = accountRepository.save(creatorAccountA);

        creatorAccountB = new Account();
        creatorAccountB.setEmailOrPhone("creator_beta@example.com");
        creatorAccountB.setPasswordHash("$2a$12$dummyhashcreatorBeta12345678901");
        creatorAccountB.setStatus("Active");
        creatorAccountB = accountRepository.save(creatorAccountB);

        vocab1 = vocabularyRepository.save(new Vocabulary("学", "xué", "xue", "Học", "Học tập"));
        vocab2 = vocabularyRepository.save(new Vocabulary("生", "shēng", "sheng", "Sinh", "Học sinh"));
        vocab3 = vocabularyRepository.save(new Vocabulary("校", "xiào", "xiao", "Hiệu", "Trường học"));

        for (String email : List.of("admin_integ@example.com", "learner_integ@example.com", "moderator_integ@example.com")) {
            accountRepository.findByEmailOrPhone(email)
                    .orElseGet(() -> {
                        Account acc = new Account();
                        acc.setEmailOrPhone(email);
                        acc.setPasswordHash("$2a$12$dummyhashother12345678901234");
                        acc.setStatus("Active");
                        return accountRepository.save(acc);
                    });
        }

        creatorTokenA = jwtUtil.generateToken("creator_alpha@example.com", List.of("Creator"));
        creatorTokenB = jwtUtil.generateToken("creator_beta@example.com", List.of("Creator"));
        learnerToken = jwtUtil.generateToken("learner_integ@example.com", List.of("Learner"));
        moderatorToken = jwtUtil.generateToken("moderator_integ@example.com", List.of("Moderator"));
        adminToken = jwtUtil.generateToken("admin_integ@example.com", List.of("Admin"));
    }

    @Nested
    @DisplayName("1. Security Boundary Tests (Role-Based Access Control)")
    class SecurityBoundaryTests {

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN calling Creator API THEN returns 401 UNAUTHORIZED")
        void testUnauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/creator/lessons"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN Learner token WHEN calling Creator API THEN returns 403 FORBIDDEN")
        void testLearnerToken_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/creator/lessons")
                            .header("Authorization", "Bearer " + learnerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN Moderator token WHEN calling Creator API THEN returns 403 FORBIDDEN")
        void testModeratorToken_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/creator/lessons")
                            .header("Authorization", "Bearer " + moderatorToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN Creator token WHEN calling Creator API THEN returns 200 OK")
        void testCreatorToken_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/creator/lessons")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @DisplayName("GIVEN Admin token WHEN calling Creator API THEN returns 200 OK")
        void testAdminToken_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/creator/lessons")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    @DisplayName("2. Checkpoint 5B: Creator Ownership Isolation Tests")
    class OwnershipIsolationTests {

        private Long lessonBId;

        @BeforeEach
        void createLessonB() {
            Lesson lessonB = new Lesson("Bài riêng tư của B", creatorAccountB);
            lessonB.setStatus("Draft");
            lessonB = lessonRepository.save(lessonB);
            lessonBId = lessonB.getLessonId();
        }

        @Test
        @DisplayName("GIVEN lesson of Creator B WHEN Creator A attempts GET /api/v1/creator/lessons/{id} THEN returns 403 FORBIDDEN (Checkpoint 5B)")
        void testCreatorA_cannotGetCreatorBLesson() throws Exception {
            mockMvc.perform(get("/api/v1/creator/lessons/" + lessonBId)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN lesson of Creator B WHEN Creator A attempts PUT /api/v1/creator/lessons/{id} THEN returns 403 FORBIDDEN (Checkpoint 5B)")
        void testCreatorA_cannotUpdateCreatorBLesson() throws Exception {
            UpdateLessonRequest request = new UpdateLessonRequest("Tiêu đề bị sửa lén");

            mockMvc.perform(put("/api/v1/creator/lessons/" + lessonBId)
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN lesson of Creator B WHEN Creator A attempts DELETE /api/v1/creator/lessons/{id} THEN returns 403 FORBIDDEN (Checkpoint 5B)")
        void testCreatorA_cannotDeleteCreatorBLesson() throws Exception {
            mockMvc.perform(delete("/api/v1/creator/lessons/" + lessonBId)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN lesson of Creator B WHEN Creator A attempts POST .../reorder THEN returns 403 FORBIDDEN (Checkpoint 5B)")
        void testCreatorA_cannotReorderCreatorBLesson() throws Exception {
            ReorderVocabRequest request = ReorderVocabRequest.of(List.of(vocab1.getVocabId()));

            mockMvc.perform(post("/api/v1/creator/lessons/" + lessonBId + "/reorder")
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN lesson of Creator B WHEN Creator A attempts POST .../submit THEN returns 403 FORBIDDEN (Checkpoint 5B)")
        void testCreatorA_cannotSubmitCreatorBLesson() throws Exception {
            mockMvc.perform(post("/api/v1/creator/lessons/" + lessonBId + "/submit")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("3. Checkpoint 5B: Complete Creator Studio Workflow")
    class CompleteWorkflowTests {

        @Test
        @DisplayName("GIVEN Creator WHEN creating draft, adding vocabularies, reordering, submitting THEN completes full workflow")
        void testFullCreatorWorkflow() throws Exception {
            // Step 1: Create Draft lesson
            CreateLessonRequest createReq = new CreateLessonRequest("Bài Học Studio Toàn Diện");
            String createResStr = mockMvc.perform(post("/api/v1/creator/lessons")
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.status").value("Draft"))
                    .andExpect(jsonPath("$.data.vocabularyCount").value(0))
                    .andReturn().getResponse().getContentAsString();

            Long lessonId = ((Number) com.jayway.jsonpath.JsonPath.read(createResStr, "$.data.lessonId")).longValue();

            // Step 2: Add vocabulary 1 and vocabulary 2
            mockMvc.perform(post("/api/v1/creator/lessons/" + lessonId + "/vocabularies/" + vocab1.getVocabId())
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.vocabularyCount").value(1))
                    .andExpect(jsonPath("$.data.vocabularies[0].orderIndex").value(1));

            mockMvc.perform(post("/api/v1/creator/lessons/" + lessonId + "/vocabularies/" + vocab2.getVocabId())
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.vocabularyCount").value(2))
                    .andExpect(jsonPath("$.data.vocabularies[1].orderIndex").value(2));

            // Step 3: Reorder vocabulary (swap order: vocab 2 first, vocab 1 second)
            ReorderVocabRequest reorderReq = ReorderVocabRequest.of(List.of(vocab2.getVocabId(), vocab1.getVocabId()));
            mockMvc.perform(put("/api/v1/creator/lessons/" + lessonId + "/reorder")
                            .header("Authorization", "Bearer " + creatorTokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reorderReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.vocabularies[0].vocabId").value(vocab2.getVocabId()))
                    .andExpect(jsonPath("$.data.vocabularies[0].orderIndex").value(1))
                    .andExpect(jsonPath("$.data.vocabularies[1].vocabId").value(vocab1.getVocabId()))
                    .andExpect(jsonPath("$.data.vocabularies[1].orderIndex").value(2));

            // Step 4: Submit for moderation (Draft -> Pending)
            mockMvc.perform(post("/api/v1/creator/lessons/" + lessonId + "/submit")
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("Pending"));

            // Step 5: Verify Public API isolation (Module 5A Checkpoint):
            // Anonymous GET /api/v1/lessons must NOT include this Pending lesson
            mockMvc.perform(get("/api/v1/lessons"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[?(@.lessonId == " + lessonId + ")]").doesNotExist());

            // Anonymous GET /api/v1/lessons/{id} must return 404 NOT_FOUND
            mockMvc.perform(get("/api/v1/lessons/" + lessonId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("GIVEN Draft lesson WHEN Creator deletes it THEN returns 204 No Content")
        void testDeleteDraftLesson_success() throws Exception {
            Lesson lesson = new Lesson("Bài Xóa Test", creatorAccountA);
            lesson.setStatus("Draft");
            lesson = lessonRepository.save(lesson);
            Long id = lesson.getLessonId();

            mockMvc.perform(delete("/api/v1/creator/lessons/" + id)
                            .header("Authorization", "Bearer " + creatorTokenA))
                    .andExpect(status().isNoContent());

            assertThat(lessonRepository.findById(id)).isEmpty();
        }
    }
}
