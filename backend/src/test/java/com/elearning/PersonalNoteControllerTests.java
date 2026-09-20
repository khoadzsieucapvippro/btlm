package com.elearning;

import com.elearning.controller.PersonalNoteController;
import com.elearning.dto.request.PersonalNoteRequest;
import com.elearning.dto.response.PageResponse;
import com.elearning.dto.response.PersonalNoteResponse;
import com.elearning.exception.BusinessException;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.common.ErrorCode;
import com.elearning.service.PersonalNoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 8A.2: PersonalNoteController Unit Tests (MockMvc & Delegation)")
class PersonalNoteControllerTests {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PersonalNoteService personalNoteService;

    @InjectMocks
    private PersonalNoteController personalNoteController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(personalNoteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("1. GET /api/v1/vocabularies/{vocabId}/notes")
    class GetNotesEndpoints {

        @Test
        @DisplayName("GET notes: delegates to personalNoteService.getNotesByVocabulary and wraps in ApiResponse")
        void testGetNotes_delegatesCorrectly() throws Exception {
            PersonalNoteResponse n1 = new PersonalNoteResponse(1L, 100L, "Note 1", LocalDateTime.now());
            PersonalNoteResponse n2 = new PersonalNoteResponse(2L, 100L, "Note 2", LocalDateTime.now());
            PageResponse<PersonalNoteResponse> pageResponse = new PageResponse<>(0, 20, 2, 1, List.of(n1, n2));
            when(personalNoteService.getNotesByVocabulary(eq(100L), any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/v1/vocabularies/100/notes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.items", org.hamcrest.Matchers.hasSize(2)))
                    .andExpect(jsonPath("$.data.items[0].noteId", is(1)))
                    .andExpect(jsonPath("$.data.items[0].content", is("Note 1")));

            verify(personalNoteService).getNotesByVocabulary(eq(100L), any(Pageable.class));
        }

        @Test
        @DisplayName("GET notes: nonexistent vocabulary returns 404 NOT_FOUND")
        void testGetNotes_nonexistentVocab_returns404() throws Exception {
            when(personalNoteService.getNotesByVocabulary(eq(999L), any(Pageable.class)))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng với ID: 999"));

            mockMvc.perform(get("/api/v1/vocabularies/999/notes"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.message", is("Không tìm thấy từ vựng với ID: 999")));
        }
    }

    @Nested
    @DisplayName("2. POST /api/v1/vocabularies/{vocabId}/notes")
    class CreateNoteEndpoints {

        @Test
        @DisplayName("POST note: valid request delegates to createNote and returns 201 Created")
        void testCreateNote_valid_returns201() throws Exception {
            PersonalNoteRequest request = new PersonalNoteRequest("Học từ vựng mới: 汉语");
            PersonalNoteResponse response = new PersonalNoteResponse(5L, 100L, "Học từ vựng mới: 汉语", LocalDateTime.now());
            when(personalNoteService.createNote(eq(100L), any(PersonalNoteRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/vocabularies/100/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.noteId", is(5)))
                    .andExpect(jsonPath("$.data.content", is("Học từ vựng mới: 汉语")));

            verify(personalNoteService).createNote(eq(100L), any(PersonalNoteRequest.class));
        }

        @Test
        @DisplayName("POST note: 501 chars fails @Valid and returns 400 VALIDATION_ERROR without calling service")
        void testCreateNote_501Chars_returns400_noServiceCall() throws Exception {
            PersonalNoteRequest request = new PersonalNoteRequest("A".repeat(501));

            mockMvc.perform(post("/api/v1/vocabularies/100/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            verify(personalNoteService, never()).createNote(any(), any());
        }

        @Test
        @DisplayName("POST note: null content fails @Valid and returns 400 VALIDATION_ERROR without calling service")
        void testCreateNote_nullContent_returns400_noServiceCall() throws Exception {
            PersonalNoteRequest request = new PersonalNoteRequest(null);

            mockMvc.perform(post("/api/v1/vocabularies/100/notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            verify(personalNoteService, never()).createNote(any(), any());
        }
    }

    @Nested
    @DisplayName("3. PUT /api/v1/notes/{noteId}")
    class UpdateNoteEndpoints {

        @Test
        @DisplayName("PUT note: owner update delegates to updateNote and returns 200 OK")
        void testUpdateNote_valid_returns200() throws Exception {
            PersonalNoteRequest request = new PersonalNoteRequest("Nội dung đã cập nhật");
            PersonalNoteResponse response = new PersonalNoteResponse(10L, 100L, "Nội dung đã cập nhật", LocalDateTime.now());
            when(personalNoteService.updateNote(eq(10L), any(PersonalNoteRequest.class))).thenReturn(response);

            mockMvc.perform(put("/api/v1/notes/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.content", is("Nội dung đã cập nhật")));

            verify(personalNoteService).updateNote(eq(10L), any(PersonalNoteRequest.class));
        }

        @Test
        @DisplayName("PUT note: other user update throws FORBIDDEN and returns 403 Forbidden")
        void testUpdateNote_unauthorized_returns403() throws Exception {
            PersonalNoteRequest request = new PersonalNoteRequest("Nội dung trái phép");
            when(personalNoteService.updateNote(eq(10L), any(PersonalNoteRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền chỉnh sửa ghi chú này"));

            mockMvc.perform(put("/api/v1/notes/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                    .andExpect(jsonPath("$.message", is("Bạn không có quyền chỉnh sửa ghi chú này")));
        }

        @Test
        @DisplayName("PUT note: nonexistent noteId returns 404 NOT_FOUND")
        void testUpdateNote_nonexistent_returns404() throws Exception {
            PersonalNoteRequest request = new PersonalNoteRequest("Nội dung");
            when(personalNoteService.updateNote(eq(999L), any(PersonalNoteRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy ghi chú với ID: 999"));

            mockMvc.perform(put("/api/v1/notes/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        }
    }

    @Nested
    @DisplayName("4. DELETE /api/v1/notes/{noteId}")
    class DeleteNoteEndpoints {

        @Test
        @DisplayName("DELETE note: owner delete delegates to deleteNote and returns 200 OK")
        void testDeleteNote_valid_returns200() throws Exception {
            mockMvc.perform(delete("/api/v1/notes/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.message", is("Xóa ghi chú thành công")));

            verify(personalNoteService).deleteNote(10L);
        }

        @Test
        @DisplayName("DELETE note: other user delete throws FORBIDDEN and returns 403 Forbidden")
        void testDeleteNote_unauthorized_returns403() throws Exception {
            org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền xóa ghi chú này"))
                    .when(personalNoteService).deleteNote(10L);

            mockMvc.perform(delete("/api/v1/notes/10"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                    .andExpect(jsonPath("$.message", is("Bạn không có quyền xóa ghi chú này")));
        }

        @Test
        @DisplayName("DELETE note: nonexistent noteId returns 404 NOT_FOUND")
        void testDeleteNote_nonexistent_returns404() throws Exception {
            org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy ghi chú với ID: 999"))
                    .when(personalNoteService).deleteNote(999L);

            mockMvc.perform(delete("/api/v1/notes/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")));
        }
    }
}
