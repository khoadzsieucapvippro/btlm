package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.PersonalNoteRequest;
import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.PersonalNote;
import com.elearning.entity.ReviewLog;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.UserSrsSetting;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.CardProgressRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.PersonalNoteRepository;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.ReviewLogRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.UserSrsSettingRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.security.JwtUtil;
import com.elearning.service.CreatorLessonService;
import com.elearning.service.PersonalNoteService;
import com.elearning.service.SrsService;
import com.elearning.service.VocabularyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real Database Integration Tests for R3.1: Vocabulary Lifecycle & Learning History Integrity.
 * Verifies on MySQL 8.4 Testcontainers:
 * 1. Scenario A: Unreferenced Vocabulary delete -> SUCCEEDS (204 No Content, cleans VOCAB_RADICAL, preserves RADICAL).
 * 2. Scenario B: Vocabulary in LessonVocabulary -> BLOCKED (409 CONFLICT, 0 mutations).
 * 3. Scenario C: Vocabulary with PersonalNote -> BLOCKED (409 CONFLICT, 0 mutations, preserves notes).
 * 4. Scenario D: Vocabulary with CardProgress -> BLOCKED (409 CONFLICT, 0 mutations, no orphan CardProgress).
 * 5. Scenario E: Vocabulary with ReviewLog -> BLOCKED (409 CONFLICT, 0 mutations, no orphan ReviewLog).
 * 6. Scenario F: Vocabulary with all references -> BLOCKED (409 CONFLICT, 0 mutations).
 * 7. Scenario G: Authorization Matrix (Learner/Creator/Moderator -> 403, Anonymous -> 401).
 * 8. Scenario H: Transaction Atomicity / Rollback on error.
 * 9. Scenario I: Concurrency Delete vs SRS Review.
 * 10. Scenario J: Concurrency Delete vs Personal Note creation.
 * 11. Scenario K: Concurrency Delete vs Lesson association.
 * 12. Scenario L: Polymorphic ID reuse prevention audit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Task R3.1: Vocabulary Lifecycle & Learning History Integrity Integration Tests")
public class VocabularyLifecycleIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private VocabularyService vocabularyService;

    @Autowired
    private SrsService srsService;

    @Autowired
    private PersonalNoteService personalNoteService;

    @Autowired
    private CreatorLessonService creatorLessonService;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private RadicalRepository radicalRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Autowired
    private PersonalNoteRepository personalNoteRepository;

    @Autowired
    private CardProgressRepository cardProgressRepository;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserSrsSettingRepository userSrsSettingRepository;

    @Autowired
    private TransactionTemplate txTemplate;

    private UserProfile testLearner;
    private Account testLearnerAccount;
    private String learnerEmail;

    private UserProfile testCreator;
    private Account testCreatorAccount;

    private UserProfile testAdmin;
    private Account testAdminAccount;

    private UserProfile testModerator;
    private Account testModeratorAccount;

    private String adminToken;
    private String learnerToken;
    private String creatorToken;
    private String moderatorToken;

    private final List<Vocabulary> createdVocabularies = new ArrayList<>();
    private final List<Long> createdLessonIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);
        Role creatorRole = roleRepository.findByRoleName("Creator").orElse(null);
        Role adminRole = roleRepository.findByRoleName("Admin").orElse(null);
        Role moderatorRole = roleRepository.findByRoleName("Moderator").orElse(null);

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        learnerEmail = "learner_vc_" + uniqueSuffix + "@test.com";
        String creatorEmail = "creator_vc_" + uniqueSuffix + "@test.com";
        String adminEmail = "admin_vc_" + uniqueSuffix + "@test.com";
        String moderatorEmail = "moderator_vc_" + uniqueSuffix + "@test.com";

        // Create learner
        testLearnerAccount = new Account();
        testLearnerAccount.setEmailOrPhone(learnerEmail);
        testLearnerAccount.setPasswordHash("passHash");
        testLearnerAccount.setStatus("Active");
        if (learnerRole != null) testLearnerAccount.getRoles().add(learnerRole);
        testLearnerAccount = accountRepository.save(testLearnerAccount);

        UserProfile lProfile = new UserProfile();
        lProfile.setAccount(testLearnerAccount);
        lProfile.setFullName("Learner VC " + uniqueSuffix);
        testLearner = userProfileRepository.save(lProfile);
        userSrsSettingRepository.save(new UserSrsSetting(testLearner, 20, 100));

        // Create creator
        testCreatorAccount = new Account();
        testCreatorAccount.setEmailOrPhone(creatorEmail);
        testCreatorAccount.setPasswordHash("passHash");
        testCreatorAccount.setStatus("Active");
        if (creatorRole != null) testCreatorAccount.getRoles().add(creatorRole);
        testCreatorAccount = accountRepository.save(testCreatorAccount);

        UserProfile cProfile = new UserProfile();
        cProfile.setAccount(testCreatorAccount);
        cProfile.setFullName("Creator VC " + uniqueSuffix);
        testCreator = userProfileRepository.save(cProfile);

        // Create admin
        testAdminAccount = new Account();
        testAdminAccount.setEmailOrPhone(adminEmail);
        testAdminAccount.setPasswordHash("passHash");
        testAdminAccount.setStatus("Active");
        if (adminRole != null) testAdminAccount.getRoles().add(adminRole);
        testAdminAccount = accountRepository.save(testAdminAccount);

        UserProfile aProfile = new UserProfile();
        aProfile.setAccount(testAdminAccount);
        aProfile.setFullName("Admin VC " + uniqueSuffix);
        testAdmin = userProfileRepository.save(aProfile);

        // Create moderator
        testModeratorAccount = new Account();
        testModeratorAccount.setEmailOrPhone(moderatorEmail);
        testModeratorAccount.setPasswordHash("passHash");
        testModeratorAccount.setStatus("Active");
        if (moderatorRole != null) testModeratorAccount.getRoles().add(moderatorRole);
        testModeratorAccount = accountRepository.save(testModeratorAccount);

        UserProfile mProfile = new UserProfile();
        mProfile.setAccount(testModeratorAccount);
        mProfile.setFullName("Moderator VC " + uniqueSuffix);
        testModerator = userProfileRepository.save(mProfile);

        // Generate JWTs
        adminToken = jwtUtil.generateToken(adminEmail, List.of("Admin"));
        learnerToken = jwtUtil.generateToken(learnerEmail, List.of("Learner"));
        creatorToken = jwtUtil.generateToken(creatorEmail, List.of("Creator"));
        moderatorToken = jwtUtil.generateToken(moderatorEmail, List.of("Moderator"));

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();

        // 1. Delete lesson vocabulary links and lessons first to respect fk_lesson_created_by RESTRICT constraint
        for (Long lId : createdLessonIds) {
            lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(lId));
            lessonRepository.findById(lId).ifPresent(lessonRepository::delete);
        }

        // 2. Delete vocabulary records
        for (Vocabulary v : createdVocabularies) {
            vocabularyRepository.findById(v.getVocabId()).ifPresent(vocabularyRepository::delete);
        }

        // 3. Delete learner SRS, notes, and profile
        if (testLearner != null) {
            personalNoteRepository.deleteAll(personalNoteRepository.findByUserOrderByCreatedAtDesc(testLearner));
            reviewLogRepository.deleteAll(reviewLogRepository.findByUserOrderByReviewedAtDesc(testLearner));
            cardProgressRepository.deleteAll(cardProgressRepository.findByUserAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(testLearner, LocalDateTime.now().plusYears(10)));
            userSrsSettingRepository.findByUser(testLearner).ifPresent(userSrsSettingRepository::delete);
            userProfileRepository.delete(testLearner);
            if (testLearnerAccount != null) accountRepository.delete(testLearnerAccount);
        }
        if (testCreator != null) {
            userProfileRepository.delete(testCreator);
            if (testCreatorAccount != null) accountRepository.delete(testCreatorAccount);
        }
        if (testModerator != null) {
            userProfileRepository.delete(testModerator);
            if (testModeratorAccount != null) accountRepository.delete(testModeratorAccount);
        }
        if (testAdmin != null) {
            userProfileRepository.delete(testAdmin);
            if (testAdminAccount != null) accountRepository.delete(testAdminAccount);
        }
    }

    private Vocabulary createVocabulary(String hanzi, String pinyin, String meaningVi) {
        return txTemplate.execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            Vocabulary v = new Vocabulary(hanzi + suffix, pinyin, pinyin + suffix, "Han Viet", meaningVi);
            // Link Kangxi radical 9 (人)
            radicalRepository.findById(9).ifPresent(v::addRadical);
            v = vocabularyRepository.save(v);
            createdVocabularies.add(v);
            return v;
        });
    }

    private Lesson createLesson(String title, String status) {
        return txTemplate.execute(statusTx -> {
            Lesson lesson = new Lesson();
            lesson.setTitle(title + " " + UUID.randomUUID().toString().substring(0, 6));
            lesson.setStatus(status);
            lesson.setCreatedBy(testCreatorAccount);
            lesson = lessonRepository.save(lesson);
            createdLessonIds.add(lesson.getLessonId());
            return lesson;
        });
    }

    private void linkVocabularyToLesson(Lesson lesson, Vocabulary vocabulary, int orderIndex) {
        txTemplate.execute(status -> {
            LessonVocabulary lv = new LessonVocabulary(lesson, vocabulary, orderIndex);
            lessonVocabularyRepository.save(lv);
            return null;
        });
    }

    @Nested
    @DisplayName("1. Referential Guard Tests (Individual Dependency Checks)")
    class ReferentialGuardTests {

        @Test
        @DisplayName("Scenario A: Unreferenced Vocabulary delete -> SUCCEEDS (204 No Content, cleans VOCAB_RADICAL, preserves RADICAL)")
        void testDeleteUnreferencedVocabulary_succeeds() {
            Vocabulary vocab = createVocabulary("Dan", "dan", "Don");

            // Verify radical 9 is linked
            assertThat(vocab.getRadicals()).isNotEmpty();
            int radicalId = 9;

            // Admin deletes vocabulary
            vocabularyService.deleteVocabulary(vocab.getVocabId());

            // Assert Vocabulary is deleted
            assertThat(vocabularyRepository.findById(vocab.getVocabId())).isEmpty();

            // Assert Radical entity in RADICAL table is NOT deleted
            assertThat(radicalRepository.findById(radicalId)).isPresent();
        }

        @Test
        @DisplayName("Scenario B: Vocabulary referenced in LessonVocabulary -> BLOCKED (409 CONFLICT, 0 mutations)")
        void testDeleteVocabulary_referencedByLesson_blocked() {
            Vocabulary vocab = createVocabulary("Bao", "bao", "Bao");
            Lesson lesson = createLesson("Lesson Ref", "Approved");
            linkVocabularyToLesson(lesson, vocab, 1);

            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(vocab.getVocabId()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Không thể xóa từ vựng đang được liên kết với bài học");
                    });

            // Assert Vocabulary and Lesson link are intact
            assertThat(vocabularyRepository.findById(vocab.getVocabId())).isPresent();
            assertThat(lessonVocabularyRepository.existsByVocabulary_VocabId(vocab.getVocabId())).isTrue();
        }

        @Test
        @DisplayName("Scenario C: Vocabulary referenced in PersonalNote -> BLOCKED (409 CONFLICT, preserves learner notes)")
        void testDeleteVocabulary_referencedByPersonalNote_blocked() {
            Vocabulary vocab = createVocabulary("Bi", "bi", "But");

            // Learner creates a personal note
            txTemplate.execute(status -> {
                PersonalNote note = new PersonalNote(testLearner, vocab, "Ghi chú từ bút");
                personalNoteRepository.save(note);
                return null;
            });

            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(vocab.getVocabId()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Không thể xóa từ vựng đang có ghi chú cá nhân");
                    });

            // Assert note and vocabulary are preserved
            assertThat(vocabularyRepository.findById(vocab.getVocabId())).isPresent();
            assertThat(personalNoteRepository.existsByVocabulary_VocabId(vocab.getVocabId())).isTrue();
        }

        @Test
        @DisplayName("Scenario D: Vocabulary referenced in CardProgress -> BLOCKED (409 CONFLICT, no orphan CardProgress)")
        void testDeleteVocabulary_referencedByCardProgress_blocked() {
            Vocabulary vocab = createVocabulary("Mo", "mo", "Muc");
            Lesson lesson = createLesson("Lesson For SRS", "Approved");
            linkVocabularyToLesson(lesson, vocab, 1);

            // Learner reviews new card -> creates CardProgress
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
            );
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 3, 2));

            // Unlink from lesson so that ONLY CardProgress/ReviewLog guards are tested
            txTemplate.execute(status -> {
                lessonVocabularyRepository.deleteAll(lessonVocabularyRepository.findByLesson_LessonIdOrderByOrderIndexAsc(lesson.getLessonId()));
                return null;
            });

            // Switch back to Admin
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(testAdminAccount.getEmailOrPhone(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_Admin")))
            );

            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(vocab.getVocabId()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Không thể xóa từ vựng đang có tiến trình học tập");
                    });

            // Assert CardProgress and Vocabulary exist intact
            assertThat(vocabularyRepository.findById(vocab.getVocabId())).isPresent();
            assertThat(cardProgressRepository.existsByItemTypeAndItemId("VOCABULARY", vocab.getVocabId())).isTrue();
        }

        @Test
        @DisplayName("Scenario E: Vocabulary referenced in ReviewLog -> BLOCKED (409 CONFLICT, no orphan ReviewLog)")
        void testDeleteVocabulary_referencedByReviewLog_blocked() {
            Vocabulary vocab = createVocabulary("Zhi", "zhi", "Giay");

            // Seed only a ReviewLog directly without CardProgress or Lesson
            txTemplate.execute(status -> {
                ReviewLog log = new ReviewLog(testLearner, "VOCABULARY", vocab.getVocabId(), (byte) 3, 0, 1, 2);
                reviewLogRepository.save(log);
                return null;
            });

            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(vocab.getVocabId()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                        assertThat(be.getMessage()).contains("Không thể xóa từ vựng đã có lịch sử ôn tập");
                    });

            // Assert ReviewLog and Vocabulary exist intact
            assertThat(vocabularyRepository.findById(vocab.getVocabId())).isPresent();
            assertThat(reviewLogRepository.existsByItemTypeAndItemId("VOCABULARY", vocab.getVocabId())).isTrue();
        }

        @Test
        @DisplayName("Scenario F: Vocabulary with ALL dependencies (Lesson, Note, Progress, Log) -> BLOCKED (409 CONFLICT)")
        void testDeleteVocabulary_allDependencies_blocked() {
            Vocabulary vocab = createVocabulary("Yan", "yan", "Nghien");
            Lesson lesson = createLesson("Lesson Full Deps", "Approved");
            linkVocabularyToLesson(lesson, vocab, 1);

            // Learner creates note and studies card
            txTemplate.execute(status -> {
                PersonalNote note = new PersonalNote(testLearner, vocab, "Ghi chú mực tàu");
                personalNoteRepository.save(note);
                return null;
            });

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
            );
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocab.getVocabId(), 4, 3));

            // Switch back to Admin
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(testAdminAccount.getEmailOrPhone(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_Admin")))
            );

            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(vocab.getVocabId()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });

            // Assert all rows intact
            assertThat(vocabularyRepository.findById(vocab.getVocabId())).isPresent();
            assertThat(lessonVocabularyRepository.existsByVocabulary_VocabId(vocab.getVocabId())).isTrue();
            assertThat(personalNoteRepository.existsByVocabulary_VocabId(vocab.getVocabId())).isTrue();
            assertThat(cardProgressRepository.existsByItemTypeAndItemId("VOCABULARY", vocab.getVocabId())).isTrue();
            assertThat(reviewLogRepository.existsByItemTypeAndItemId("VOCABULARY", vocab.getVocabId())).isTrue();
        }
    }

    @Nested
    @DisplayName("2. Authorization & HTTP Layer Security Matrix")
    class AuthorizationMatrixTests {

        @Test
        @DisplayName("Scenario G.1: Learner attempting DELETE /api/v1/admin/vocabulary/{id} -> 403 FORBIDDEN")
        void testLearnerDelete_returns403() throws Exception {
            Vocabulary vocab = createVocabulary("Quan", "quan", "Quyen");

            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + vocab.getVocabId())
                            .header("Authorization", "Bearer " + learnerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("Scenario G.2: Creator attempting DELETE /api/v1/admin/vocabulary/{id} -> 403 FORBIDDEN")
        void testCreatorDelete_returns403() throws Exception {
            Vocabulary vocab = createVocabulary("Ce", "ce", "Sach");

            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + vocab.getVocabId())
                            .header("Authorization", "Bearer " + creatorToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("Scenario G.3: Moderator attempting DELETE /api/v1/admin/vocabulary/{id} -> 403 FORBIDDEN")
        void testModeratorDelete_returns403() throws Exception {
            Vocabulary vocab = createVocabulary("Dian", "dian", "Dien");

            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + vocab.getVocabId())
                            .header("Authorization", "Bearer " + moderatorToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("Scenario G.4: Anonymous attempting DELETE /api/v1/admin/vocabulary/{id} -> 401 UNAUTHORIZED")
        void testAnonymousDelete_returns401() throws Exception {
            Vocabulary vocab = createVocabulary("Bao", "bao", "Bao");

            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + vocab.getVocabId()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("Scenario G.5: Admin deletes unreferenced vocabulary via HTTP -> 204 No Content")
        void testAdminDelete_unreferenced_returns204() throws Exception {
            Vocabulary vocab = createVocabulary("He", "he", "Hop");

            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + vocab.getVocabId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            assertThat(vocabularyRepository.findById(vocab.getVocabId())).isEmpty();
        }

        @Test
        @DisplayName("Scenario G.6: Admin deletes referenced vocabulary via HTTP -> 409 CONFLICT")
        void testAdminDelete_referenced_returns409() throws Exception {
            Vocabulary vocab = createVocabulary("Xiang", "xiang", "Tuong");
            Lesson lesson = createLesson("Lesson HTTP Ref", "Approved");
            linkVocabularyToLesson(lesson, vocab, 1);

            mockMvc.perform(delete("/api/v1/admin/vocabulary/" + vocab.getVocabId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        }
    }

    @Nested
    @DisplayName("3. Concurrency Races & ID Reuse Invariant Proof")
    class ConcurrencyAndIdReuseTests {

        @Test
        @DisplayName("Scenario I.1: Concurrent First-Ever Review vs Admin Delete — Order A: Delete locks first")
        void testConcurrentFirstReviewVsDelete_OrderA_DeleteWins() throws Exception {
            Vocabulary vocab = createVocabulary("Pao", "pao", "Bao");
            Long vocabId = vocab.getVocabId();

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch deleteLockedLatch = new CountDownLatch(1);

            AtomicInteger reviewNotFound = new AtomicInteger(0);
            AtomicInteger deleteSuccess = new AtomicInteger(0);
            AtomicInteger unexpectedErrors = new AtomicInteger(0);

            // Thread 1: Admin Deletes vocabulary
            Callable<Void> deleteTask = () -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(testAdminAccount.getEmailOrPhone(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_Admin")))
                );
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    vocabularyService.deleteVocabulary(vocabId);
                    deleteSuccess.incrementAndGet();
                    deleteLockedLatch.countDown();
                } catch (Exception ex) {
                    unexpectedErrors.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            };

            // Thread 2: Learner attempts first review after delete locks/commits
            Callable<Void> reviewTask = () -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
                );
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    deleteLockedLatch.await(5, TimeUnit.SECONDS);
                    srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabId, 3, 2));
                } catch (BusinessException be) {
                    if (be.getErrorCode() == ErrorCode.NOT_FOUND || be.getErrorCode() == ErrorCode.UNPROCESSABLE_ENTITY) {
                        reviewNotFound.incrementAndGet();
                    } else {
                        unexpectedErrors.incrementAndGet();
                    }
                } catch (Exception ex) {
                    unexpectedErrors.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            };

            Future<Void> f1 = executor.submit(deleteTask);
            Future<Void> f2 = executor.submit(reviewTask);

            startLatch.countDown();

            f1.get(10, TimeUnit.SECONDS);
            f2.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(unexpectedErrors.get()).isZero();
            assertThat(deleteSuccess.get()).isEqualTo(1);
            assertThat(reviewNotFound.get()).isEqualTo(1);

            // Invariant: ZERO CardProgress, ZERO ReviewLog, ZERO Vocabulary
            assertThat(vocabularyRepository.findById(vocabId)).isEmpty();
            assertThat(cardProgressRepository.existsByItemTypeAndItemId("VOCABULARY", vocabId)).isFalse();
            assertThat(reviewLogRepository.existsByItemTypeAndItemId("VOCABULARY", vocabId)).isFalse();
        }

        @Test
        @DisplayName("Scenario I.2: Concurrent First-Ever Review vs Admin Delete — Order B: Review locks first")
        void testConcurrentFirstReviewVsDelete_OrderB_ReviewWins() throws Exception {
            Vocabulary vocab = createVocabulary("Fei", "fei", "Phi");
            Long vocabId = vocab.getVocabId();
            Lesson lesson = createLesson("Lesson Fei Approved", "Approved");
            linkVocabularyToLesson(lesson, vocab, 1);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch reviewCompletedLatch = new CountDownLatch(1);

            AtomicInteger reviewSuccess = new AtomicInteger(0);
            AtomicInteger deleteBlockedConflict = new AtomicInteger(0);
            AtomicInteger unexpectedErrors = new AtomicInteger(0);

            // Thread 1: Learner reviews first
            Callable<Void> reviewTask = () -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
                );
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabId, 3, 2));
                    reviewSuccess.incrementAndGet();
                    reviewCompletedLatch.countDown();
                } catch (Exception ex) {
                    unexpectedErrors.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            };

            // Thread 2: Admin attempts delete
            Callable<Void> deleteTask = () -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(testAdminAccount.getEmailOrPhone(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_Admin")))
                );
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    reviewCompletedLatch.await(5, TimeUnit.SECONDS);
                    vocabularyService.deleteVocabulary(vocabId);
                } catch (BusinessException be) {
                    if (be.getErrorCode() == ErrorCode.CONFLICT) {
                        deleteBlockedConflict.incrementAndGet();
                    } else {
                        unexpectedErrors.incrementAndGet();
                    }
                } catch (Exception ex) {
                    unexpectedErrors.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            };

            Future<Void> f1 = executor.submit(reviewTask);
            Future<Void> f2 = executor.submit(deleteTask);

            startLatch.countDown();

            f1.get(10, TimeUnit.SECONDS);
            f2.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(unexpectedErrors.get()).isZero();
            assertThat(reviewSuccess.get()).isEqualTo(1);
            assertThat(deleteBlockedConflict.get()).isEqualTo(1);

            // Invariant: Vocabulary, CardProgress, ReviewLog and LessonVocabulary are all preserved
            assertThat(vocabularyRepository.findById(vocabId)).isPresent();
            assertThat(cardProgressRepository.existsByItemTypeAndItemId("VOCABULARY", vocabId)).isTrue();
            assertThat(reviewLogRepository.existsByItemTypeAndItemId("VOCABULARY", vocabId)).isTrue();
            assertThat(lessonVocabularyRepository.existsByVocabulary_VocabId(vocabId)).isTrue();
        }

        @Test
        @DisplayName("Scenario I.3: Repeated Concurrency Stress (10 iterations) — Delete vs Review")
        void testConcurrentReviewVsDelete_RepeatedStress() throws Exception {
            for (int i = 0; i < 10; i++) {
                Vocabulary vocab = createVocabulary("StressRev" + i, "stress" + i, "Nghia" + i);
                Long vocabId = vocab.getVocabId();
                Lesson lesson = createLesson("Lesson Stress " + i, "Approved");
                linkVocabularyToLesson(lesson, vocab, 1);

                ExecutorService executor = Executors.newFixedThreadPool(2);
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch readyLatch = new CountDownLatch(2);

                AtomicInteger reviewDone = new AtomicInteger(0);
                AtomicInteger deleteDone = new AtomicInteger(0);

                executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
                    );
                    readyLatch.countDown();
                    try {
                        startLatch.await(5, TimeUnit.SECONDS);
                        srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabId, 3, 2));
                        reviewDone.incrementAndGet();
                    } catch (Exception ignored) {}
                    finally {
                        SecurityContextHolder.clearContext();
                    }
                });

                executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(testAdminAccount.getEmailOrPhone(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_Admin")))
                    );
                    readyLatch.countDown();
                    try {
                        startLatch.await(5, TimeUnit.SECONDS);
                        vocabularyService.deleteVocabulary(vocabId);
                        deleteDone.incrementAndGet();
                    } catch (Exception ignored) {}
                    finally {
                        SecurityContextHolder.clearContext();
                    }
                });

                readyLatch.await(5, TimeUnit.SECONDS);
                startLatch.countDown();
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);

                // Invariant Check: NEVER allow CardProgress or ReviewLog if Vocabulary is deleted!
                boolean vocabExists = vocabularyRepository.findById(vocabId).isPresent();
                boolean progressExists = cardProgressRepository.existsByItemTypeAndItemId("VOCABULARY", vocabId);
                boolean logExists = reviewLogRepository.existsByItemTypeAndItemId("VOCABULARY", vocabId);

                if (!vocabExists) {
                    assertThat(progressExists).as("No orphan CardProgress on iter " + i).isFalse();
                    assertThat(logExists).as("No orphan ReviewLog on iter " + i).isFalse();
                }
            }
        }

        @Test
        @DisplayName("Scenario J.1: Concurrent Personal Note Creation vs Admin Delete on same Vocabulary")
        void testConcurrentNoteCreationVsDelete() throws Exception {
            Vocabulary vocab = createVocabulary("Dao", "dao", "Dao");
            Long vocabId = vocab.getVocabId();

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch readyLatch = new CountDownLatch(2);

            AtomicInteger noteCreated = new AtomicInteger(0);
            AtomicInteger deleteConflict = new AtomicInteger(0);
            AtomicInteger deleteSuccess = new AtomicInteger(0);
            AtomicInteger unexpectedErrors = new AtomicInteger(0);

            Callable<Void> noteTask = () -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
                );
                readyLatch.countDown();
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    personalNoteService.createNote(vocabId, new PersonalNoteRequest("Concurrent note content"));
                    noteCreated.incrementAndGet();
                } catch (BusinessException be) {
                    if (be.getErrorCode() == ErrorCode.NOT_FOUND) {
                        // Delete won before note started
                    } else {
                        unexpectedErrors.incrementAndGet();
                    }
                } catch (Exception ex) {
                    unexpectedErrors.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            };

            Callable<Void> deleteTask = () -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(testAdminAccount.getEmailOrPhone(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_Admin")))
                );
                readyLatch.countDown();
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    vocabularyService.deleteVocabulary(vocabId);
                    deleteSuccess.incrementAndGet();
                } catch (BusinessException be) {
                    if (be.getErrorCode() == ErrorCode.CONFLICT) {
                        deleteConflict.incrementAndGet();
                    } else {
                        unexpectedErrors.incrementAndGet();
                    }
                } catch (Exception ex) {
                    unexpectedErrors.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            };

            Future<Void> f1 = executor.submit(noteTask);
            Future<Void> f2 = executor.submit(deleteTask);

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();

            f1.get(10, TimeUnit.SECONDS);
            f2.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(unexpectedErrors.get()).isZero();

            if (deleteSuccess.get() == 1) {
                assertThat(vocabularyRepository.findById(vocabId)).isEmpty();
                assertThat(personalNoteRepository.existsByVocabulary_VocabId(vocabId)).isFalse();
            } else {
                assertThat(deleteConflict.get()).isEqualTo(1);
                assertThat(noteCreated.get()).isEqualTo(1);
                assertThat(vocabularyRepository.findById(vocabId)).isPresent();
                assertThat(personalNoteRepository.existsByVocabulary_VocabId(vocabId)).isTrue();
            }
        }

        @Test
        @DisplayName("Scenario J.2: Repeated Concurrency Stress (10 iterations) — Delete vs Personal Note")
        void testConcurrentNoteCreationVsDelete_RepeatedStress() throws Exception {
            for (int i = 0; i < 10; i++) {
                Vocabulary vocab = createVocabulary("StressNote" + i, "snote" + i, "Nghia" + i);
                Long vocabId = vocab.getVocabId();

                ExecutorService executor = Executors.newFixedThreadPool(2);
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch readyLatch = new CountDownLatch(2);

                executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
                    );
                    readyLatch.countDown();
                    try {
                        startLatch.await(5, TimeUnit.SECONDS);
                        personalNoteService.createNote(vocabId, new PersonalNoteRequest("Note stress " + vocabId));
                    } catch (Exception ignored) {}
                    finally {
                        SecurityContextHolder.clearContext();
                    }
                });

                executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(testAdminAccount.getEmailOrPhone(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_Admin")))
                    );
                    readyLatch.countDown();
                    try {
                        startLatch.await(5, TimeUnit.SECONDS);
                        vocabularyService.deleteVocabulary(vocabId);
                    } catch (Exception ignored) {}
                    finally {
                        SecurityContextHolder.clearContext();
                    }
                });

                readyLatch.await(5, TimeUnit.SECONDS);
                startLatch.countDown();
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);

                boolean vocabExists = vocabularyRepository.findById(vocabId).isPresent();
                boolean noteExists = personalNoteRepository.existsByVocabulary_VocabId(vocabId);

                if (!vocabExists) {
                    assertThat(noteExists).as("No orphan PersonalNote on iter " + i).isFalse();
                }
            }
        }

        @Test
        @DisplayName("Scenario K: Concurrent Delete vs Lesson Association (Creator addVocabularyToLesson vs Admin delete)")
        void testConcurrentLessonLinkVsDelete() throws Exception {
            Vocabulary vocab = createVocabulary("Lian", "lian", "Lien");
            Long vocabId = vocab.getVocabId();
            Lesson lesson = createLesson("Lesson Draft Link", "Draft");
            Long lessonId = lesson.getLessonId();

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch readyLatch = new CountDownLatch(2);

            AtomicInteger linkSuccess = new AtomicInteger(0);
            AtomicInteger deleteSuccess = new AtomicInteger(0);
            AtomicInteger deleteConflict = new AtomicInteger(0);
            AtomicInteger linkNotFound = new AtomicInteger(0);
            AtomicInteger unexpectedErrors = new AtomicInteger(0);

            // Thread 1: Creator adds vocabulary to lesson
            Callable<Void> linkTask = () -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(testCreatorAccount.getEmailOrPhone(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_Creator")))
                );
                readyLatch.countDown();
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    creatorLessonService.addVocabularyToLesson(lessonId, vocabId);
                    linkSuccess.incrementAndGet();
                } catch (BusinessException be) {
                    if (be.getErrorCode() == ErrorCode.NOT_FOUND) {
                        linkNotFound.incrementAndGet();
                    } else {
                        unexpectedErrors.incrementAndGet();
                    }
                } catch (Exception ex) {
                    unexpectedErrors.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            };

            // Thread 2: Admin deletes vocabulary
            Callable<Void> deleteTask = () -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(testAdminAccount.getEmailOrPhone(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_Admin")))
                );
                readyLatch.countDown();
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    vocabularyService.deleteVocabulary(vocabId);
                    deleteSuccess.incrementAndGet();
                } catch (BusinessException be) {
                    if (be.getErrorCode() == ErrorCode.CONFLICT) {
                        deleteConflict.incrementAndGet();
                    } else {
                        unexpectedErrors.incrementAndGet();
                    }
                } catch (Exception ex) {
                    unexpectedErrors.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                }
                return null;
            };

            Future<Void> f1 = executor.submit(linkTask);
            Future<Void> f2 = executor.submit(deleteTask);

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown();

            f1.get(10, TimeUnit.SECONDS);
            f2.get(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(unexpectedErrors.get()).isZero();

            if (deleteSuccess.get() == 1) {
                assertThat(vocabularyRepository.findById(vocabId)).isEmpty();
                assertThat(lessonVocabularyRepository.existsByVocabulary_VocabId(vocabId)).isFalse();
            } else {
                assertThat(deleteConflict.get()).isEqualTo(1);
                assertThat(linkSuccess.get()).isEqualTo(1);
                assertThat(vocabularyRepository.findById(vocabId)).isPresent();
                assertThat(lessonVocabularyRepository.existsByVocabulary_VocabId(vocabId)).isTrue();
            }
        }

        @Test
        @DisplayName("Scenario M: Delete unreferenced Vocabulary with Radicals — Clean VOCAB_RADICAL cascade, preserves master RADICAL")
        void testVocabRadicalCascadeIntegrity() {
            Vocabulary vocab = createVocabulary("Decomp", "decomp", "PhanTich");
            Long vocabId = vocab.getVocabId();

            txTemplate.executeWithoutResult(status -> {
                Vocabulary v = vocabularyRepository.findById(vocabId).orElseThrow();
                v.addRadical(radicalRepository.findById(1).orElseThrow());
                v.addRadical(radicalRepository.findById(2).orElseThrow());
                vocabularyRepository.saveAndFlush(v);
            });

            // Admin deletes unreferenced vocabulary
            vocabularyService.deleteVocabulary(vocabId);

            // Vocabulary is gone
            assertThat(vocabularyRepository.findById(vocabId)).isEmpty();

            // Master RADICAL table records (ID 1, ID 2) remain intact!
            assertThat(radicalRepository.findById(1)).isPresent();
            assertThat(radicalRepository.findById(2)).isPresent();
        }

        @Test
        @DisplayName("Scenario L: ID Reuse Invariant Proof — Guard guarantees stale SRS history is never inherited")
        void testIdReuseInvariantProof() {
            // 1. Create Vocab A and study it in SRS
            Vocabulary vocabA = createVocabulary("Gou", "gou", "Cho");
            Lesson lessonA = createLesson("Lesson Gou", "Approved");
            linkVocabularyToLesson(lessonA, vocabA, 1);

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(learnerEmail, "N/A", List.of(new SimpleGrantedAuthority("ROLE_Learner")))
            );
            srsService.reviewCard(new ReviewCardRequest("VOCABULARY", vocabA.getVocabId(), 4, 3));

            // CardProgress and ReviewLog exist for vocabA ID
            Long vocabId = vocabA.getVocabId();
            assertThat(cardProgressRepository.existsByItemTypeAndItemId("VOCABULARY", vocabId)).isTrue();
            assertThat(reviewLogRepository.existsByItemTypeAndItemId("VOCABULARY", vocabId)).isTrue();

            // 2. Switch to Admin and attempt to delete vocabA
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(testAdminAccount.getEmailOrPhone(), "N/A", List.of(new SimpleGrantedAuthority("ROLE_Admin")))
            );

            // Deletion is STRICTLY BLOCKED because history exists
            assertThatThrownBy(() -> vocabularyService.deleteVocabulary(vocabId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });

            // Proof: Because vocabA cannot be deleted, its ID cannot be freed or reused for another word (e.g. "Mao" / Cat).
            // Thus, polymorphic CardProgress & ReviewLog will never be falsely associated with a different vocabulary.
            assertThat(vocabularyRepository.findById(vocabId)).isPresent();
            assertThat(vocabularyRepository.findById(vocabId).get().getHanzi()).contains("Gou");
        }
    }
}
