package com.elearning.controller;

import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.PageResponse;
import com.elearning.dto.response.RadicalDetailResponse;
import com.elearning.dto.response.RadicalResponse;
import com.elearning.service.RadicalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST Controller for Kangxi Radicals catalog lookup.
 * Base path: /api/v1/radicals
 * Endpoints:
 * - GET /api/v1/radicals: Paginated list of Kangxi radicals (Public)
 * - GET /api/v1/radicals/{id}: Detail of a Kangxi radical by ID (Public)
 */
@RestController
@RequestMapping("/api/v1/radicals")
public class RadicalController {

    private final RadicalService radicalService;

    public RadicalController(RadicalService radicalService) {
        this.radicalService = radicalService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RadicalResponse>>> getAllRadicals(Pageable pageable) {
        Page<RadicalResponse> radicalsPage = radicalService.getAllRadicals(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(radicalsPage)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RadicalDetailResponse>> getRadicalById(@PathVariable("id") Integer id) {
        RadicalDetailResponse response = radicalService.getRadicalById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
