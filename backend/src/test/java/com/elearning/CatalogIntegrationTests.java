package com.elearning;

import com.elearning.dto.request.CreateRadicalRequest;
import com.elearning.dto.request.CreateVocabularyRequest;
import com.elearning.dto.request.UpdateRadicalRequest;
import com.elearning.dto.request.UpdateVocabularyRequest;
import com.elearning.entity.Radical;
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

/**
 * Task 4C.1: Comprehensive Catalog Domain Integration Verification Tests.
 *
 * Verifies the complete end-to-end integration path:
 * MockMvc -> SecurityFilterChain -> Controller -> Service -> Repository -> JPA/Hibernate -> MySQL Database.
 *
 * Covers:
 * - Module 4A: Radical Catalog (Public retrieval of 214 Kangxi radicals, Detail, Admin CRUD & RBAC)
 * - Module 4B: Vocabulary Catalog & Search (Public listing, Hanzi/Pinyin/Toneless search, Radical N:N filter, Pagination, Admin CRUD & RBAC)
 * - Checkpoint 4A: Public lookup returns 214 radicals invariant; Learner calling Admin API returns 403 Forbidden.
 * - Checkpoint 4B: Toneless keyword search ("ni" -> "nǐ / 你") works; PageResponse envelopes correctly.
 * - Checkpoint Phase 4: Full domain catalog verification with test isolation and zero false-positive mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 4C.1: Catalog Domain MockMvc Integration Verification (Checkpoint Phase 4)")
class CatalogIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RadicalRepository radicalRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private com.elearning.repository.AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String learnerToken;
    private String creatorToken;
    private String moderatorToken;

    private Vocabulary seededVocabNi;
    private Vocabulary seededVocabXiu;

    @BeforeEach
    void setUp() {
        for (String email : List.of("admin@elearning.com", "learner@elearning.com", "creator@elearning.com", "moderator@elearning.com")) {
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
        creatorToken = jwtUtil.generateToken("creator@elearning.com", List.of("Creator"));
        moderatorToken = jwtUtil.generateToken("moderator@elearning.com", List.of("Moderator"));

        // Seed vocabulary "你 (nǐ / ni)" linked with radical 9 (人 / 亻) for Checkpoint 4B
        seededVocabNi = new Vocabulary("你", "nǐ", "ni", "Nhĩ", "Bạn, anh, chị (ngôi thứ 2)");
        seededVocabNi.setExampleSentence("你好！");
        seededVocabNi.setExampleTranslation("Xin chào!");
        radicalRepository.findById(9).ifPresent(seededVocabNi::addRadical);
        seededVocabNi = vocabularyRepository.save(seededVocabNi);

        // Seed vocabulary "休 (xiū / xiu)" linked with radical 9 and radical 75 (木)
        seededVocabXiu = new Vocabulary("休", "xiū", "xiu", "Hưu", "Nghỉ ngơi");
        seededVocabXiu.setExampleSentence("他在休息。");
        seededVocabXiu.setExampleTranslation("Anh ấy đang nghỉ ngơi.");
        radicalRepository.findById(9).ifPresent(seededVocabXiu::addRadical);
        radicalRepository.findById(75).ifPresent(seededVocabXiu::addRadical);
        seededVocabXiu = vocabularyRepository.save(seededVocabXiu);
    }

    @Nested
    @DisplayName("1. Module 4A — Radical Catalog & Admin Verification (Checkpoint 4A)")
    class RadicalCatalogVerificationTests {

        @Test
        @DisplayName("GIVEN GET /api/v1/radicals?page=0&size=20 WHEN unauthenticated THEN 200 OK and exactly 214 total Kangxi radicals (Checkpoint 4A)")
        void testRadicalPublicListReturns214Records() throws Exception {
            mockMvc.perform(get("/api/v1/radicals?page=0&size=20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.totalElements").value(214))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].radicalId").value(1))
                    .andExpect(jsonPath("$.data.items[0].character").value("一"));
        }

        @Test
        @DisplayName("GIVEN GET /api/v1/radicals without pagination WHEN unauthenticated THEN 200 OK and all 214 radicals")
        void testRadicalPublicFullList() throws Exception {
            mockMvc.perform(get("/api/v1/radicals"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.totalElements").value(214));
        }

        @Test
        @DisplayName("GIVEN GET /api/v1/radicals/9 (Human radical) WHEN requested THEN 200 OK with correct Han-Viet & Pinyin")
        void testRadicalPublicDetail() throws Exception {
            mockMvc.perform(get("/api/v1/radicals/9"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.radicalId").value(9))
                    .andExpect(jsonPath("$.data.pinyin").value("rén"));
        }

        @Test
        @DisplayName("GIVEN GET /api/v1/radicals/999 (nonexistent ID) WHEN requested THEN 404 NOT_FOUND")
        void testRadicalPublicDetailNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/radicals/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN POST /api/v1/admin/radicals THEN 401 Unauthorized")
        void testAdminRadicalUnauthenticatedReturns401() throws Exception {
            CreateRadicalRequest request = new CreateRadicalRequest("测", "cè", "Trắc", "Thử nghiệm");

            mockMvc.perform(post("/api/v1/admin/radicals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN Learner role WHEN POST /api/v1/admin/radicals THEN 403 Forbidden (Checkpoint 4A)")
        void testAdminRadicalLearnerReturns403() throws Exception {
            CreateRadicalRequest request = new CreateRadicalRequest("测", "cè", "Trắc", "Thử nghiệm");

            mockMvc.perform(post("/api/v1/admin/radicals")
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN Creator and Moderator roles WHEN accessing Admin radical API THEN 403 Forbidden")
        void testAdminRadicalCreatorAndModeratorForbidden() throws Exception {
            CreateRadicalRequest request = new CreateRadicalRequest("测", "cè", "Trắc", "Thử nghiệm");

            mockMvc.perform(post("/api/v1/admin/radicals")
                            .header("Authorization", "Bearer " + creatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            mockMvc.perform(put("/api/v1/admin/radicals/1")
                            .header("Authorization", "Bearer " + moderatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateRadicalRequest("一", "yī", "Nhất", "Một"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN Admin role WHEN performing CRUD on Radical THEN operations succeed (201, 200, 204)")
        void testAdminRadicalCrudSuccess() throws Exception {
            // 1. Create radical
            CreateRadicalRequest createReq = new CreateRadicalRequest("新", "xīn", "Tân", "Mới");
            MvcResult createResult = mockMvc.perform(post("/api/v1/admin/radicals")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.radicalId").isNumber())
                    .andReturn();

            JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
            int createdId = root.path("data").path("radicalId").asInt();

            // 2. Update radical
            UpdateRadicalRequest updateReq = new UpdateRadicalRequest("新", "xīn", "Tân", "Mới mẻ, tươi mới");
            mockMvc.perform(put("/api/v1/admin/radicals/" + createdId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.meaningVi").value("Mới mẻ, tươi mới"));

            // 3. Delete radical
            mockMvc.perform(delete("/api/v1/admin/radicals/" + createdId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            // 4. Verify deleted via GET
            mockMvc.perform(get("/api/v1/radicals/" + createdId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("2. Module 4B — Vocabulary Search & Tone-less Retrieval (Checkpoint 4B)")
    class VocabularyCatalogSearchVerificationTests {

        @Test
        @DisplayName("GIVEN search keyword 'ni' WHEN GET /api/v1/vocabulary?search=ni THEN finds '你 (nǐ / ni)' (Mandatory Checkpoint 4B)")
        void testTonelessSearchKeywordNiReturnsExpected() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?search=ni"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].hanzi").value("你"))
                    .andExpect(jsonPath("$.data.items[0].pinyin").value("nǐ"))
                    .andExpect(jsonPath("$.data.items[0].pinyinRaw").value("ni"))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").isNumber())
                    .andExpect(jsonPath("$.data.totalElements").isNumber())
                    .andExpect(jsonPath("$.data.totalPages").isNumber());
        }

        @Test
        @DisplayName("GIVEN search keyword with tone 'nǐ' WHEN GET /api/v1/vocabulary?search=nǐ THEN finds '你'")
        void testAccentedPinyinSearch() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?search=nǐ"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items[0].hanzi").value("你"));
        }

        @Test
        @DisplayName("GIVEN search keyword Hanzi '你' WHEN GET /api/v1/vocabulary?search=你 THEN finds '你'")
        void testHanziSearch() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?search=你"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items[0].hanzi").value("你"));
        }

        @Test
        @DisplayName("GIVEN search keyword 'xiu' WHEN GET /api/v1/vocabulary?search=xiu THEN finds '休 (xiū / xiu)'")
        void testTonelessSearchKeywordXiu() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?search=xiu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items[0].hanzi").value("休"));
        }

        @Test
        @DisplayName("GIVEN search keyword with empty string or blank WHEN GET /api/v1/vocabulary?search=  THEN returns all vocabularies without error")
        void testBlankSearchReturnsAll() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?search=   "))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items").isNotEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("GIVEN search keyword with no match WHEN GET /api/v1/vocabulary?search=nonexistentxyz THEN returns empty page")
        void testSearchNoMatchReturnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?search=nonexistentxyz123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.totalPages").value(0));
        }

        @Test
        @DisplayName("GIVEN existing vocabulary ID WHEN GET /api/v1/vocabulary/{id} THEN returns detail with linked radicals without duplicates")
        void testVocabularyDetailWithLinkedRadicals() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary/" + seededVocabXiu.getVocabId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.vocabId").value(seededVocabXiu.getVocabId()))
                    .andExpect(jsonPath("$.data.hanzi").value("休"))
                    .andExpect(jsonPath("$.data.radicals").isArray())
                    .andExpect(jsonPath("$.data.radicals.length()").value(2))
                    .andExpect(jsonPath("$.data.radicals[0].radicalId").value(9))
                    .andExpect(jsonPath("$.data.radicals[1].radicalId").value(75));
        }

        @Test
        @DisplayName("GIVEN nonexistent vocabulary ID WHEN GET /api/v1/vocabulary/999999 THEN 404 NOT_FOUND")
        void testVocabularyDetailNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary/999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("3. Module 4B — Vocabulary Pagination Verification")
    class VocabularyCatalogPaginationVerificationTests {

        @Test
        @DisplayName("GIVEN explicit page=0 and size=1 WHEN GET /api/v1/vocabulary THEN returns page 0 with exactly 1 item")
        void testPaginationPageZeroSizeOne() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?page=0&size=1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(1))
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("GIVEN page=1 and size=1 WHEN GET /api/v1/vocabulary THEN returns page 1 with second item")
        void testPaginationPageOneSizeOne() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?page=1&size=1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.size").value(1))
                    .andExpect(jsonPath("$.data.items.length()").value(1));
        }

        @Test
        @DisplayName("GIVEN page beyond totalPages WHEN GET /api/v1/vocabulary?page=999&size=20 THEN returns empty items with totalElements preserved")
        void testPaginationBeyondLastPage() throws Exception {
            mockMvc.perform(get("/api/v1/vocabulary?page=999&size=20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.page").value(999))
                    .andExpect(jsonPath("$.data.items").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }
    }

    @Nested
    @DisplayName("4. Module 4B — Admin Vocabulary Authorization & CRUD (Security Matrix)")
    class VocabularyAdminSecurityAndCrudVerificationTests {

        @Test
        @DisplayName("GIVEN unauthenticated request WHEN calling Admin vocabulary endpoints THEN 401 Unauthorized")
        void testAdminVocabularyUnauthenticatedReturns401() throws Exception {
            CreateVocabularyRequest createReq = new CreateVocabularyRequest("好", "hǎo", "hao", "Hảo", "Tốt");

            mockMvc.perform(post("/api/v1/admin/vocabulary")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

            mockMvc.perform(put("/api/v1/admin/vocabulary/" + seededVocabNi.getVocabId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateVocabularyRequest("你", "nǐ", "ni", "Nhĩ", "Bạn"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + seededVocabNi.getVocabId()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN malformed/invalid JWT token WHEN calling Admin vocabulary endpoint THEN 401 Unauthorized")
        void testAdminVocabularyInvalidTokenReturns401() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + seededVocabNi.getVocabId())
                            .header("Authorization", "Bearer invalid.jwt.token.here"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("GIVEN non-admin roles (Learner, Creator, Moderator) WHEN calling Admin vocabulary endpoints THEN 403 Forbidden")
        void testAdminVocabularyNonAdminRolesReturn403() throws Exception {
            CreateVocabularyRequest createReq = new CreateVocabularyRequest("好", "hǎo", "hao", "Hảo", "Tốt");

            // Learner -> 403
            mockMvc.perform(post("/api/v1/admin/vocabulary")
                            .header("Authorization", "Bearer " + learnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            // Creator -> 403
            mockMvc.perform(put("/api/v1/admin/vocabulary/" + seededVocabNi.getVocabId())
                            .header("Authorization", "Bearer " + creatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateVocabularyRequest("你", "nǐ", "ni", "Nhĩ", "Bạn"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            // Moderator -> 403
            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + seededVocabNi.getVocabId())
                            .header("Authorization", "Bearer " + moderatorToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN Admin role WHEN performing full CRUD on Vocabulary THEN operations succeed (201, 200, 204)")
        void testAdminVocabularyCrudSuccess() throws Exception {
            // 1. Create vocabulary "明 (míng / ming)" linked with radicals 72 (日) and 74 (月)
            CreateVocabularyRequest createReq = new CreateVocabularyRequest("明", "míng", "ming", "Minh", "Sáng sủa, thông minh");
            createReq.setRadicalIds(Set.of(72, 74));

            MvcResult createResult = mockMvc.perform(post("/api/v1/admin/vocabulary")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.hanzi").value("明"))
                    .andExpect(jsonPath("$.data.pinyinRaw").value("ming"))
                    .andExpect(jsonPath("$.data.radicals.length()").value(2))
                    .andReturn();

            JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
            long createdId = root.path("data").path("vocabId").asLong();

            // 2. Update vocabulary "明"
            UpdateVocabularyRequest updateReq = new UpdateVocabularyRequest("明", "míng", "ming", "Minh", "Rõ ràng, sáng sủa");
            updateReq.setRadicalIds(Set.of(72)); // change to single radical 72

            mockMvc.perform(put("/api/v1/admin/vocabulary/" + createdId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.meaningVi").value("Rõ ràng, sáng sủa"))
                    .andExpect(jsonPath("$.data.radicals.length()").value(1));

            // 3. Delete vocabulary "明"
            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + createdId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            // 4. Verify deleted via GET returns 404
            mockMvc.perform(get("/api/v1/vocabulary/" + createdId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GIVEN duplicate (hanzi, pinyinRaw) WHEN Admin creates vocabulary THEN 409 Conflict")
        void testAdminVocabularyDuplicateConflict() throws Exception {
            CreateVocabularyRequest duplicateReq = new CreateVocabularyRequest("你", "nǐ", "ni", "Nhĩ", "Bạn");

            mockMvc.perform(post("/api/v1/admin/vocabulary")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateReq)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        }

        @Test
        @DisplayName("GIVEN blank fields WHEN Admin creates vocabulary THEN 400 Bad Request")
        void testAdminVocabularyValidationFailure() throws Exception {
            CreateVocabularyRequest invalidReq = new CreateVocabularyRequest("", "", "", "", "");

            mockMvc.perform(post("/api/v1/admin/vocabulary")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
}
