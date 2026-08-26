package com.elearning.test;

import com.elearning.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controllers used exclusively to verify role-based authorization boundaries
 * in Spring Security integration tests without adding placeholder business features to production.
 */
public class RbacTestControllers {

    @RestController
    public static class AdminTestController {
        @GetMapping("/api/v1/admin/test")
        public ResponseEntity<ApiResponse<String>> testAdminAccess() {
            return ResponseEntity.ok(ApiResponse.success("Admin endpoint reached"));
        }
    }

    @RestController
    public static class ModeratorTestController {
        @GetMapping("/api/v1/moderator/test")
        public ResponseEntity<ApiResponse<String>> testModeratorAccess() {
            return ResponseEntity.ok(ApiResponse.success("Moderator endpoint reached"));
        }
    }

    @RestController
    public static class CreatorTestController {
        @GetMapping("/api/v1/creator/test")
        public ResponseEntity<ApiResponse<String>> testCreatorAccess() {
            return ResponseEntity.ok(ApiResponse.success("Creator endpoint reached"));
        }
    }
}
