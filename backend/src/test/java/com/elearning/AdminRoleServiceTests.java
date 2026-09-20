package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.UpdateAccountRolesRequest;
import com.elearning.dto.response.AccountResponse;
import com.elearning.dto.response.RoleResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.service.impl.AdminRoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 8D.2: AdminRoleService Unit Tests")
class AdminRoleServiceTests {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AdminRoleServiceImpl adminRoleService;

    private Role roleLearner;
    private Role roleCreator;
    private Role roleModerator;
    private Role roleAdmin;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        roleLearner = new Role(1, "Learner");
        roleCreator = new Role(2, "Creator");
        roleModerator = new Role(3, "Moderator");
        roleAdmin = new Role(4, "Admin");

        testAccount = new Account();
        testAccount.setAccountId(5L);
        testAccount.setEmailOrPhone("user5@example.com");
        testAccount.setStatus("Active");
        testAccount.setAuthorizationVersion(1L);
        testAccount.addRole(roleLearner);
    }

    @Nested
    @DisplayName("getRoles Tests")
    class GetRolesTests {

        @Test
        @DisplayName("GIVEN system roles exist WHEN calling getRoles THEN returns all 4 system roles sorted")
        void testGetRoles_success() {
            when(roleRepository.findAll()).thenReturn(List.of(roleAdmin, roleLearner, roleModerator, roleCreator));

            List<RoleResponse> roles = adminRoleService.getRoles();

            assertThat(roles).hasSize(4);
            assertThat(roles.get(0).getRoleId()).isEqualTo(1);
            assertThat(roles.get(0).getRoleName()).isEqualTo("Learner");
            assertThat(roles.get(3).getRoleId()).isEqualTo(4);
            assertThat(roles.get(3).getRoleName()).isEqualTo("Admin");
        }
    }

    @Nested
    @DisplayName("updateAccountRoles Tests")
    class UpdateAccountRolesTests {

        @Test
        @DisplayName("GIVEN effective role change WHEN updating roles THEN roles replaced and authorizationVersion incremented")
        void testUpdateAccountRoles_effectiveChangeIncrementsVersion() {
            when(roleRepository.findAll()).thenReturn(List.of(roleLearner, roleCreator, roleModerator, roleAdmin));
            when(accountRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

            UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of("Learner", "Creator"));
            AccountResponse response = adminRoleService.updateAccountRoles(5L, request, "admin@example.com");

            assertThat(response).isNotNull();
            assertThat(response.getRoles()).containsExactlyInAnyOrder("Learner", "Creator");
            assertThat(testAccount.getAuthorizationVersion()).isEqualTo(2L);
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("GIVEN no-op role update (identical roles) WHEN updating THEN authorizationVersion remains unchanged")
        void testUpdateAccountRoles_noOpDoesNotIncrementVersion() {
            when(roleRepository.findAll()).thenReturn(List.of(roleLearner, roleCreator, roleModerator, roleAdmin));
            when(accountRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(testAccount));

            // Requesting same role "Learner" that testAccount already has
            UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of("Learner"));
            AccountResponse response = adminRoleService.updateAccountRoles(5L, request, "admin@example.com");

            assertThat(response).isNotNull();
            assertThat(testAccount.getAuthorizationVersion()).isEqualTo(1L);
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("GIVEN duplicate role names in request WHEN updating THEN roles are deduplicated (BUS-8D-02)")
        void testUpdateAccountRoles_deduplicatesDuplicateEntries() {
            when(roleRepository.findAll()).thenReturn(List.of(roleLearner, roleCreator, roleModerator, roleAdmin));
            when(accountRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

            UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of("Creator", "Creator", "ROLE_CREATOR"));
            AccountResponse response = adminRoleService.updateAccountRoles(5L, request, "admin@example.com");

            assertThat(response).isNotNull();
            assertThat(response.getRoles()).containsExactly("Creator");
            assertThat(testAccount.getAuthorizationVersion()).isEqualTo(2L);
        }

        @Test
        @DisplayName("GIVEN invalid role name WHEN updating THEN throws BAD_REQUEST")
        void testUpdateAccountRoles_invalidRoleThrowsBadRequest() {
            when(roleRepository.findAll()).thenReturn(List.of(roleLearner, roleCreator, roleModerator, roleAdmin));

            UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of("Learner", "SuperAdmin"));

            assertThatThrownBy(() -> adminRoleService.updateAccountRoles(5L, request, "admin@example.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                    .hasMessageContaining("Vai trò không hợp lệ: SuperAdmin");
        }

        @Test
        @DisplayName("GIVEN empty role list WHEN updating THEN throws VALIDATION_ERROR (BUS-8D-01)")
        void testUpdateAccountRoles_emptyRolesThrowsValidationError() {
            UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of());

            assertThatThrownBy(() -> adminRoleService.updateAccountRoles(5L, request, "admin@example.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
        }

        @Test
        @DisplayName("GIVEN admin self-demotion attempt WHEN removing Admin role from own account THEN throws BAD_REQUEST (POL-8D-02)")
        void testUpdateAccountRoles_selfDemotionThrowsBadRequest() {
            testAccount.getRoles().clear();
            testAccount.addRole(roleAdmin);

            when(roleRepository.findAll()).thenReturn(List.of(roleLearner, roleCreator, roleModerator, roleAdmin));
            when(accountRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(testAccount));

            // Admin user5 attempting to change own roles to only Learner
            UpdateAccountRolesRequest request = new UpdateAccountRolesRequest(List.of("Learner"));

            assertThatThrownBy(() -> adminRoleService.updateAccountRoles(5L, request, "user5@example.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST)
                    .hasMessage("Không thể tự tước quyền Quản trị viên (Admin) của chính mình");
        }
    }
}
