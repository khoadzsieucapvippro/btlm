package com.elearning;

import com.elearning.dto.request.PersonalNoteRequest;
import com.elearning.entity.Account;
import com.elearning.entity.PersonalNote;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.PersonalNoteRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 8A.2: PersonalNote REST API Integration Tests (MockMvc + MySQL 8.4)")
class PersonalNoteIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private PersonalNoteRepository personalNoteRepository;

    private UserProfile userA;
    private UserProfile userB;
    private String tokenA;
    private String tokenB;
    private Vocabulary vocab1;

    @BeforeEach
    void setUp() {
        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);

        // Create User A
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        Account accA = new Account();
        accA.setEmailOrPhone("user_a_" + suffixA + "@test.com");
        accA.setPasswordHash("hash");
        accA.setStatus("Active");
        if (learnerRole != null) {
            accA.getRoles().add(learnerRole);
        }
        accA = accountRepository.save(accA);

        userA = new UserProfile();
        userA.setAccount(accA);
        userA.setFullName("User A " + suffixA);
        userA = userProfileRepository.save(userA);
        tokenA = jwtUtil.generateToken(accA.getEmailOrPhone(), List.of("ROLE_LEARNER"));

        // Create User B
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        Account accB = new Account();
        accB.setEmailOrPhone("user_b_" + suffixB + "@test.com");
        accB.setPasswordHash("hash");
        accB.setStatus("Active");
        if (learnerRole != null) {
            accB.getRoles().add(learnerRole);
        }
        accB = accountRepository.save(accB);

        userB = new UserProfile();
        userB.setAccount(accB);
        userB.setFullName("User B " + suffixB);
        userB = userProfileRepository.save(userB);
        tokenB = jwtUtil.generateToken(accB.getEmailOrPhone(), List.of("ROLE_LEARNER"));

        // Create test Vocabulary
        vocab1 = vocabularyRepository.findByHanziAndPinyinRaw("好", "hao")
                .orElseGet(() -> vocabularyRepository.save(new Vocabulary("好", "hǎo", "hao", "Hảo", "Tốt, đẹp")));
    }

    @Nested
    @DisplayName("1. Security Boundary Tests (Anonymous vs Authenticated)")
    class SecurityBoundaryTests {

        @Test
        @DisplayName("Anonymous GET /api/v1/vocabularies/{vocabId}/notes returns 401 Unauthorized")
        void testGetNotes_anonymous_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Anonymous POST /api/v1/vocabularies/{vocabId}/notes returns 401 Unauthorized")
        void testCreateNote_anonymous_returns401() throws Exception {
            PersonalNoteRequest request = new PersonalNoteRequest("Note");
            mockMvc.perform(post("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Anonymous PUT /api/v1/notes/{noteId} returns 401 Unauthorized")
        void testUpdateNote_anonymous_returns401() throws Exception {
            PersonalNoteRequest request = new PersonalNoteRequest("Updated");
            mockMvc.perform(put("/api/v1/notes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }

        @Test
        @DisplayName("Anonymous DELETE /api/v1/notes/{noteId} returns 401 Unauthorized")
        void testDeleteNote_anonymous_returns401() throws Exception {
            mockMvc.perform(delete("/api/v1/notes/1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
        }
    }

    @Nested
    @DisplayName("2. Checkpoint 8A Vertical Slice & Business Invariant Tests")
    class Checkpoint8AVerticalSliceTests {

        @Test
        @DisplayName("Checkpoint 8A.1: Create note with <= 500 chars returns 201 Created and persists on MySQL")
        void testCreateNote_500Chars_returns201() throws Exception {
            String content500 = "Ghi chú: " + "A".repeat(491);
            assertThat(content500).hasSize(500);

            PersonalNoteRequest request = new PersonalNoteRequest(content500);

            mockMvc.perform(post("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.noteId").isNumber())
                    .andExpect(jsonPath("$.data.vocabId", is(vocab1.getVocabId().intValue())))
                    .andExpect(jsonPath("$.data.content", is(content500)));

            // Independent Database verification
            List<PersonalNote> notesOnDb = personalNoteRepository.findByUserAndVocabularyOrderByCreatedAtDesc(userA, vocab1);
            assertThat(notesOnDb).hasSize(1);
            assertThat(notesOnDb.get(0).getContent()).isEqualTo(content500);
        }

        @Test
        @DisplayName("Checkpoint 8A.2: Create note with > 500 chars returns 400 VALIDATION_ERROR without DB mutation")
        void testCreateNote_501Chars_returns400() throws Exception {
            long initialCount = personalNoteRepository.count();
            String content501 = "X".repeat(501);
            PersonalNoteRequest request = new PersonalNoteRequest(content501);

            mockMvc.perform(post("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            assertThat(personalNoteRepository.count()).isEqualTo(initialCount);
        }

        @Test
        @DisplayName("Checkpoint 8A.3: User A attempting to modify User B's note returns 403 Forbidden and preserves DB content")
        void testUpdateNote_otherUser_returns403() throws Exception {
            // User B creates note
            PersonalNote noteB = personalNoteRepository.save(new PersonalNote(userB, vocab1, "Original User B Note"));

            // User A attempts to update User B's note
            PersonalNoteRequest request = new PersonalNoteRequest("Hacked by User A");

            mockMvc.perform(put("/api/v1/notes/" + noteB.getNoteId())
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                    .andExpect(jsonPath("$.message", is("Bạn không có quyền chỉnh sửa ghi chú này")));

            // Independent DB check
            PersonalNote noteOnDb = personalNoteRepository.findById(noteB.getNoteId()).orElseThrow();
            assertThat(noteOnDb.getContent()).isEqualTo("Original User B Note");
        }

        @Test
        @DisplayName("Checkpoint 8A.4: User A attempting to delete User B's note returns 403 Forbidden and keeps note in DB")
        void testDeleteNote_otherUser_returns403() throws Exception {
            // User B creates note
            PersonalNote noteB = personalNoteRepository.save(new PersonalNote(userB, vocab1, "User B Note to preserve"));

            // User A attempts delete
            mockMvc.perform(delete("/api/v1/notes/" + noteB.getNoteId())
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                    .andExpect(jsonPath("$.message", is("Bạn không có quyền xóa ghi chú này")));

            // Independent DB check
            assertThat(personalNoteRepository.findById(noteB.getNoteId())).isPresent();
        }

        @Test
        @DisplayName("Full Lifecycle Vertical Slice: Create (201) -> Get (200) -> Put (200) -> Delete (200) -> Get (empty)")
        void testFullLifecycleVerticalSlice() throws Exception {
            // 1. Create note
            PersonalNoteRequest createReq = new PersonalNoteRequest("Ghi chú ban đầu");
            String createResJson = mockMvc.perform(post("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andReturn().getResponse().getContentAsString();

            Long noteId = objectMapper.readTree(createResJson).path("data").path("noteId").asLong();

            // 2. Get notes
            mockMvc.perform(get("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(1)))
                    .andExpect(jsonPath("$.data.items[0].content", is("Ghi chú ban đầu")));

            // 3. Put note
            PersonalNoteRequest updateReq = new PersonalNoteRequest("Ghi chú đã sửa đổi");
            mockMvc.perform(put("/api/v1/notes/" + noteId)
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", is("Ghi chú đã sửa đổi")));

            // 4. Delete note
            mockMvc.perform(delete("/api/v1/notes/" + noteId)
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Xóa ghi chú thành công")));

            // 5. Get notes -> empty
            mockMvc.perform(get("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(0)));
        }

        @Test
        @DisplayName("Unlimited Notes: Learner can create 6+ notes on same vocabulary without hitting any artificial 5-note cap")
        void testUnlimitedNotes_sixNotes_allSucceed() throws Exception {
            for (int i = 1; i <= 6; i++) {
                PersonalNoteRequest req = new PersonalNoteRequest("Note #" + i + " on vocabulary 好");
                mockMvc.perform(post("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes")
                                .header("Authorization", "Bearer " + tokenA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isCreated());
            }

            mockMvc.perform(get("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(6)));
        }

        @Test
        @DisplayName("Unicode & Special Characters: Chinese and Vietnamese characters round-trip successfully")
        void testUnicodeRoundTrip() throws Exception {
            String unicodeContent = "学习中文：你好，很高兴认识你！Chúc bạn một ngày tốt lành.";
            PersonalNoteRequest req = new PersonalNoteRequest(unicodeContent);

            mockMvc.perform(post("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.content", is(unicodeContent)));

            mockMvc.perform(get("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes")
                            .header("Authorization", "Bearer " + tokenA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].content", is(unicodeContent)));
        }

        @Test
        @DisplayName("Validation: Malformed JSON returns 400 VALIDATION_ERROR")
        void testMalformedJson_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/vocabularies/" + vocab1.getVocabId() + "/notes")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ malformed json }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("Error Handling: Nonexistent vocabulary returns 404 NOT_FOUND")
        void testNonexistentVocabulary_returns404() throws Exception {
            PersonalNoteRequest req = new PersonalNoteRequest("Note on missing vocab");
            mockMvc.perform(post("/api/v1/vocabularies/999999/notes")
                            .header("Authorization", "Bearer " + tokenA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        }
    }
}
