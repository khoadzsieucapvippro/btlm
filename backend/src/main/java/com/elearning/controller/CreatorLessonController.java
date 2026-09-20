package com.elearning.controller;

import com.elearning.dto.request.CreateLessonRequest;
import com.elearning.dto.request.ReorderVocabRequest;
import com.elearning.dto.request.UpdateLessonRequest;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.ImportValidationReport;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.LessonSummaryResponse;
import com.elearning.dto.response.PageResponse;
import com.elearning.service.CreatorLessonService;
import com.elearning.service.ExcelParserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for Creator Lesson Studio (Module 5B & 5C).
 * Base path: /api/v1/creator/lessons
 * Protected by Spring Security: Requires ROLE_CREATOR or ROLE_ADMIN.
 * Enforces strict thin-controller architecture; delegates all ownership,
 * validation, and state machine transitions to {@link CreatorLessonService}
 * and Excel parsing to {@link ExcelParserService}.
 */
@RestController
@RequestMapping("/api/v1/creator/lessons")
public class CreatorLessonController {

    private final CreatorLessonService creatorLessonService;
    private final ExcelParserService excelParserService;

    public CreatorLessonController(CreatorLessonService creatorLessonService,
                                   ExcelParserService excelParserService) {
        this.creatorLessonService = creatorLessonService;
        this.excelParserService = excelParserService;
    }

    /**
     * Creates a new lesson in 'Draft' status owned by the current authenticated creator.
     *
     * @param request lesson creation parameters
     * @return 201 Created with LessonDetailResponse
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LessonDetailResponse>> createLesson(
            @Valid @RequestBody CreateLessonRequest request) {
        LessonDetailResponse response = creatorLessonService.createLesson(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo bài học thành công", response));
    }

    /**
     * Retrieves a paginated list of lessons authored by the current creator.
     *
     * @param pageable pagination parameters (page, size)
     * @return 200 OK with PageResponse of LessonSummaryResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LessonSummaryResponse>>> getMyLessons(Pageable pageable) {
        Page<LessonSummaryResponse> page = creatorLessonService.getMyLessons(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    /**
     * Retrieves full details of a lesson owned by the current creator,
     * including its constituent vocabulary items in display order.
     *
     * @param id lesson primary ID
     * @return 200 OK with LessonDetailResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LessonDetailResponse>> getMyLessonById(@PathVariable("id") Long id) {
        LessonDetailResponse response = creatorLessonService.getMyLessonById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates an existing lesson's title / metadata.
     * Allowed only for lessons in 'Draft' or 'Rejected' state.
     *
     * @param id      lesson primary ID
     * @param request update payload
     * @return 200 OK with updated LessonDetailResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LessonDetailResponse>> updateMyLesson(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateLessonRequest request) {
        LessonDetailResponse response = creatorLessonService.updateMyLesson(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài học thành công", response));
    }

    /**
     * Deletes a lesson owned by the creator.
     * Allowed only for lessons in 'Draft' or 'Rejected' state.
     *
     * @param id lesson primary ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyLesson(@PathVariable("id") Long id) {
        creatorLessonService.deleteMyLesson(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a vocabulary to a lesson owned by the creator.
     *
     * @param id      lesson primary ID
     * @param vocabId vocabulary primary ID
     * @return 200 OK with updated LessonDetailResponse
     */
    @PostMapping("/{id}/vocabularies/{vocabId}")
    public ResponseEntity<ApiResponse<LessonDetailResponse>> addVocabularyToLesson(
            @PathVariable("id") Long id,
            @PathVariable("vocabId") Long vocabId) {
        LessonDetailResponse response = creatorLessonService.addVocabularyToLesson(id, vocabId);
        return ResponseEntity.ok(ApiResponse.success("Thêm từ vựng vào bài học thành công", response));
    }

    /**
     * Removes a vocabulary from a lesson owned by the creator.
     *
     * @param id      lesson primary ID
     * @param vocabId vocabulary primary ID
     * @return 200 OK with updated LessonDetailResponse
     */
    @DeleteMapping("/{id}/vocabularies/{vocabId}")
    public ResponseEntity<ApiResponse<LessonDetailResponse>> removeVocabularyFromLesson(
            @PathVariable("id") Long id,
            @PathVariable("vocabId") Long vocabId) {
        LessonDetailResponse response = creatorLessonService.removeVocabularyFromLesson(id, vocabId);
        return ResponseEntity.ok(ApiResponse.success("Xóa từ vựng khỏi bài học thành công", response));
    }

    /**
     * Reorders vocabulary items within a lesson.
     * Supports both PUT and POST methods on /{id}/reorder.
     *
     * @param id      lesson primary ID
     * @param request reorder payload
     * @return 200 OK with updated LessonDetailResponse
     */
    @RequestMapping(value = "/{id}/reorder", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<LessonDetailResponse>> reorderVocabulary(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReorderVocabRequest request) {
        LessonDetailResponse response = creatorLessonService.reorderVocabulary(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thứ tự từ vựng thành công", response));
    }

    /**
     * Submits a lesson for moderation queue ('Draft'/'Rejected' -> 'Pending').
     *
     * @param id lesson primary ID
     * @return 200 OK with updated LessonDetailResponse
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<LessonDetailResponse>> submitForModeration(@PathVariable("id") Long id) {
        LessonDetailResponse response = creatorLessonService.submitForModeration(id);
        return ResponseEntity.ok(ApiResponse.success("Nộp bài học kiểm duyệt thành công", response));
    }

    /**
     * Step 1: Uploads and validates an Excel file for preview.
     * Generates a detailed validation report without mutating database state.
     * Conforms to .agents/API.md section 2.4 (POST /api/v1/creator/lessons/import).
     *
     * @param file uploaded .xlsx file
     * @return 200 OK with ImportValidationReport wrapped in ApiResponse
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportValidationReport>> importPreview(
            @RequestParam("file") MultipartFile file) {
        ImportValidationReport report = excelParserService.parseAndValidate(file);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * Step 2: Confirms creation of a new lesson and constituent vocabulary items from Excel.
     * Executes in a single atomic transaction: creates Lesson, persists new Vocabulary items,
     * links vocabularies to lesson in display order.
     * Conforms to .agents/API.md section 2.4 (POST /api/v1/creator/lessons/import/confirm).
     *
     * @param title title of the new lesson
     * @param file  the uploaded .xlsx file
     * @return 201 Created with LessonDetailResponse wrapped in ApiResponse
     */
    @PostMapping(value = "/import/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LessonDetailResponse>> importConfirm(
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file) {
        LessonDetailResponse response = creatorLessonService.importLessonFromExcel(title, file);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Import bài học từ Excel thành công", response));
    }
}
