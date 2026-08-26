package com.elearning;

import com.elearning.repository.AccountRepository;
import com.elearning.security.CustomUserDetails;
import com.elearning.security.JwtUtil;
import com.elearning.test.RbacTestControllers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({
        RbacTestControllers.AdminTestController.class,
        RbacTestControllers.ModeratorTestController.class,
        RbacTestControllers.CreatorTestController.class
})
@DisplayName("Task 3D.1: Security & RBAC Integration Verification Tests")
class RbacSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Nested
    @DisplayName("1. Unauthenticated Access Boundaries (No JWT -> 401 Unauthorized)")
    class UnauthenticatedBoundaryTests {

        @ParameterizedTest(name = "Unauthenticated GET {0} -> 401 Unauthorized")
        @CsvSource({
                "/api/v1/admin/test",
                "/api/v1/moderator/test",
                "/api/v1/creator/test",
                "/api/v1/users/profile"
        })
        void testUnauthenticatedRequestsReturn401(String endpoint) throws Exception {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Nested
    @DisplayName("2. Mandatory Learner Negative Tests (Learner -> Protected Admin/Mod/Creator -> 403 Forbidden)")
    class LearnerNegativeTests {

        @Test
        @DisplayName("GIVEN authenticated Learner WHEN accessing Admin-only endpoint THEN 403 Forbidden")
        void testLearnerAccessAdminForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/admin/test")
                            .with(user("learner").roles("Learner")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN authenticated Learner WHEN accessing Moderator-only endpoint THEN 403 Forbidden")
        void testLearnerAccessModeratorForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/test")
                            .with(user("learner").roles("Learner")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN authenticated Learner WHEN accessing Creator-only endpoint THEN 403 Forbidden")
        void testLearnerAccessCreatorForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/creator/test")
                            .with(user("learner").roles("Learner")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("3. Cross-Role RBAC Authorization Matrix Tests")
    class RoleAuthorizationMatrixTests {

        @Test
        @DisplayName("GIVEN Creator WHEN accessing Creator endpoint THEN 200 OK")
        void testCreatorAllowedOnCreatorEndpoint() throws Exception {
            mockMvc.perform(get("/api/v1/creator/test")
                            .with(user("creator").roles("Creator")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Creator endpoint reached"));
        }

        @Test
        @DisplayName("GIVEN Creator WHEN accessing Admin or Moderator endpoint THEN 403 Forbidden")
        void testCreatorForbiddenOnAdminAndModeratorEndpoints() throws Exception {
            mockMvc.perform(get("/api/v1/admin/test")
                            .with(user("creator").roles("Creator")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            mockMvc.perform(get("/api/v1/moderator/test")
                            .with(user("creator").roles("Creator")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN Moderator WHEN accessing Moderator endpoint THEN 200 OK")
        void testModeratorAllowedOnModeratorEndpoint() throws Exception {
            mockMvc.perform(get("/api/v1/moderator/test")
                            .with(user("moderator").roles("Moderator")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Moderator endpoint reached"));
        }

        @Test
        @DisplayName("GIVEN Moderator WHEN accessing Admin or Creator endpoint THEN 403 Forbidden")
        void testModeratorForbiddenOnAdminAndCreatorEndpoints() throws Exception {
            mockMvc.perform(get("/api/v1/admin/test")
                            .with(user("moderator").roles("Moderator")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            mockMvc.perform(get("/api/v1/creator/test")
                            .with(user("moderator").roles("Moderator")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN Admin WHEN accessing Admin, Moderator, or Creator endpoint THEN 200 OK")
        void testAdminAllowedOnAdminModeratorAndCreatorEndpoints() throws Exception {
            mockMvc.perform(get("/api/v1/admin/test")
                            .with(user("admin").roles("Admin")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Admin endpoint reached"));

            mockMvc.perform(get("/api/v1/moderator/test")
                            .with(user("admin").roles("Admin")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Moderator endpoint reached"));

            mockMvc.perform(get("/api/v1/creator/test")
                            .with(user("admin").roles("Admin")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Creator endpoint reached"));
        }
    }

    @Nested
    @DisplayName("4. CustomUserDetails Principal Integration Tests")
    class CustomUserDetailsRbacTests {

        @Test
        @DisplayName("GIVEN CustomUserDetails with ROLE_Admin WHEN accessing Admin endpoint THEN 200 OK")
        void testCustomUserDetailsAdminAccess() throws Exception {
            CustomUserDetails adminDetails = new CustomUserDetails(
                    99L, "admin@elearning.com", "hash", "Active",
                    List.of(new SimpleGrantedAuthority("ROLE_Admin"))
            );

            mockMvc.perform(get("/api/v1/admin/test")
                            .with(user(adminDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Admin endpoint reached"));
        }

        @Test
        @DisplayName("GIVEN CustomUserDetails with ROLE_Learner WHEN accessing Admin endpoint THEN 403 Forbidden")
        void testCustomUserDetailsLearnerForbidden() throws Exception {
            CustomUserDetails learnerDetails = new CustomUserDetails(
                    98L, "learner@elearning.com", "hash", "Active",
                    List.of(new SimpleGrantedAuthority("ROLE_Learner"))
            );

            mockMvc.perform(get("/api/v1/admin/test")
                            .with(user(learnerDetails)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("5. Real End-to-End JWT RBAC Integration Flow (JwtUtil -> Bearer -> Filter -> RBAC)")
    class RealJwtRbacIntegrationTests {

        @Test
        @DisplayName("GIVEN real JWT with Learner role WHEN accessing Admin endpoint THEN 403 Forbidden")
        void testRealJwtLearnerForbiddenOnAdmin() throws Exception {
            String token = jwtUtil.generateToken("learner.jwt@elearning.com", List.of("Learner"));

            mockMvc.perform(get("/api/v1/admin/test")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("GIVEN real JWT with Admin role WHEN accessing Admin endpoint THEN 200 OK")
        void testRealJwtAdminAllowedOnAdmin() throws Exception {
            String token = jwtUtil.generateToken("admin.jwt@elearning.com", List.of("Admin"));

            mockMvc.perform(get("/api/v1/admin/test")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Admin endpoint reached"));
        }

        @Test
        @DisplayName("GIVEN real JWT with Creator role WHEN accessing Creator endpoint THEN 200 OK")
        void testRealJwtCreatorAllowedOnCreator() throws Exception {
            String token = jwtUtil.generateToken("creator.jwt@elearning.com", List.of("Creator"));

            mockMvc.perform(get("/api/v1/creator/test")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Creator endpoint reached"));
        }

        @Test
        @DisplayName("GIVEN real JWT with Moderator role WHEN accessing Moderator endpoint THEN 200 OK")
        void testRealJwtModeratorAllowedOnModerator() throws Exception {
            String token = jwtUtil.generateToken("moderator.jwt@elearning.com", List.of("Moderator"));

            mockMvc.perform(get("/api/v1/moderator/test")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("Moderator endpoint reached"));
        }

        @Test
        @DisplayName("GIVEN real JWT with Moderator role WHEN accessing Admin endpoint THEN 403 Forbidden")
        void testRealJwtModeratorForbiddenOnAdmin() throws Exception {
            String token = jwtUtil.generateToken("moderator.jwt@elearning.com", List.of("Moderator"));

            mockMvc.perform(get("/api/v1/admin/test")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("6. Public Endpoints Regression (Public auth routes remain unauthenticated)")
    class PublicEndpointsRegressionTests {

        @Test
        @DisplayName("GIVEN unauthenticated request to /api/v1/auth/login WHEN invalid body THEN reaches controller returning 400 Bad Request")
        void testAuthLoginPublic() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
}
