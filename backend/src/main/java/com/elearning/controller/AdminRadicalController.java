package com.elearning.controller;

import com.elearning.dto.request.CreateRadicalRequest;
import com.elearning.dto.request.UpdateRadicalRequest;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.RadicalDetailResponse;
import com.elearning.service.RadicalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST Controller for Kangxi Radicals catalog management (CRUD).
 * Base path: /api/v1/admin/radicals
 * Protected by Spring Security (Admin role required).
 * Endpoints:
 * - POST /api/v1/admin/radicals: Create a new Kangxi radical (201 Created)
 * - PUT /api/v1/admin/radicals/{id}: Update an existing Kangxi radical (200 OK)
 * - DELETE /api/v1/admin/radicals/{id}: Delete an existing Kangxi radical (204 No Content)
 */
@RestController
@RequestMapping("/api/v1/admin/radicals")
public class AdminRadicalController {

    private final RadicalService radicalService;

    public AdminRadicalController(RadicalService radicalService) {
        this.radicalService = radicalService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RadicalDetailResponse>> createRadical(@Valid @RequestBody CreateRadicalRequest request) {
        RadicalDetailResponse response = radicalService.createRadical(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo bộ thủ thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RadicalDetailResponse>> updateRadical(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateRadicalRequest request) {
        RadicalDetailResponse response = radicalService.updateRadical(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bộ thủ thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRadical(@PathVariable("id") Integer id) {
        radicalService.deleteRadical(id);
        return ResponseEntity.noContent().build();
    }
}
