package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.PersonalNoteRequest;
import com.elearning.dto.response.PageResponse;
import com.elearning.dto.response.PersonalNoteResponse;
import com.elearning.entity.Account;
import com.elearning.entity.PersonalNote;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.PersonalNoteRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.PersonalNoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("Task 8A.1: PersonalNoteService Integration Tests (MySQL 8.4)")
class PersonalNoteServiceIntegrationTests {

    @Autowired
    private PersonalNoteService personalNoteService;

    @Autowired
    private PersonalNoteRepository personalNoteRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    private UserProfile userA;
    private UserProfile userB;
    private Vocabulary vocab1;
    private Vocabulary vocab2;

    @BeforeEach
    void setUp() {
        Role learnerRole = roleRepository.findByRoleName("Learner").orElse(null);

        // Create User A
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        Account accA = new Account();
        accA.setEmailOrPhone("note_user_a_" + suffixA + "@test.com");
        accA.setPasswordHash("hash");
        accA.setStatus("Active");
        if (learnerRole != null) {
            accA.getRoles().add(learnerRole);
        }
        accA = accountRepository.save(accA);

        userA = new UserProfile();
        userA.setAccount(accA);
        userA.setFullName("Note User A " + suffixA);
        userA = userProfileRepository.save(userA);

        // Create User B
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        Account accB = new Account();
        accB.setEmailOrPhone("note_user_b_" + suffixB + "@test.com");
        accB.setPasswordHash("hash");
        accB.setStatus("Active");
        if (learnerRole != null) {
            accB.getRoles().add(learnerRole);
        }
        accB = accountRepository.save(accB);

        userB = new UserProfile();
        userB.setAccount(accB);
        userB.setFullName("Note User B " + suffixB);
        userB = userProfileRepository.save(userB);

        // Create Vocab 1
        vocab1 = vocabularyRepository.findByHanziAndPinyinRaw("好", "hao")
                .orElseGet(() -> vocabularyRepository.save(new Vocabulary("好", "hǎo", "hao", "Hảo", "Tốt, đẹp")));

        // Create Vocab 2
        vocab2 = vocabularyRepository.findByHanziAndPinyinRaw("看", "kan")
                .orElseGet(() -> vocabularyRepository.save(new Vocabulary("看", "kàn", "kan", "Khán", "Nhìn, xem")));

        // Default auth as User A
        authenticateAs(userA);
    }

    private void authenticateAs(UserProfile user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getAccount().getEmailOrPhone(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("1. Create & Read Notes")
    class CreateAndReadTests {

        @Test
        @DisplayName("Create note with valid content and retrieve via getNotesByVocabulary")
        void testCreateAndGetNotes_success() {
            PersonalNoteRequest request = new PersonalNoteRequest("Ghi chú cách dùng từ 好: 你好, 好吃。");
            PersonalNoteResponse created = personalNoteService.createNote(vocab1.getVocabId(), request);

            assertThat(created).isNotNull();
            assertThat(created.getNoteId()).isNotNull();
            assertThat(created.getVocabId()).isEqualTo(vocab1.getVocabId());
            assertThat(created.getContent()).isEqualTo("Ghi chú cách dùng từ 好: 你好, 好吃。");
            assertThat(created.getCreatedAt()).isNotNull();

            // Verify via getNotesByVocabulary
            PageResponse<PersonalNoteResponse> notes = personalNoteService.getNotesByVocabulary(vocab1.getVocabId(), Pageable.unpaged());
            assertThat(notes.getItems()).hasSize(1);
            assertThat(notes.getItems().get(0).getNoteId()).isEqualTo(created.getNoteId());
        }

        @Test
        @DisplayName("Create note with exactly 500 characters succeeds and stores exact content")
        void testExact500Chars_persistedSuccessfully() {
            String content500 = "Ghi chú Tiếng Trung và Tiếng Việt: 很好! " + "A".repeat(461);
            assertThat(content500).hasSize(500);

            PersonalNoteResponse response = personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest(content500));

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(500);

            // Direct DB verification
            PersonalNote note = personalNoteRepository.findById(response.getNoteId()).orElseThrow();
            assertThat(note.getContent()).hasSize(500);
            assertThat(note.getContent()).isEqualTo(content500);
        }

        @Test
        @DisplayName("Create note with 501 characters is rejected by service without inserting DB row")
        void test501Chars_rejectedByService() {
            long initialCount = personalNoteRepository.count();
            String content501 = "X".repeat(501);

            assertThatThrownBy(() -> personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest(content501)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

            assertThat(personalNoteRepository.count()).isEqualTo(initialCount);
        }

        @Test
        @DisplayName("Unlimited notes invariant: creating 6+ notes on same vocabulary for same user succeeds (no 5-note cap)")
        void testUnlimitedNotes_noFiveNoteCap() {
            for (int i = 1; i <= 6; i++) {
                PersonalNoteResponse resp = personalNoteService.createNote(
                        vocab1.getVocabId(),
                        new PersonalNoteRequest("Ghi chú số " + i + " cho từ vựng 好")
                );
                assertThat(resp).isNotNull();
            }

            PageResponse<PersonalNoteResponse> notes = personalNoteService.getNotesByVocabulary(vocab1.getVocabId(), Pageable.unpaged());
            assertThat(notes.getItems()).hasSize(6);
        }
    }

    @Nested
    @DisplayName("2. Multi-User & Multi-Vocabulary Isolation")
    class IsolationTests {

        @Test
        @DisplayName("Cross-User Isolation: User A and User B notes on same vocabulary remain isolated")
        void testUserIsolation() {
            // User A creates 3 notes
            authenticateAs(userA);
            personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest("User A Note 1"));
            personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest("User A Note 2"));
            personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest("User A Note 3"));

            // User B creates 2 notes
            authenticateAs(userB);
            personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest("User B Note 1"));
            personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest("User B Note 2"));

            // User A query
            authenticateAs(userA);
            PageResponse<PersonalNoteResponse> notesA = personalNoteService.getNotesByVocabulary(vocab1.getVocabId(), Pageable.unpaged());
            assertThat(notesA.getItems()).hasSize(3);
            assertThat(notesA.getItems()).allMatch(n -> n.getContent().startsWith("User A"));

            // User B query
            authenticateAs(userB);
            PageResponse<PersonalNoteResponse> notesB = personalNoteService.getNotesByVocabulary(vocab1.getVocabId(), Pageable.unpaged());
            assertThat(notesB.getItems()).hasSize(2);
            assertThat(notesB.getItems()).allMatch(n -> n.getContent().startsWith("User B"));
        }

        @Test
        @DisplayName("Vocabulary Isolation: Notes on vocab1 are not returned when querying vocab2")
        void testVocabularyIsolation() {
            authenticateAs(userA);
            personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest("Note for 好"));
            personalNoteService.createNote(vocab2.getVocabId(), new PersonalNoteRequest("Note for 看"));

            PageResponse<PersonalNoteResponse> notesVocab1 = personalNoteService.getNotesByVocabulary(vocab1.getVocabId(), Pageable.unpaged());
            assertThat(notesVocab1.getItems()).hasSize(1);
            assertThat(notesVocab1.getItems().get(0).getContent()).isEqualTo("Note for 好");

            PageResponse<PersonalNoteResponse> notesVocab2 = personalNoteService.getNotesByVocabulary(vocab2.getVocabId(), Pageable.unpaged());
            assertThat(notesVocab2.getItems()).hasSize(1);
            assertThat(notesVocab2.getItems().get(0).getContent()).isEqualTo("Note for 看");
        }
    }

    @Nested
    @DisplayName("3. Update & Delete Ownership Enforcement")
    class OwnershipTests {

        @Test
        @DisplayName("Update note: Owner can update; created_at and vocab_id remain immutable")
        void testUpdateNote_owner_success() {
            authenticateAs(userA);
            PersonalNoteResponse created = personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest("Original content"));
            LocalDateTime originalCreatedAt = created.getCreatedAt();

            PersonalNoteResponse updated = personalNoteService.updateNote(created.getNoteId(), new PersonalNoteRequest("Updated content"));

            assertThat(updated.getContent()).isEqualTo("Updated content");
            assertThat(updated.getVocabId()).isEqualTo(vocab1.getVocabId());
            assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);

            // Direct DB check
            PersonalNote note = personalNoteRepository.findById(created.getNoteId()).orElseThrow();
            assertThat(note.getContent()).isEqualTo("Updated content");
            assertThat(note.getVocabulary().getVocabId()).isEqualTo(vocab1.getVocabId());
            assertThat(note.getUser().getUserId()).isEqualTo(userA.getUserId());
        }

        @Test
        @DisplayName("Update note: User B attempting to update User A's note throws FORBIDDEN (403) and leaves content unmodified")
        void testUpdateNote_otherUser_forbidden() {
            // User A creates note
            authenticateAs(userA);
            PersonalNoteResponse noteA = personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest("User A original"));

            // User B attempts update
            authenticateAs(userB);
            assertThatThrownBy(() -> personalNoteService.updateNote(noteA.getNoteId(), new PersonalNoteRequest("Hacked content")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

            // Verify content unchanged on DB
            PersonalNote noteOnDb = personalNoteRepository.findById(noteA.getNoteId()).orElseThrow();
            assertThat(noteOnDb.getContent()).isEqualTo("User A original");
        }

        @Test
        @DisplayName("Delete note: Owner can delete note successfully")
        void testDeleteNote_owner_success() {
            authenticateAs(userA);
            PersonalNoteResponse created = personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest("To be deleted"));

            personalNoteService.deleteNote(created.getNoteId());

            assertThat(personalNoteRepository.findById(created.getNoteId())).isEmpty();
        }

        @Test
        @DisplayName("Delete note: User B attempting to delete User A's note throws FORBIDDEN (403) and leaves note intact")
        void testDeleteNote_otherUser_forbidden() {
            // User A creates note
            authenticateAs(userA);
            PersonalNoteResponse noteA = personalNoteService.createNote(vocab1.getVocabId(), new PersonalNoteRequest("User A preserved"));

            // User B attempts delete
            authenticateAs(userB);
            assertThatThrownBy(() -> personalNoteService.deleteNote(noteA.getNoteId()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

            // Verify note still exists on DB
            assertThat(personalNoteRepository.findById(noteA.getNoteId())).isPresent();
        }
    }
}
