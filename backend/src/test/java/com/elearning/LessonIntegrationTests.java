package com.elearning;

import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.security.JwtUtil;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring Boot MockMvc Integration Tests for Public Lesson Exploration (Module 5A).
 * Verifies Checkpoint 5A:
 * - Public list & detail access without token
 * - Draft, Pending, Rejected lessons hidden (404 / excluded from list)
 * - Strict vocabulary ordering by order_index ASC
 * - Pagination correctness
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Task 5A.2: Public Lesson MockMvc Integration & Checkpoint 5A Tests")
class LessonIntegrationTests {

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

    private Lesson approvedLesson1;
    private Lesson approvedLesson2;
    private Lesson draftLesson;
    private Lesson pendingLesson;
    private Lesson rejectedLesson;

    private Vocabulary vocab1;
    private Vocabulary vocab2;
    private Vocabulary vocab3;

    private String learnerToken;

    @BeforeEach
    void setUp() {
        Account creator = new Account();
        creator.setEmailOrPhone("creator_controller_test@example.com");
        creator.setPasswordHash("$2a$12$dummyhashcreator123456789012345");
        creator.setStatus("Active");
        creator = accountRepository.save(creator);

        learnerToken = jwtUtil.generateToken("learner_user@example.com", List.of("Learner"));
        accountRepository.findByEmailOrPhone("learner_user@example.com")
                .orElseGet(() -> {
                    Account a = new Account();
                    a.setEmailOrPhone("learner_user@example.com");
                    a.setPasswordHash("hash1234567890");
                    a.setStatus("Active");
                    return accountRepository.save(a);
                });

        vocab1 = vocabularyRepository.save(new Vocabulary("学", "xué", "xue", "Học", "Học tập"));
        vocab2 = vocabularyRepository.save(new Vocabulary("生", "shēng", "sheng", "Sinh", "Học sinh"));
        vocab3 = vocabularyRepository.save(new Vocabulary("校", "xiào", "xiao", "Hiệu", "Trường học"));

        // 1. Approved Lesson 1 with 3 items added out-of-order (3, 1, 2)
        approvedLesson1 = new Lesson("Bài 1 - Trường học", creator);
        approvedLesson1.setStatus("Approved");
        approvedLesson1 = lessonRepository.save(approvedLesson1);

        lessonVocabularyRepository.save(new LessonVocabulary(approvedLesson1, vocab3, 3));
        lessonVocabularyRepository.save(new LessonVocabulary(approvedLesson1, vocab1, 1));
        lessonVocabularyRepository.save(new LessonVocabulary(approvedLesson1, vocab2, 2));

        // 2. Approved Lesson 2 with 1 item
        approvedLesson2 = new Lesson("Bài 2 - Chào hỏi", creator);
        approvedLesson2.setStatus("Approved");
        approvedLesson2 = lessonRepository.save(approvedLesson2);
        lessonVocabularyRepository.save(new LessonVocabulary(approvedLesson2, vocab1, 1));

        // 3. Draft Lesson
        draftLesson = new Lesson("Bài nháp - Private", creator);
        draftLesson.setStatus("Draft");
        draftLesson = lessonRepository.save(draftLesson);
        lessonVocabularyRepository.save(new LessonVocabulary(draftLesson, vocab2, 1));

        // 4. Pending Lesson
        pendingLesson = new Lesson("Bài chờ duyệt", creator);
        pendingLesson.setStatus("Pending");
        pendingLesson = lessonRepository.save(pendingLesson);

        // 5. Rejected Lesson
        rejectedLesson = new Lesson("Bài bị từ chối", creator);
        rejectedLesson.setStatus("Rejected");
        rejectedLesson = lessonRepository.save(rejectedLesson);
    }

    @Nested
    @DisplayName("1. Public Access & Checkpoint 5A (List Filtering)")
    class PublicListVerificationTests {

        @Test
        @DisplayName("GIVEN anonymous request WHEN GET /api/v1/lessons THEN returns 200 OK with only Approved lessons")
        void testAnonymousGetApprovedLessons() throws Exception {
            mockMvc.perform(get("/api/v1/lessons")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.items", hasSize(2)))
                    .andExpect(jsonPath("$.data.items[?(@.title == 'Bài 1 - Trường học')]").exists())
                    .andExpect(jsonPath("$.data.items[?(@.title == 'Bài 2 - Chào hỏi')]").exists())
                    // Verify non-approved lessons are NOT included
                    .andExpect(jsonPath("$.data.items[?(@.title == 'Bài nháp - Private')]").doesNotExist())
                    .andExpect(jsonPath("$.data.items[?(@.title == 'Bài chờ duyệt')]").doesNotExist())
                    .andExpect(jsonPath("$.data.items[?(@.title == 'Bài bị từ chối')]").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN authenticated request with Learner token WHEN GET /api/v1/lessons THEN also returns 200 OK")
        void testAuthenticatedGetApprovedLessons() throws Exception {
            mockMvc.perform(get("/api/v1/lessons")
                            .header("Authorization", "Bearer " + learnerToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @DisplayName("GIVEN pagination params WHEN GET /api/v1/lessons?page=0&size=1 THEN returns first page correctly")
        void testPaginationFirstPage() throws Exception {
            mockMvc.perform(get("/api/v1/lessons")
                            .param("page", "0")
                            .param("size", "1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.items", hasSize(1)));
        }

        @Test
        @DisplayName("GIVEN page beyond total pages WHEN GET /api/v1/lessons?page=999&size=20 THEN returns empty items with totalElements preserved")
        void testPaginationBeyondLastPage() throws Exception {
            mockMvc.perform(get("/api/v1/lessons")
                            .param("page", "999")
                            .param("size", "20")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page").value(999))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.items").isEmpty());
        }
    }

    @Nested
    @DisplayName("2. Detail Lookup & Checkpoint 5A (404 on Non-Approved Lessons)")
    class PublicDetailVerificationTests {

        @Test
        @DisplayName("GIVEN approved lesson ID WHEN GET /api/v1/lessons/{id} THEN returns 200 OK and ordered vocabularies 1, 2, 3")
        void testGetApprovedLessonById_success() throws Exception {
            mockMvc.perform(get("/api/v1/lessons/" + approvedLesson1.getLessonId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.lessonId").value(approvedLesson1.getLessonId()))
                    .andExpect(jsonPath("$.data.title").value("Bài 1 - Trường học"))
                    .andExpect(jsonPath("$.data.status").value("Approved"))
                    .andExpect(jsonPath("$.data.vocabularyCount").value(3))
                    .andExpect(jsonPath("$.data.vocabularies", hasSize(3)))
                    // Assert strictly ascending order: 1 -> 2 -> 3
                    .andExpect(jsonPath("$.data.vocabularies[0].orderIndex").value(1))
                    .andExpect(jsonPath("$.data.vocabularies[0].hanzi").value("学"))
                    .andExpect(jsonPath("$.data.vocabularies[1].orderIndex").value(2))
                    .andExpect(jsonPath("$.data.vocabularies[1].hanzi").value("生"))
                    .andExpect(jsonPath("$.data.vocabularies[2].orderIndex").value(3))
                    .andExpect(jsonPath("$.data.vocabularies[2].hanzi").value("校"));
        }

        @Test
        @DisplayName("GIVEN Draft lesson ID WHEN GET /api/v1/lessons/{id} THEN returns 404 NOT_FOUND (Checkpoint 5A)")
        void testGetDraftLessonById_hiddenAs404() throws Exception {
            mockMvc.perform(get("/api/v1/lessons/" + draftLesson.getLessonId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message", containsString("Không tìm thấy bài học với ID: " + draftLesson.getLessonId())));
        }

        @Test
        @DisplayName("GIVEN Pending lesson ID WHEN GET /api/v1/lessons/{id} THEN returns 404 NOT_FOUND (Checkpoint 5A)")
        void testGetPendingLessonById_hiddenAs404() throws Exception {
            mockMvc.perform(get("/api/v1/lessons/" + pendingLesson.getLessonId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message", containsString("Không tìm thấy bài học với ID: " + pendingLesson.getLessonId())));
        }

        @Test
        @DisplayName("GIVEN Rejected lesson ID WHEN GET /api/v1/lessons/{id} THEN returns 404 NOT_FOUND (Checkpoint 5A)")
        void testGetRejectedLessonById_hiddenAs404() throws Exception {
            mockMvc.perform(get("/api/v1/lessons/" + rejectedLesson.getLessonId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message", containsString("Không tìm thấy bài học với ID: " + rejectedLesson.getLessonId())));
        }

        @Test
        @DisplayName("GIVEN nonexistent lesson ID WHEN GET /api/v1/lessons/999999 THEN returns 404 NOT_FOUND")
        void testGetNonexistentLessonById_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/lessons/999999")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message", containsString("Không tìm thấy bài học với ID: 999999")));
        }
    }
}
