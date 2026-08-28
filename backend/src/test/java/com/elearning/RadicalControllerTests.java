package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.controller.AdminRadicalController;
import com.elearning.controller.RadicalController;
import com.elearning.dto.request.CreateRadicalRequest;
import com.elearning.dto.request.UpdateRadicalRequest;
import com.elearning.dto.response.RadicalDetailResponse;
import com.elearning.dto.response.RadicalResponse;
import com.elearning.exception.BusinessException;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.service.RadicalService;
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

import java.time.LocalDateTime;
import java.util.List;

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
@DisplayName("Task 4A.2: RadicalController & AdminRadicalController Unit Contract Tests")
class RadicalControllerTests {

    @Mock
    private RadicalService radicalService;

    @InjectMocks
    private RadicalController radicalController;

    @InjectMocks
    private AdminRadicalController adminRadicalController;

    private MockMvc publicMockMvc;
    private MockMvc adminMockMvc;
    private ObjectMapper objectMapper;

    private RadicalResponse sampleSummary;
    private RadicalDetailResponse sampleDetail;

    @BeforeEach
    void setUp() {
        publicMockMvc = MockMvcBuilders.standaloneSetup(radicalController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminMockMvc = MockMvcBuilders.standaloneSetup(adminRadicalController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        sampleSummary = new RadicalResponse(1, "一", "yī", "Nhất", "Một",
                "http://audio.mp3", "http://video.mp4");

        sampleDetail = new RadicalDetailResponse(1, "一", "yī", "Nhất", "Một",
                "http://audio.mp3", "http://video.mp4",
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 2, 12, 0));
    }

    @Nested
    @DisplayName("1. Public GET /api/v1/radicals (List & Pagination)")
    class PublicListTests {

        @Test
        @DisplayName("GIVEN valid request WHEN GET /api/v1/radicals THEN returns 200 OK with PageResponse")
        void testGetAllRadicalsPaged() throws Exception {
            Page<RadicalResponse> page = new PageImpl<>(List.of(sampleSummary), PageRequest.of(0, 20), 1);
            when(radicalService.getAllRadicals(any(Pageable.class))).thenReturn(page);

            publicMockMvc.perform(get("/api/v1/radicals?page=0&size=20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.items[0].radicalId").value(1))
                    .andExpect(jsonPath("$.data.items[0].character").value("一"))
                    .andExpect(jsonPath("$.data.items[0].pinyin").value("yī"))
                    .andExpect(jsonPath("$.data.items[0].meaningHanViet").value("Nhất"))
                    .andExpect(jsonPath("$.data.items[0].meaningVi").value("Một"));
        }
    }

    @Nested
    @DisplayName("2. Public GET /api/v1/radicals/{id} (Detail by ID)")
    class PublicDetailTests {

        @Test
        @DisplayName("GIVEN existing radical ID WHEN GET /api/v1/radicals/1 THEN returns 200 OK with detail")
        void testGetRadicalByIdFound() throws Exception {
            when(radicalService.getRadicalById(1)).thenReturn(sampleDetail);

            publicMockMvc.perform(get("/api/v1/radicals/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.radicalId").value(1))
                    .andExpect(jsonPath("$.data.character").value("一"))
                    .andExpect(jsonPath("$.data.pinyin").value("yī"))
                    .andExpect(jsonPath("$.data.meaningHanViet").value("Nhất"))
                    .andExpect(jsonPath("$.data.meaningVi").value("Một"))
                    .andExpect(jsonPath("$.data.audioUrl").value("http://audio.mp3"))
                    .andExpect(jsonPath("$.data.videoWritingUrl").value("http://video.mp4"));
        }

        @Test
        @DisplayName("GIVEN nonexistent radical ID WHEN GET /api/v1/radicals/999 THEN returns 404 NOT_FOUND")
        void testGetRadicalByIdNotFound() throws Exception {
            when(radicalService.getRadicalById(999))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bộ thủ với ID: 999"));

            publicMockMvc.perform(get("/api/v1/radicals/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy bộ thủ với ID: 999"));
        }
    }

    @Nested
    @DisplayName("3. Admin POST /api/v1/admin/radicals (Create)")
    class AdminCreateTests {

        @Test
        @DisplayName("GIVEN valid CreateRadicalRequest WHEN POST /api/v1/admin/radicals THEN returns 201 CREATED")
        void testCreateRadicalSuccess() throws Exception {
            CreateRadicalRequest request = new CreateRadicalRequest(
                    "口", "kǒu", "Khẩu", "Miệng", "http://audio.mp3", "http://video.mp4"
            );
            RadicalDetailResponse created = new RadicalDetailResponse(30, "口", "kǒu", "Khẩu", "Miệng",
                    "http://audio.mp3", "http://video.mp4", LocalDateTime.now(), LocalDateTime.now());

            when(radicalService.createRadical(any(CreateRadicalRequest.class))).thenReturn(created);

            adminMockMvc.perform(post("/api/v1/admin/radicals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Tạo bộ thủ thành công"))
                    .andExpect(jsonPath("$.data.radicalId").value(30))
                    .andExpect(jsonPath("$.data.character").value("口"));
        }

        @Test
        @DisplayName("GIVEN blank required fields WHEN POST /api/v1/admin/radicals THEN returns 400 VALIDATION_ERROR")
        void testCreateRadicalValidationError() throws Exception {
            CreateRadicalRequest invalidRequest = new CreateRadicalRequest(
                    "", "", "", "", null, null
            );

            adminMockMvc.perform(post("/api/v1/admin/radicals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("GIVEN duplicate character WHEN POST /api/v1/admin/radicals THEN returns 409 CONFLICT")
        void testCreateRadicalDuplicate() throws Exception {
            CreateRadicalRequest request = new CreateRadicalRequest(
                    "一", "yī", "Nhất", "Một", null, null
            );

            when(radicalService.createRadical(any(CreateRadicalRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "Bộ thủ với ký tự '一' đã tồn tại"));

            adminMockMvc.perform(post("/api/v1/admin/radicals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.message").value("Bộ thủ với ký tự '一' đã tồn tại"));
        }
    }

    @Nested
    @DisplayName("4. Admin PUT /api/v1/admin/radicals/{id} (Update)")
    class AdminUpdateTests {

        @Test
        @DisplayName("GIVEN valid UpdateRadicalRequest WHEN PUT /api/v1/admin/radicals/1 THEN returns 200 OK")
        void testUpdateRadicalSuccess() throws Exception {
            UpdateRadicalRequest request = new UpdateRadicalRequest(
                    "一", "yī", "Nhất", "Số một, đứng đầu", "http://new-audio.mp3", null
            );
            RadicalDetailResponse updated = new RadicalDetailResponse(1, "一", "yī", "Nhất", "Số một, đứng đầu",
                    "http://new-audio.mp3", null, LocalDateTime.now(), LocalDateTime.now());

            when(radicalService.updateRadical(eq(1), any(UpdateRadicalRequest.class))).thenReturn(updated);

            adminMockMvc.perform(put("/api/v1/admin/radicals/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Cập nhật bộ thủ thành công"))
                    .andExpect(jsonPath("$.data.meaningVi").value("Số một, đứng đầu"));
        }

        @Test
        @DisplayName("GIVEN nonexistent ID WHEN PUT /api/v1/admin/radicals/999 THEN returns 404 NOT_FOUND")
        void testUpdateRadicalNotFound() throws Exception {
            UpdateRadicalRequest request = new UpdateRadicalRequest(
                    "一", "yī", "Nhất", "Số một", null, null
            );

            when(radicalService.updateRadical(eq(999), any(UpdateRadicalRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bộ thủ với ID: 999"));

            adminMockMvc.perform(put("/api/v1/admin/radicals/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("GIVEN duplicate character conflict WHEN PUT /api/v1/admin/radicals/1 THEN returns 409 CONFLICT")
        void testUpdateRadicalConflict() throws Exception {
            UpdateRadicalRequest request = new UpdateRadicalRequest(
                    "丨", "gǔn", "Cổn", "Nét sổ", null, null
            );

            when(radicalService.updateRadical(eq(1), any(UpdateRadicalRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "Bộ thủ với ký tự '丨' đã tồn tại"));

            adminMockMvc.perform(put("/api/v1/admin/radicals/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        }
    }

    @Nested
    @DisplayName("5. Admin DELETE /api/v1/admin/radicals/{id} (Delete)")
    class AdminDeleteTests {

        @Test
        @DisplayName("GIVEN existing radical ID WHEN DELETE /api/v1/admin/radicals/1 THEN returns 204 NO_CONTENT")
        void testDeleteRadicalSuccess() throws Exception {
            doNothing().when(radicalService).deleteRadical(1);

            adminMockMvc.perform(delete("/api/v1/admin/radicals/1"))
                    .andExpect(status().isNoContent());

            verify(radicalService).deleteRadical(1);
        }

        @Test
        @DisplayName("GIVEN nonexistent ID WHEN DELETE /api/v1/admin/radicals/999 THEN returns 404 NOT_FOUND")
        void testDeleteRadicalNotFound() throws Exception {
            doThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bộ thủ với ID: 999"))
                    .when(radicalService).deleteRadical(999);

            adminMockMvc.perform(delete("/api/v1/admin/radicals/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("GIVEN linked radical WHEN DELETE /api/v1/admin/radicals/1 THEN returns 409 CONFLICT")
        void testDeleteRadicalLinkedConflict() throws Exception {
            doThrow(new BusinessException(ErrorCode.CONFLICT, "Không thể xóa bộ thủ đang được liên kết với từ vựng"))
                    .when(radicalService).deleteRadical(1);

            adminMockMvc.perform(delete("/api/v1/admin/radicals/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        }
    }
}
