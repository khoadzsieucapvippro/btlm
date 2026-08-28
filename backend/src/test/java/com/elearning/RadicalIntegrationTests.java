package com.elearning;

import com.elearning.dto.request.CreateRadicalRequest;
import com.elearning.dto.request.UpdateRadicalRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 4A.2: Radical Catalog & Admin CRUD Integration Tests (Checkpoint 4A)")
class RadicalIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.elearning.repository.AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String learnerToken;

    @BeforeEach
    void setUp() {
        for (String email : List.of("admin@elearning.com", "learner@elearning.com")) {
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
    }

    @Nested
    @DisplayName("1. Public Radical Catalog Endpoints (No JWT required)")
    class PublicCatalogIntegrationTests {

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN GET /api/v1/radicals THEN 200 OK with all 214 seeded Kangxi radicals")
        void testGetAllRadicalsPublicSuccess() throws Exception {
            mockMvc.perform(get("/api/v1/radicals?page=0&size=20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.totalElements").value(214))
                    .andExpect(jsonPath("$.data.totalPages").value(11))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].radicalId").value(1))
                    .andExpect(jsonPath("$.data.items[0].character").value("一"))
                    .andExpect(jsonPath("$.data.items[0].pinyin").value("yī"))
                    .andExpect(jsonPath("$.data.items[0].meaningHanViet").value("Nhất"))
                    .andExpect(jsonPath("$.data.items[0].meaningVi").value(""));
        }

        @Test
        @DisplayName("GIVEN existing radical ID 1 WHEN GET /api/v1/radicals/1 THEN 200 OK with detail metadata")
        void testGetRadicalByIdPublicSuccess() throws Exception {
            mockMvc.perform(get("/api/v1/radicals/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.radicalId").value(1))
                    .andExpect(jsonPath("$.data.character").value("一"))
                    .andExpect(jsonPath("$.data.pinyin").value("yī"))
                    .andExpect(jsonPath("$.data.meaningHanViet").value("Nhất"))
                    .andExpect(jsonPath("$.data.meaningVi").value(""))
                    .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
        }

        @Test
        @DisplayName("GIVEN nonexistent radical ID 999 WHEN GET /api/v1/radicals/999 THEN 404 NOT_FOUND")
        void testGetRadicalByIdNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/radicals/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy bộ thủ với ID: 999"));
        }
    }

    @Nested
    @DisplayName("2. Security Boundaries on Admin Endpoints (/api/v1/admin/radicals/**)")
    class SecurityBoundaryIntegrationTests {

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN calling Admin endpoints THEN 401 Unauthorized")
        void testUnauthenticatedAdminEndpointsReturn401() throws Exception {
            CreateRadicalRequest createReq = new CreateRadicalRequest("test", "test", "test", "test", null, null);

            mockMvc.perform(post("/api/v1/admin/radicals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

            mockMvc.perform(put("/api/v1/admin/radicals/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

            mockMvc.perform(delete("/api/v1/admin/radicals/1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN authenticated Learner WHEN calling Admin endpoints THEN 403 Forbidden")
        void testLearnerCallingAdminEndpointsReturns403() throws Exception {
            CreateRadicalRequest createReq = new CreateRadicalRequest("test", "test", "test", "test", null, null);

            mockMvc.perform(post("/api/v1/admin/radicals")
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            mockMvc.perform(put("/api/v1/admin/radicals/1")
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            mockMvc.perform(delete("/api/v1/admin/radicals/1")
                            .header("Authorization", "Bearer " + learnerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("3. Admin CRUD Vertical Slice Execution (/api/v1/admin/radicals/**)")
    class AdminCrudExecutionIntegrationTests {

        @Test
        @DisplayName("GIVEN authenticated Admin WHEN creating, updating, and deleting radical THEN full lifecycle succeeds")
        void testAdminCrudLifecycle() throws Exception {
            // 1. CREATE new radical
            CreateRadicalRequest createReq = new CreateRadicalRequest(
                    "測試", "cèshì", "Trắc Thí", "Thử nghiệm bộ thủ mới",
                    "https://cdn.example.com/audio/test.mp3",
                    "https://cdn.example.com/video/test.mp4"
            );

            MvcResult createResult = mockMvc.perform(post("/api/v1/admin/radicals")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Tạo bộ thủ thành công"))
                    .andExpect(jsonPath("$.data.character").value("測試"))
                    .andExpect(jsonPath("$.data.pinyin").value("cèshì"))
                    .andExpect(jsonPath("$.data.meaningHanViet").value("Trắc Thí"))
                    .andExpect(jsonPath("$.data.meaningVi").value("Thử nghiệm bộ thủ mới"))
                    .andReturn();

            JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
            int newRadicalId = root.get("data").get("radicalId").asInt();

            // 2. VERIFY public lookup immediately reflects newly created radical
            mockMvc.perform(get("/api/v1/radicals/" + newRadicalId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.character").value("測試"));

            // 3. UPDATE radical
            UpdateRadicalRequest updateReq = new UpdateRadicalRequest(
                    "測試", "cèshì", "Trắc Thí", "Thử nghiệm bộ thủ đã cập nhật",
                    "https://cdn.example.com/audio/updated.mp3",
                    "https://cdn.example.com/video/updated.mp4"
            );

            mockMvc.perform(put("/api/v1/admin/radicals/" + newRadicalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Cập nhật bộ thủ thành công"))
                    .andExpect(jsonPath("$.data.meaningVi").value("Thử nghiệm bộ thủ đã cập nhật"))
                    .andExpect(jsonPath("$.data.audioUrl").value("https://cdn.example.com/audio/updated.mp3"));

            // 4. DELETE radical
            mockMvc.perform(delete("/api/v1/admin/radicals/" + newRadicalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            // 5. VERIFY public lookup returns 404 after deletion
            mockMvc.perform(get("/api/v1/radicals/" + newRadicalId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("GIVEN duplicate character WHEN Admin creates radical THEN 409 Conflict")
        void testCreateDuplicateCharacterConflict() throws Exception {
            CreateRadicalRequest duplicateReq = new CreateRadicalRequest(
                    "一", "yī", "Nhất", "Một", null, null
            );

            mockMvc.perform(post("/api/v1/admin/radicals")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateReq)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.message").value("Bộ thủ với ký tự '一' đã tồn tại"));
        }

        @Test
        @DisplayName("GIVEN invalid request missing fields WHEN Admin creates radical THEN 400 Bad Request")
        void testCreateValidationError() throws Exception {
            CreateRadicalRequest invalidReq = new CreateRadicalRequest(
                    "", "", "", "", null, null
            );

            mockMvc.perform(post("/api/v1/admin/radicals")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("GIVEN nonexistent ID WHEN Admin updates radical THEN 404 Not Found")
        void testUpdateNotFound() throws Exception {
            UpdateRadicalRequest updateReq = new UpdateRadicalRequest(
                    "一", "yī", "Nhất", "Một", null, null
            );

            mockMvc.perform(put("/api/v1/admin/radicals/999")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("GIVEN character conflict WHEN Admin updates radical THEN 409 Conflict")
        void testUpdateCharacterConflict() throws Exception {
            // Trying to change radical 1's character to '丨' (which is radical 2)
            UpdateRadicalRequest conflictReq = new UpdateRadicalRequest(
                    "丨", "gǔn", "Cổn", "Nét sổ", null, null
            );

            mockMvc.perform(put("/api/v1/admin/radicals/1")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(conflictReq)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.message").value("Bộ thủ với ký tự '丨' đã tồn tại"));
        }

        @Test
        @DisplayName("GIVEN nonexistent ID WHEN Admin deletes radical THEN 404 Not Found")
        void testDeleteNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/radicals/999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }
}
