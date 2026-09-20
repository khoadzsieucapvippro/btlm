package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.LoginRequest;
import com.elearning.dto.request.RegisterRequest;
import com.elearning.dto.response.AuthResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Role;
import com.elearning.entity.UserProfile;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.RoleRepository;
import com.elearning.security.CustomUserDetails;
import com.elearning.security.JwtUtil;
import com.elearning.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role learnerRole;

    @BeforeEach
    void setUp() {
        learnerRole = new Role(1, "Learner");
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegisterTests {

        @Test
        @DisplayName("GIVEN valid RegisterRequest WHEN register THEN creates Account with Learner role, hashed password, UserProfile, and returns AuthResponse")
        void testRegisterSuccess() {
            RegisterRequest request = new RegisterRequest("newlearner@elearning.com", "rawPass123", "Tran Van A");

            when(accountRepository.existsByEmailOrPhone("newlearner@elearning.com")).thenReturn(false);
            when(roleRepository.findByRoleName("Learner")).thenReturn(Optional.of(learnerRole));
            when(passwordEncoder.encode("rawPass123")).thenReturn("$2a$10$hashedPasswordValue");
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
                Account acc = invocation.getArgument(0);
                // simulate JPA identity generation
                try {
                    var field = Account.class.getDeclaredField("accountId");
                    field.setAccessible(true);
                    field.set(acc, 100L);
                } catch (Exception ignored) {}
                return acc;
            });
            when(jwtUtil.generateToken("newlearner@elearning.com", List.of("Learner"), 1L)).thenReturn("mock.jwt.token");

            AuthResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("mock.jwt.token");
            assertThat(response.getType()).isEqualTo("Bearer");
            assertThat(response.getEmailOrPhone()).isEqualTo("newlearner@elearning.com");
            assertThat(response.getFullName()).isEqualTo("Tran Van A");
            assertThat(response.getRoles()).containsExactly("Learner");

            // Verify account details persisted
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            Account saved = captor.getValue();
            assertThat(saved.getEmailOrPhone()).isEqualTo("newlearner@elearning.com");
            assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hashedPasswordValue");
            assertThat(saved.getStatus()).isEqualTo("Active");
            assertThat(saved.getRoles()).containsExactly(learnerRole);
            assertThat(saved.getUserProfile()).isNotNull();
            assertThat(saved.getUserProfile().getFullName()).isEqualTo("Tran Van A");
        }

        @Test
        @DisplayName("GIVEN existing emailOrPhone WHEN register THEN throws BusinessException CONFLICT without saving")
        void testRegisterDuplicateIdentifier() {
            RegisterRequest request = new RegisterRequest("existing@elearning.com", "pass123", "User Name");
            when(accountRepository.existsByEmailOrPhone("existing@elearning.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("GIVEN missing Learner role in repository WHEN register THEN throws BusinessException INTERNAL_ERROR")
        void testRegisterLearnerRoleMissing() {
            RegisterRequest request = new RegisterRequest("learner@elearning.com", "pass123", "User Name");
            when(accountRepository.existsByEmailOrPhone("learner@elearning.com")).thenReturn(false);
            when(roleRepository.findByRoleName("Learner")).thenReturn(Optional.empty());
            when(roleRepository.findById(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));

            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("GIVEN valid credentials and active account WHEN login THEN returns AuthResponse with JWT")
        void testLoginSuccess() {
            LoginRequest request = new LoginRequest("user@elearning.com", "correctPass");

            Account account = new Account();
            account.setEmailOrPhone("user@elearning.com");
            account.setPasswordHash("hashedPass");
            account.setStatus("Active");
            account.addRole(learnerRole);
            UserProfile profile = new UserProfile();
            profile.setFullName("Nguyen Van B");
            account.setUserProfile(profile);

            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
            when(accountRepository.findByEmailOrPhone("user@elearning.com")).thenReturn(Optional.of(account));
            when(jwtUtil.generateToken("user@elearning.com", List.of("Learner"), 1L)).thenReturn("login.jwt.token");

            AuthResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("login.jwt.token");
            assertThat(response.getEmailOrPhone()).isEqualTo("user@elearning.com");
            assertThat(response.getFullName()).isEqualTo("Nguyen Van B");
            assertThat(response.getRoles()).containsExactly("Learner");
        }

        @Test
        @DisplayName("GIVEN invalid credentials WHEN login THEN throws BusinessException UNAUTHORIZED without leaking user existence")
        void testLoginInvalidCredentials() {
            LoginRequest request = new LoginRequest("user@elearning.com", "wrongPass");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        }

        @Test
        @DisplayName("GIVEN inactive or banned account WHEN login THEN throws BusinessException UNAUTHORIZED")
        void testLoginInactiveAccount() {
            LoginRequest request = new LoginRequest("banned@elearning.com", "pass");

            Account account = new Account();
            account.setEmailOrPhone("banned@elearning.com");
            account.setStatus("Banned");
            CustomUserDetails userDetails = CustomUserDetails.fromAccount(account);
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(accountRepository.findByEmailOrPhone("banned@elearning.com")).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        }
    }
}
