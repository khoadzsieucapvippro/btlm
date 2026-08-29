package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.controller.CreatorLessonController;
import com.elearning.dto.request.CreateLessonRequest;
import com.elearning.dto.request.ReorderVocabRequest;
import com.elearning.dto.request.UpdateLessonRequest;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.dto.response.LessonVocabItemResponse;
import com.elearning.exception.BusinessException;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.service.CreatorLessonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.elearning.dto.response.ImportValidationReport;
import com.elearning.service.ExcelParserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 5B.2 & 5C.2: CreatorLessonController Unit Contract Tests")
class CreatorLessonControllerTests {

    @Mock
    private CreatorLessonService creatorLessonService;

    @Mock
    private ExcelParserService excelParserService;

    @InjectMocks
    private CreatorLessonController creatorLessonController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private LessonDetailResponse sampleDetail;
    private LessonSummaryResponse sampleSummary;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(creatorLessonController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        LessonVocabItemResponse vocabItem = new LessonVocabItemResponse();
        vocabItem.setVocabId(10L);
        vocabItem.setHanzi("学");
        vocabItem.setPinyin("xué");
        vocabItem.setPinyinRaw("xue");
        vocabItem.setMeaningHanViet("Học");
        vocabItem.setMeaningVi("Học tập");
        vocabItem.setOrderIndex(1);

        sampleDetail = new LessonDetailResponse(
                100L,
                "Bài 1 - Giới thiệu",
                "Draft",
                1,
                List.of(vocabItem),
                LocalDateTime.of(2026, 8, 27, 20, 0),
                LocalDateTime.of(2026, 8, 27, 20, 0)
        );

        sampleSummary = new LessonSummaryResponse(
                100L,
                "Bài 1 - Giới thiệu",
                "Draft",
                1,
                LocalDateTime.of(2026, 8, 27, 20, 0),
                LocalDateTime.of(2026, 8, 27, 20, 0)
        );
    }

    @Nested
    @DisplayName("1. POST /api/v1/creator/lessons (Create Lesson)")
    class CreateLessonTests {

        @Test
        @DisplayName("GIVEN valid CreateLessonRequest WHEN POST /api/v1/creator/lessons THEN returns 201 Created")
        void testCreateLesson_success() throws Exception {
            when(creatorLessonService.createLesson(any(CreateLessonRequest.class))).thenReturn(sampleDetail);

            CreateLessonRequest request = new CreateLessonRequest("Bài 1 - Giới thiệu");

            mockMvc.perform(post("/api/v1/creator/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Tạo bài học thành công"))
                    .andExpect(jsonPath("$.data.lessonId").value(100))
                    .andExpect(jsonPath("$.data.title").value("Bài 1 - Giới thiệu"))
                    .andExpect(jsonPath("$.data.status").value("Draft"));
        }

        @Test
        @DisplayName("GIVEN invalid CreateLessonRequest with blank title WHEN POST THEN returns 400 Bad Request")
        void testCreateLesson_blankTitle() throws Exception {
            CreateLessonRequest request = new CreateLessonRequest("   ");

            mockMvc.perform(post("/api/v1/creator/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GIVEN CreateLessonRequest with null element in vocabularyIds WHEN POST THEN returns 400 Bad Request with VALIDATION_ERROR")
        void testCreateLesson_nullVocabIdInList() throws Exception {
            String jsonWithNullVocab = "{\"title\":\"Bài 1\",\"vocabularyIds\":[1, null, 3]}";

            mockMvc.perform(post("/api/v1/creator/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithNullVocab))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GIVEN CreateLessonRequest with negative vocabId in vocabularyIds WHEN POST THEN returns 400 Bad Request with VALIDATION_ERROR")
        void testCreateLesson_negativeVocabIdInList() throws Exception {
            String jsonWithNegative = "{\"title\":\"Bài 1\",\"vocabularyIds\":[1, -5]}";

            mockMvc.perform(post("/api/v1/creator/lessons")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithNegative))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("2. GET /api/v1/creator/lessons (List Creator Lessons)")
    class GetMyLessonsTests {

        @Test
        @DisplayName("GIVEN pagination parameters WHEN GET /api/v1/creator/lessons THEN returns 200 OK with PageResponse")
        void testGetMyLessons_success() throws Exception {
            Page<LessonSummaryResponse> page = new PageImpl<>(List.of(sampleSummary), PageRequest.of(0, 10), 1);
            when(creatorLessonService.getMyLessons(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/creator/lessons?page=0&size=10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.items[0].lessonId").value(100))
                    .andExpect(jsonPath("$.data.items[0].title").value("Bài 1 - Giới thiệu"));
        }
    }

    @Nested
    @DisplayName("3. GET /api/v1/creator/lessons/{id} (Detail by ID)")
    class GetMyLessonByIdTests {

        @Test
        @DisplayName("GIVEN existing lesson ID WHEN GET /api/v1/creator/lessons/{id} THEN returns 200 OK with detail")
        void testGetMyLessonById_success() throws Exception {
            when(creatorLessonService.getMyLessonById(100L)).thenReturn(sampleDetail);

            mockMvc.perform(get("/api/v1/creator/lessons/100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.lessonId").value(100))
                    .andExpect(jsonPath("$.data.title").value("Bài 1 - Giới thiệu"))
                    .andExpect(jsonPath("$.data.vocabularies").isArray());
        }

        @Test
        @DisplayName("GIVEN lesson belonging to another creator WHEN GET THEN propagates 403 FORBIDDEN")
        void testGetMyLessonById_forbidden() throws Exception {
            when(creatorLessonService.getMyLessonById(200L))
                    .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền thao tác trên bài học này"));

            mockMvc.perform(get("/api/v1/creator/lessons/200"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.message").value("Bạn không có quyền thao tác trên bài học này"));
        }

        @Test
        @DisplayName("GIVEN nonexistent lesson ID WHEN GET THEN propagates 404 NOT_FOUND")
        void testGetMyLessonById_notFound() throws Exception {
            when(creatorLessonService.getMyLessonById(999L))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy bài học với ID: 999"));

            mockMvc.perform(get("/api/v1/creator/lessons/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy bài học với ID: 999"));
        }
    }

    @Nested
    @DisplayName("4. PUT /api/v1/creator/lessons/{id} (Update Lesson)")
    class UpdateLessonTests {

        @Test
        @DisplayName("GIVEN valid UpdateLessonRequest WHEN PUT THEN returns 200 OK with updated detail")
        void testUpdateMyLesson_success() throws Exception {
            when(creatorLessonService.updateMyLesson(eq(100L), any(UpdateLessonRequest.class))).thenReturn(sampleDetail);

            UpdateLessonRequest request = new UpdateLessonRequest("Bài 1 - Tiêu đề mới");

            mockMvc.perform(put("/api/v1/creator/lessons/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Cập nhật bài học thành công"));
        }

        @Test
        @DisplayName("GIVEN lesson in Pending state WHEN PUT THEN propagates 409 CONFLICT")
        void testUpdateMyLesson_conflict() throws Exception {
            when(creatorLessonService.updateMyLesson(eq(100L), any(UpdateLessonRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "Bài học đang trong hàng đợi kiểm duyệt, không thể chỉnh sửa"));

            UpdateLessonRequest request = new UpdateLessonRequest("Hack");

            mockMvc.perform(put("/api/v1/creator/lessons/100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        }
    }

    @Nested
    @DisplayName("5. DELETE /api/v1/creator/lessons/{id} (Delete Lesson)")
    class DeleteLessonTests {

        @Test
        @DisplayName("GIVEN valid ID WHEN DELETE THEN returns 204 No Content")
        void testDeleteMyLesson_success() throws Exception {
            doNothing().when(creatorLessonService).deleteMyLesson(100L);

            mockMvc.perform(delete("/api/v1/creator/lessons/100"))
                    .andExpect(status().isNoContent());

            verify(creatorLessonService).deleteMyLesson(100L);
        }

        @Test
        @DisplayName("GIVEN lesson belonging to another creator WHEN DELETE THEN propagates 403 FORBIDDEN")
        void testDeleteMyLesson_forbidden() throws Exception {
            doThrow(new BusinessException(ErrorCode.FORBIDDEN, "Bạn không có quyền thao tác trên bài học này"))
                    .when(creatorLessonService).deleteMyLesson(200L);

            mockMvc.perform(delete("/api/v1/creator/lessons/200"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("6. POST & DELETE Vocabulary associations")
    class VocabularyAssociationTests {

        @Test
        @DisplayName("GIVEN valid IDs WHEN POST .../vocabularies/{vocabId} THEN returns 200 OK")
        void testAddVocabularyToLesson_success() throws Exception {
            when(creatorLessonService.addVocabularyToLesson(100L, 10L)).thenReturn(sampleDetail);

            mockMvc.perform(post("/api/v1/creator/lessons/100/vocabularies/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Thêm từ vựng vào bài học thành công"));
        }

        @Test
        @DisplayName("GIVEN valid IDs WHEN DELETE .../vocabularies/{vocabId} THEN returns 200 OK")
        void testRemoveVocabularyFromLesson_success() throws Exception {
            when(creatorLessonService.removeVocabularyFromLesson(100L, 10L)).thenReturn(sampleDetail);

            mockMvc.perform(delete("/api/v1/creator/lessons/100/vocabularies/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Xóa từ vựng khỏi bài học thành công"));
        }
    }

    @Nested
    @DisplayName("7. Reorder Vocabulary Tests (PUT & POST /{id}/reorder)")
    class ReorderVocabularyTests {

        @Test
        @DisplayName("GIVEN valid ReorderVocabRequest WHEN PUT /api/v1/creator/lessons/{id}/reorder THEN returns 200 OK")
        void testReorderVocabulary_putSuccess() throws Exception {
            when(creatorLessonService.reorderVocabulary(eq(100L), any(ReorderVocabRequest.class))).thenReturn(sampleDetail);

            ReorderVocabRequest request = ReorderVocabRequest.of(List.of(20L, 10L));

            mockMvc.perform(put("/api/v1/creator/lessons/100/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Cập nhật thứ tự từ vựng thành công"));
        }

        @Test
        @DisplayName("GIVEN valid ReorderVocabRequest WHEN POST /api/v1/creator/lessons/{id}/reorder THEN also returns 200 OK")
        void testReorderVocabulary_postSuccess() throws Exception {
            when(creatorLessonService.reorderVocabulary(eq(100L), any(ReorderVocabRequest.class))).thenReturn(sampleDetail);

            ReorderVocabRequest request = ReorderVocabRequest.of(List.of(20L, 10L));

            mockMvc.perform(post("/api/v1/creator/lessons/100/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Cập nhật thứ tự từ vựng thành công"));
        }

        @Test
        @DisplayName("GIVEN ReorderVocabRequest with null element in orderedVocabIds WHEN PUT THEN returns 400 VALIDATION_ERROR")
        void testReorderVocabulary_nullElementInOrderedIds() throws Exception {
            String json = "{\"orderedVocabIds\":[10, null, 20]}";

            mockMvc.perform(put("/api/v1/creator/lessons/100/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GIVEN ReorderVocabRequest with negative orderIndex in items WHEN PUT THEN returns 400 VALIDATION_ERROR")
        void testReorderVocabulary_negativeOrderIndex() throws Exception {
            String json = "{\"items\":[{\"vocabId\":10, \"orderIndex\":-1}]}";

            mockMvc.perform(put("/api/v1/creator/lessons/100/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("8. POST /api/v1/creator/lessons/{id}/submit (Submit for Moderation)")
    class SubmitForModerationTests {

        @Test
        @DisplayName("GIVEN valid lesson ID WHEN POST /api/v1/creator/lessons/{id}/submit THEN returns 200 OK")
        void testSubmitForModeration_success() throws Exception {
            LessonDetailResponse pendingResponse = new LessonDetailResponse(
                    100L,
                    "Bài 1 - Giới thiệu",
                    "Pending",
                    1,
                    sampleDetail.getVocabularies(),
                    sampleDetail.getCreatedAt(),
                    sampleDetail.getUpdatedAt()
            );
            when(creatorLessonService.submitForModeration(100L)).thenReturn(pendingResponse);

            mockMvc.perform(post("/api/v1/creator/lessons/100/submit"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Nộp bài học kiểm duyệt thành công"))
                    .andExpect(jsonPath("$.data.status").value("Pending"));
        }

        @Test
        @DisplayName("GIVEN already pending lesson WHEN POST submit THEN propagates 409 CONFLICT")
        void testSubmitForModeration_conflict() throws Exception {
            when(creatorLessonService.submitForModeration(100L))
                    .thenThrow(new BusinessException(ErrorCode.CONFLICT, "Bài học đã được nộp và đang chờ kiểm duyệt"));

            mockMvc.perform(post("/api/v1/creator/lessons/100/submit"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"))
                    .andExpect(jsonPath("$.message").value("Bài học đã được nộp và đang chờ kiểm duyệt"));
        }
    }

    @Nested
    @DisplayName("Excel Import Endpoints Tests (Module 5C)")
    class ExcelImportEndpointsTests {

        @Test
        @DisplayName("GIVEN valid file WHEN POST /api/v1/creator/lessons/import THEN returns 200 with ImportValidationReport")
        void testImportPreview_success() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "vocab.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new byte[]{1, 2, 3}
            );

            ImportValidationReport report = new ImportValidationReport();
            report.setIsValid(true);
            report.setTotalRows(5);
            report.setValidRowsCount(5);
            report.setSummaryMessage("Kiểm tra thành công: 5 dòng hợp lệ");

            when(excelParserService.parseAndValidate(any())).thenReturn(report);

            mockMvc.perform(multipart("/api/v1/creator/lessons/import").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.isValid").value(true))
                    .andExpect(jsonPath("$.data.totalRows").value(5))
                    .andExpect(jsonPath("$.data.validRowsCount").value(5));
        }

        @Test
        @DisplayName("GIVEN missing file WHEN POST /api/v1/creator/lessons/import THEN returns 400 Bad Request")
        void testImportPreview_missingFile() throws Exception {
            mockMvc.perform(multipart("/api/v1/creator/lessons/import"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GIVEN valid file and title WHEN POST /api/v1/creator/lessons/import/confirm THEN returns 201 Created with LessonDetailResponse")
        void testImportConfirm_success() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "vocab.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new byte[]{1, 2, 3}
            );

            when(creatorLessonService.importLessonFromExcel(eq("Bài học mới"), any())).thenReturn(sampleDetail);

            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài học mới"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.message").value("Import bài học từ Excel thành công"))
                    .andExpect(jsonPath("$.data.lessonId").value(100L));
        }

        @Test
        @DisplayName("GIVEN missing title WHEN POST /api/v1/creator/lessons/import/confirm THEN returns 400 Bad Request")
        void testImportConfirm_missingTitle() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "vocab.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GIVEN missing file WHEN POST /api/v1/creator/lessons/import/confirm THEN returns 400 Bad Request")
        void testImportConfirm_missingFile() throws Exception {
            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .param("title", "Bài học mới"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("GIVEN unprocessable file WHEN POST /api/v1/creator/lessons/import/confirm THEN propagates 422 UNPROCESSABLE_ENTITY")
        void testImportConfirm_unprocessableEntity() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "corrupt.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new byte[]{1, 2, 3}
            );

            when(creatorLessonService.importLessonFromExcel(eq("Bài lỗi"), any()))
                    .thenThrow(new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "File chứa lỗi"));

            mockMvc.perform(multipart("/api/v1/creator/lessons/import/confirm")
                            .file(file)
                            .param("title", "Bài lỗi"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"))
                    .andExpect(jsonPath("$.message").value("File chứa lỗi"));
        }
    }
}
