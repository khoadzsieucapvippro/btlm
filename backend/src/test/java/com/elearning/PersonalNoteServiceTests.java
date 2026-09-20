package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.PersonalNoteRequest;
import com.elearning.dto.response.PageResponse;
import com.elearning.dto.response.PersonalNoteResponse;
import com.elearning.entity.Account;
import com.elearning.entity.PersonalNote;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.PersonalNoteRepository;
import com.elearning.repository.UserProfileRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.impl.PersonalNoteServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 8A.1: PersonalNoteService Unit Tests")
class PersonalNoteServiceTests {

    @Mock
    private PersonalNoteRepository personalNoteRepository;

    @Mock
    private VocabularyRepository vocabularyRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private PersonalNoteServiceImpl personalNoteService;

    private UserProfile userA;
    private UserProfile userB;
    private Vocabulary vocab1;

    @BeforeEach
    void setUp() {
        Account accA = new Account();
        accA.setAccountId(1L);
        accA.setEmailOrPhone("usera@test.com");

        userA = new UserProfile();
        userA.setUserId(10L);
        userA.setAccount(accA);

        Account accB = new Account();
        accB.setAccountId(2L);
        accB.setEmailOrPhone("userb@test.com");

        userB = new UserProfile();
        userB.setUserId(20L);
        userB.setAccount(accB);

        vocab1 = new Vocabulary();
        vocab1.setVocabId(100L);
        vocab1.setHanzi("学");

        // Authenticate as User A by default
        authenticateAs("usera@test.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String emailOrPhone) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                emailOrPhone,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("1. Create Note Tests")
    class CreateNoteTests {

        @Test
        @DisplayName("createNote: valid request creates note successfully")
        void testCreateNote_success() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));
            when(vocabularyRepository.findByIdWithLock(100L)).thenReturn(Optional.of(vocab1));

            PersonalNote saved = new PersonalNote(userA, vocab1, "Ghi chú từ vựng 学");
            saved.setNoteId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            when(personalNoteRepository.save(any(PersonalNote.class))).thenReturn(saved);

            PersonalNoteRequest request = new PersonalNoteRequest("Ghi chú từ vựng 学");
            PersonalNoteResponse response = personalNoteService.createNote(100L, request);

            assertThat(response).isNotNull();
            assertThat(response.getNoteId()).isEqualTo(1L);
            assertThat(response.getVocabId()).isEqualTo(100L);
            assertThat(response.getContent()).isEqualTo("Ghi chú từ vựng 学");

            verify(personalNoteRepository).save(any(PersonalNote.class));
        }

        @Test
        @DisplayName("createNote: 501 chars fails defense-in-depth validation")
        void testCreateNote_501Chars_fails() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));

            String content501 = "X".repeat(501);
            PersonalNoteRequest request = new PersonalNoteRequest(content501);

            assertThatThrownBy(() -> personalNoteService.createNote(100L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

            verify(personalNoteRepository, never()).save(any());
        }

        @Test
        @DisplayName("createNote: nonexistent vocabulary throws NOT_FOUND")
        void testCreateNote_nonexistentVocabulary_throwsNotFound() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));
            when(vocabularyRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

            PersonalNoteRequest request = new PersonalNoteRequest("Ghi chú");

            assertThatThrownBy(() -> personalNoteService.createNote(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

            verify(personalNoteRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("2. Read / List Notes Tests (Paginated)")
    class GetNotesTests {

        @Test
        @DisplayName("getNotesByVocabulary: returns paginated notes for current user and vocabulary")
        void testGetNotes_success() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));
            when(vocabularyRepository.findById(100L)).thenReturn(Optional.of(vocab1));

            PersonalNote n1 = new PersonalNote(userA, vocab1, "Note 1");
            n1.setNoteId(1L);
            PersonalNote n2 = new PersonalNote(userA, vocab1, "Note 2");
            n2.setNoteId(2L);
            List<PersonalNote> notesList = List.of(n1, n2);
            Page<PersonalNote> notesPage = new PageImpl<>(notesList, PageRequest.of(0, 20), 2);
            when(personalNoteRepository.findByUserAndVocabulary(eq(userA), eq(vocab1), any(Pageable.class)))
                    .thenReturn(notesPage);

            Pageable pageable = PageRequest.of(0, 20);
            PageResponse<PersonalNoteResponse> response = personalNoteService.getNotesByVocabulary(100L, pageable);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(2);
            assertThat(response.getItems()).extracting(PersonalNoteResponse::getNoteId).containsExactly(1L, 2L);
            assertThat(response.getTotalElements()).isEqualTo(2);
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("getNotesByVocabulary: nonexistent vocabulary throws NOT_FOUND")
        void testGetNotes_nonexistentVocab_throwsNotFound() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));
            when(vocabularyRepository.findById(999L)).thenReturn(Optional.empty());

            Pageable pageable = PageRequest.of(0, 20);
            assertThatThrownBy(() -> personalNoteService.getNotesByVocabulary(999L, pageable))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("getNotesByVocabulary: enforces maximum page size server-side")
        void testGetNotes_enforcesMaxPageSize() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));
            when(vocabularyRepository.findById(100L)).thenReturn(Optional.of(vocab1));

            // Request size=1000000 should be clamped to MAX_PAGE_SIZE (100)
            Pageable pageable = PageRequest.of(0, 1000000);
            Page<PersonalNote> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
            when(personalNoteRepository.findByUserAndVocabulary(eq(userA), eq(vocab1), any(Pageable.class)))
                    .thenReturn(emptyPage);

            PageResponse<PersonalNoteResponse> response = personalNoteService.getNotesByVocabulary(100L, pageable);

            assertThat(response).isNotNull();
            assertThat(response.getSize()).isLessThanOrEqualTo(100);
        }
    }

    @Nested
    @DisplayName("3. Update Note Tests")
    class UpdateNoteTests {

        @Test
        @DisplayName("updateNote: owner can update note content successfully")
        void testUpdateNote_success() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));

            PersonalNote existing = new PersonalNote(userA, vocab1, "Old content");
            existing.setNoteId(5L);
            when(personalNoteRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(personalNoteRepository.save(existing)).thenReturn(existing);

            PersonalNoteRequest request = new PersonalNoteRequest("New content");
            PersonalNoteResponse response = personalNoteService.updateNote(5L, request);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo("New content");
            assertThat(existing.getContent()).isEqualTo("New content");
            verify(personalNoteRepository).save(existing);
        }

        @Test
        @DisplayName("updateNote: user B attempting to update user A's note throws FORBIDDEN (403)")
        void testUpdateNote_unauthorized_throwsForbidden() {
            authenticateAs("userb@test.com");
            when(userProfileRepository.findByAccountEmailOrPhone("userb@test.com")).thenReturn(Optional.of(userB));

            // Note owned by user A (userId = 10), but caller is user B (userId = 20)
            PersonalNote noteOwnedByA = new PersonalNote(userA, vocab1, "User A note");
            noteOwnedByA.setNoteId(5L);
            when(personalNoteRepository.findById(5L)).thenReturn(Optional.of(noteOwnedByA));

            PersonalNoteRequest request = new PersonalNoteRequest("Hacked content");

            assertThatThrownBy(() -> personalNoteService.updateNote(5L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

            assertThat(noteOwnedByA.getContent()).isEqualTo("User A note");
            verify(personalNoteRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateNote: nonexistent noteId throws NOT_FOUND")
        void testUpdateNote_nonexistent_throwsNotFound() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));
            when(personalNoteRepository.findById(999L)).thenReturn(Optional.empty());

            PersonalNoteRequest request = new PersonalNoteRequest("Content");

            assertThatThrownBy(() -> personalNoteService.updateNote(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("4. Delete Note Tests")
    class DeleteNoteTests {

        @Test
        @DisplayName("deleteNote: owner can delete note successfully")
        void testDeleteNote_success() {
            when(userProfileRepository.findByAccountEmailOrPhone("usera@test.com")).thenReturn(Optional.of(userA));

            PersonalNote note = new PersonalNote(userA, vocab1, "Content");
            note.setNoteId(10L);
            when(personalNoteRepository.findById(10L)).thenReturn(Optional.of(note));

            personalNoteService.deleteNote(10L);

            verify(personalNoteRepository).delete(note);
        }

        @Test
        @DisplayName("deleteNote: user B attempting to delete user A's note throws FORBIDDEN (403)")
        void testDeleteNote_unauthorized_throwsForbidden() {
            authenticateAs("userb@test.com");
            when(userProfileRepository.findByAccountEmailOrPhone("userb@test.com")).thenReturn(Optional.of(userB));

            PersonalNote noteOwnedByA = new PersonalNote(userA, vocab1, "User A content");
            noteOwnedByA.setNoteId(10L);
            when(personalNoteRepository.findById(10L)).thenReturn(Optional.of(noteOwnedByA));

            assertThatThrownBy(() -> personalNoteService.deleteNote(10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

            verify(personalNoteRepository, never()).delete(any());
        }
    }
}
