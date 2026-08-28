package com.elearning;

import com.elearning.dto.request.CreateVocabularyRequest;
import com.elearning.dto.request.UpdateVocabularyRequest;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 4B.2: Vocabulary Catalog & Admin CRUD Integration Tests (Checkpoint 4B)")
class VocabularyIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private RadicalRepository radicalRepository;

    @Autowired
    private com.elearning.repository.AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String learnerToken;
    private String moderatorToken;
    private String creatorToken;

    private Vocabulary seededVocab;

    @BeforeEach
    void setUp() {
        for (String email : List.of("admin@elearning.com", "learner@elearning.com", "moderator@elearning.com", "creator@elearning.com")) {
            accountRepository.findByEmailOrPhone(email)
                    .orElseGet(() -> {
                        com.elearning.entity.Account acc = new com.elearning.entity.Account();
                        acc.setEmailOrPhone(email);
                        acc.setPasswordHash("hash1234567890");
                        acc.setStatus("Active");
                        return accountRepository.save(acc);
                    });
        }

        adminToken = jwtUtil.generateToken("admin@elearning.com", List.of("Admin"));
        learnerToken = jwtUtil.generateToken("learner@elearning.com", List.of("Learner"));
        moderatorToken = jwtUtil.generateToken("moderator@elearning.com", List.of("Moderator"));
        creatorToken = jwtUtil.generateToken("creator@elearning.com", List.of("Creator"));

        // Seed a known test vocabulary with linked radicals (Kangxi radicals 9 and 75)
        seededVocab = new Vocabulary("休", "xiū", "xiu", "Hưu", "Nghỉ ngơi");
        seededVocab.setExampleSentence("他在休息。");
        seededVocab.setExampleTranslation("Anh ấy đang nghỉ ngơi.");

        radicalRepository.findById(9).ifPresent(seededVocab::addRadical);
        radicalRepository.findById(75).ifPresent(seededVocab::addRadical);

        seededVocab = vocabularyRepository.save(seededVocab);
    }

    @Nested
    @DisplayName("1. Public Vocabulary Catalog Endpoints (No JWT required)")
    class PublicCatalogIntegrationTests {

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN GET /api/v1/vocabulary THEN 200 OK with PageResponse")
        void testGetAllVocabulariesPublicSuccess() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?page=0&size=10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.totalElements").isNumber());
        }

        @Test
        @DisplayName("GIVEN search keyword 'xiu' WHEN GET /api/v1/vocabulary?search=xiu THEN finds '休' via toneless search (Checkpoint 4B)")
        void testSearchByKeywordTonelessPublic() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?search=xiu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items[0].hanzi").value("休"))
                    .andExpect(jsonPath("$.data.items[0].pinyin").value("xiū"))
                    .andExpect(jsonPath("$.data.items[0].pinyinRaw").value("xiu"));
        }

        @Test
        @DisplayName("GIVEN search keyword with tone 'xiū' WHEN GET /api/v1/vocabulary?search=xiū THEN finds '休'")
        void testSearchByKeywordTonePublic() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?search=xiū"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items[0].hanzi").value("休"));
        }

        @Test
        @DisplayName("GIVEN search keyword Hanzi '休' WHEN GET /api/v1/vocabulary?search=休 THEN finds '休'")
        void testSearchByKeywordHanziPublic() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?search=休"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items[0].hanzi").value("休"));
        }

        @Test
        @DisplayName("GIVEN search keyword with no match WHEN GET /api/v1/vocabulary?search=nonexistent THEN returns empty PageResponse")
        void testSearchNoMatchPublic() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?search=xyznonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("GIVEN existing vocabulary ID WHEN GET /api/v1/vocabulary/{id} THEN returns 200 OK with constituent radicals")
        void testGetVocabularyByIdPublicSuccess() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary/" + seededVocab.getVocabId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.vocabId").value(seededVocab.getVocabId()))
                    .andExpect(jsonPath("$.data.hanzi").value("休"))
                    .andExpect(jsonPath("$.data.radicals").isArray())
                    .andExpect(jsonPath("$.data.radicals.length()").value(2));
        }

        @Test
        @DisplayName("GIVEN nonexistent vocabulary ID WHEN GET /api/v1/vocabulary/{id} THEN returns 404 NOT_FOUND")
        void testGetVocabularyByIdNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary/999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("2. Security & RBAC Boundaries on Admin Endpoints (/api/v1/admin/vocabulary/**)")
    class SecurityBoundaryIntegrationTests {

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN POST /api/v1/admin/vocabulary THEN 401 Unauthorized")
        void testUnauthenticatedCreateReturns401() throws Exception {
            CreateVocabularyRequest request = new CreateVocabularyRequest("你", "nǐ", "ni", "Nhĩ", "Bạn");

            mockMvc.perform(post("/api/v1/admin/vocabulary")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN Learner role WHEN POST /api/v1/admin/vocabulary THEN 403 Forbidden")
        void testLearnerCreateReturns403() throws Exception {
            CreateVocabularyRequest request = new CreateVocabularyRequest("你", "nǐ", "ni", "Nhĩ", "Bạn");

            mockMvc.perform(post("/api/v1/admin/vocabulary")
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN Moderator role WHEN PUT /api/v1/admin/vocabulary/{id} THEN 403 Forbidden")
        void testModeratorUpdateReturns403() throws Exception {
            UpdateVocabularyRequest request = new UpdateVocabularyRequest("休", "xiū", "xiu", "Hưu", "Nghỉ ngơi");

            mockMvc.perform(put("/api/v1/admin/vocabulary/" + seededVocab.getVocabId())
                            .header("Authorization", "Bearer " + moderatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN Creator role WHEN DELETE /api/v1/admin/vocabulary/{id} THEN 403 Forbidden")
        void testCreatorDeleteReturns403() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + seededVocab.getVocabId())
                            .header("Authorization", "Bearer " + creatorToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("3. Admin Vocabulary CRUD Operations (Role Admin)")
    class AdminCrudIntegrationTests {

        @Test
        @DisplayName("GIVEN Admin role and valid request WHEN POST /api/v1/admin/vocabulary THEN 201 Created and persisted with radicals")
        void testAdminCreateVocabularySuccess() throws Exception {
            CreateVocabularyRequest request = new CreateVocabularyRequest("明", "míng", "ming", "Minh", "Sáng sủa");
            request.setRadicalIds(Set.of(72, 74)); // 日 (72), 月 (74)

            MvcResult result = mockMvc.perform(post("/api/v1/admin/vocabulary")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.hanzi").value("明"))
                    .andExpect(jsonPath("$.data.pinyin").value("míng"))
                    .andExpect(jsonPath("$.data.pinyinRaw").value("ming"))
                    .andExpect(jsonPath("$.data.radicals.length()").value(2))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            long createdId = root.path("data").path("vocabId").asLong();

            // Verify persistence via public GET
            mockMvc.perform(get("/api/v1/vocabulary/" + createdId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hanzi").value("明"));
        }

        @Test
        @DisplayName("GIVEN duplicate hanzi and pinyinRaw WHEN POST /api/v1/admin/vocabulary THEN returns 409 Conflict")
        void testAdminCreateVocabularyDuplicateConflict() throws Exception {
            CreateVocabularyRequest request = new CreateVocabularyRequest("休", "xiū", "xiu", "Hưu", "Nghỉ ngơi");

            mockMvc.perform(post("/api/v1/admin/vocabulary")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        }

        @Test
        @DisplayName("GIVEN invalid body with blank fields WHEN POST /api/v1/admin/vocabulary THEN returns 400 Bad Request")
        void testAdminCreateVocabularyValidationFailure() throws Exception {
            CreateVocabularyRequest request = new CreateVocabularyRequest("", "", "", "", "");

            mockMvc.perform(post("/api/v1/admin/vocabulary")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GIVEN valid UpdateVocabularyRequest WHEN PUT /api/v1/admin/vocabulary/{id} THEN 200 OK and updated in DB")
        void testAdminUpdateVocabularySuccess() throws Exception {
            UpdateVocabularyRequest request = new UpdateVocabularyRequest("休", "xiū", "xiu", "Hưu", "Nghỉ ngơi, tĩnh dưỡng");
            request.setExampleSentence("请休息一下。");
            request.setExampleTranslation("Xin hãy nghỉ một lát.");
            request.setRadicalIds(Set.of(9)); // Change to only radical 9

            mockMvc.perform(put("/api/v1/admin/vocabulary/" + seededVocab.getVocabId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.meaningVi").value("Nghỉ ngơi, tĩnh dưỡng"))
                    .andExpect(jsonPath("$.data.radicals.length()").value(1));

            // Verify via public GET
            mockMvc.perform(get("/api/v1/vocabulary/" + seededVocab.getVocabId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.meaningVi").value("Nghỉ ngơi, tĩnh dưỡng"))
                    .andExpect(jsonPath("$.data.radicals.length()").value(1));
        }

        @Test
        @DisplayName("GIVEN nonexistent ID WHEN PUT /api/v1/admin/vocabulary/{id} THEN returns 404 NOT_FOUND")
        void testAdminUpdateVocabularyNotFound() throws Exception {
            UpdateVocabularyRequest request = new UpdateVocabularyRequest("休", "xiū", "xiu", "Hưu", "Nghỉ ngơi");

            mockMvc.perform(put("/api/v1/admin/vocabulary/999999")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("GIVEN existing vocabulary WHEN DELETE /api/v1/admin/vocabulary/{id} THEN 204 No Content and removed from DB")
        void testAdminDeleteVocabularySuccess() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + seededVocab.getVocabId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            // Subsequent GET should return 404
            mockMvc.perform(get("/api/v1/vocabulary/" + seededVocab.getVocabId()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("GIVEN nonexistent vocabulary ID WHEN DELETE /api/v1/admin/vocabulary/{id} THEN returns 404 NOT_FOUND")
        void testAdminDeleteVocabularyNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/vocabulary/999999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }
}
