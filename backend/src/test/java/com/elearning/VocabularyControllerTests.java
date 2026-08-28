package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.controller.AdminVocabularyController;
import com.elearning.controller.VocabularyController;
import com.elearning.dto.request.CreateVocabularyRequest;
import com.elearning.dto.request.UpdateVocabularyRequest;
import com.elearning.dto.request.VocabularySearchCriteria;
import com.elearning.dto.response.VocabularyDetailResponse;
import com.elearning.dto.response.VocabularyResponse;
import com.elearning.exception.BusinessException;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.service.VocabularyService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 4B.2: VocabularyController & AdminVocabularyController Unit Contract Tests")
class VocabularyControllerTests {

    @Mock
    private VocabularyService vocabularyService;

    @InjectMocks
    private VocabularyController vocabularyController;

    @InjectMocks
    private AdminVocabularyController adminVocabularyController;

    private MockMvc publicMockMvc;
    private MockMvc adminMockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        publicMockMvc = MockMvcBuilders.standaloneSetup(vocabularyController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminMockMvc = MockMvcBuilders.standaloneSetup(adminVocabularyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("1. Public VocabularyController Contract Tests (/api/v1/vocabulary)")
    class PublicVocabularyControllerTests {

        @Test
        @DisplayName("GIVEN GET /api/v1/vocabulary without search WHEN requested THEN returns 200 OK with PageResponse")
        void testGetVocabulariesWithoutSearch() throws Exception {
            VocabularyResponse item = new VocabularyResponse(1L, "学", "xué", "xue", "Học", "Học tập");
            Page<VocabularyResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);

            when(vocabularyService.searchVocabularies(any(VocabularySearchCriteria.class), any(Pageable.class)))
                    .thenReturn(page);

            publicMockMvc.perform(get("/api/v1/vocabulary?page=0&size=20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items[0].vocabId").value(1))
                    .andExpect(jsonPath("$.data.items[0].hanzi").value("学"))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(20));
        }

        @Test
        @DisplayName("GIVEN GET /api/v1/vocabulary?search=xue WHEN requested THEN passes search keyword to criteria and returns 200 OK")
        void testGetVocabulariesWithSearch() throws Exception {
            VocabularyResponse item = new VocabularyResponse(1L, "学", "xué", "xue", "Học", "Học tập");
            Page<VocabularyResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);

            when(vocabularyService.searchVocabularies(any(VocabularySearchCriteria.class), any(Pageable.class)))
                    .thenReturn(page);

            publicMockMvc.perform(get("/api/v1/vocabulary?search=xue&page=0&size=10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.items[0].hanzi").value("学"));
        }

        @Test
        @DisplayName("GIVEN GET /api/v1/vocabulary/{id} with existing ID WHEN requested THEN returns 200 OK with DetailResponse")
        void testGetVocabularyByIdSuccess() throws Exception {
            VocabularyDetailResponse detail = new VocabularyDetailResponse(1L, "学", "xué", "xue", "Học", "Học tập");
            when(vocabularyService.getVocabularyById(1L)).thenReturn(detail);

            publicMockMvc.perform(get("/api/v1/vocabulary/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.vocabId").value(1))
                    .andExpect(jsonPath("$.data.hanzi").value("学"))
                    .andExpect(jsonPath("$.data.radicals").isArray());
        }

        @Test
        @DisplayName("GIVEN GET /api/v1/vocabulary/{id} with nonexistent ID WHEN requested THEN returns 404 NOT_FOUND")
        void testGetVocabularyByIdNotFound() throws Exception {
            when(vocabularyService.getVocabularyById(999L))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng với ID: 999"));

            publicMockMvc.perform(get("/api/v1/vocabulary/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Không tìm thấy từ vựng")));
        }
    }

    @Nested
    @DisplayName("2. AdminVocabularyController Contract Tests (/api/v1/admin/vocabulary)")
    class AdminVocabularyControllerTests {

        @Test
        @DisplayName("GIVEN valid CreateVocabularyRequest WHEN POST /api/v1/admin/vocabulary THEN returns 201 Created")
        void testCreateVocabularySuccess() throws Exception {
            CreateVocabularyRequest request = new CreateVocabularyRequest("休", "xiū", "xiu", "Hưu", "Nghỉ ngơi");
            request.setRadicalIds(Set.of(9, 75));

            VocabularyDetailResponse detail = new VocabularyDetailResponse(10L, "休", "xiū", "xiu", "Hưu", "Nghỉ ngơi");
            when(vocabularyService.createVocabulary(any(CreateVocabularyRequest.class))).thenReturn(detail);

            adminMockMvc.perform(post("/api/v1/admin/vocabulary")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Tạo từ vựng thành công"))
                    .andExpect(jsonPath("$.data.vocabId").value(10))
                    .andExpect(jsonPath("$.data.hanzi").value("休"));
        }

        @Test
        @DisplayName("GIVEN invalid CreateVocabularyRequest WHEN POST /api/v1/admin/vocabulary THEN returns 400 Bad Request")
        void testCreateVocabularyValidationFailure() throws Exception {
            CreateVocabularyRequest request = new CreateVocabularyRequest("", "", "", "", "");

            adminMockMvc.perform(post("/api/v1/admin/vocabulary")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GIVEN CreateVocabularyRequest with null element in radicalIds WHEN POST THEN returns 400 Bad Request")
        void testCreateVocabulary_nullRadicalId() throws Exception {
            String json = "{\"hanzi\":\"休\",\"pinyin\":\"xiu\",\"pinyinRaw\":\"xiu\",\"meaningHanViet\":\"Hưu\",\"meaningVi\":\"Nghỉ\",\"radicalIds\":[9, null]}";

            adminMockMvc.perform(post("/api/v1/admin/vocabulary")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GIVEN CreateVocabularyRequest with negative radicalId WHEN POST THEN returns 400 Bad Request")
        void testCreateVocabulary_negativeRadicalId() throws Exception {
            String json = "{\"hanzi\":\"休\",\"pinyin\":\"xiu\",\"pinyinRaw\":\"xiu\",\"meaningHanViet\":\"Hưu\",\"meaningVi\":\"Nghỉ\",\"radicalIds\":[-1]}";

            adminMockMvc.perform(post("/api/v1/admin/vocabulary")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GIVEN duplicate CreateVocabularyRequest WHEN POST /api/v1/admin/vocabulary THEN returns 409 Conflict")
        void testCreateVocabularyConflict() throws Exception {
            CreateVocabularyRequest request = new CreateVocabularyRequest("学", "xué", "xue", "Học", "Học tập");
            when(vocabularyService.createVocabulary(any(CreateVocabularyRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "Từ vựng đã tồn tại"));

            adminMockMvc.perform(post("/api/v1/admin/vocabulary")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        }

        @Test
        @DisplayName("GIVEN valid UpdateVocabularyRequest WHEN PUT /api/v1/admin/vocabulary/{id} THEN returns 200 OK")
        void testUpdateVocabularySuccess() throws Exception {
            UpdateVocabularyRequest request = new UpdateVocabularyRequest("学", "xué", "xue", "Học", "Học tập, nghiên cứu");
            VocabularyDetailResponse detail = new VocabularyDetailResponse(1L, "学", "xué", "xue", "Học", "Học tập, nghiên cứu");

            when(vocabularyService.updateVocabulary(eq(1L), any(UpdateVocabularyRequest.class))).thenReturn(detail);

            adminMockMvc.perform(put("/api/v1/admin/vocabulary/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Cập nhật từ vựng thành công"))
                    .andExpect(jsonPath("$.data.meaningVi").value("Học tập, nghiên cứu"));
        }

        @Test
        @DisplayName("GIVEN nonexistent ID WHEN PUT /api/v1/admin/vocabulary/{id} THEN returns 404 NOT_FOUND")
        void testUpdateVocabularyNotFound() throws Exception {
            UpdateVocabularyRequest request = new UpdateVocabularyRequest("学", "xué", "xue", "Học", "Học");
            when(vocabularyService.updateVocabulary(eq(999L), any(UpdateVocabularyRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng"));

            adminMockMvc.perform(put("/api/v1/admin/vocabulary/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("GIVEN existing ID WHEN DELETE /api/v1/admin/vocabulary/{id} THEN returns 204 No Content")
        void testDeleteVocabularySuccess() throws Exception {
            doNothing().when(vocabularyService).deleteVocabulary(1L);

            adminMockMvc.perform(delete("/api/v1/admin/vocabulary/1"))
                    .andExpect(status().isNoContent());

            verify(vocabularyService).deleteVocabulary(1L);
        }

        @Test
        @DisplayName("GIVEN nonexistent ID WHEN DELETE /api/v1/admin/vocabulary/{id} THEN returns 404 NOT_FOUND")
        void testDeleteVocabularyNotFound() throws Exception {
            doThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng"))
                    .when(vocabularyService).deleteVocabulary(999L);

            adminMockMvc.perform(delete("/api/v1/admin/vocabulary/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("GIVEN ID referenced by lesson WHEN DELETE /api/v1/admin/vocabulary/{id} THEN returns 409 Conflict")
        void testDeleteVocabularyConflict() throws Exception {
            doThrow(new BusinessException(ErrorCode.CONFLICT, "Không thể xóa từ vựng đang được liên kết"))
                    .when(vocabularyService).deleteVocabulary(1L);

            adminMockMvc.perform(delete("/api/v1/admin/vocabulary/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        }
    }
}
