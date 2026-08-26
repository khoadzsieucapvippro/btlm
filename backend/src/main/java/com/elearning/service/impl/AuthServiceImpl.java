package com.elearning.service.impl;

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
import com.elearning.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link AuthService} handling account registration and login.
 * Orchestrates AccountRepository, RoleRepository, PasswordEncoder, AuthenticationManager, and JwtUtil.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(
            AccountRepository accountRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Uniqueness check
        if (accountRepository.existsByEmailOrPhone(request.getEmailOrPhone())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Email hoặc số điện thoại đã được đăng ký trong hệ thống");
        }

        // 2. Fetch default Learner role
        Role learnerRole = roleRepository.findByRoleName("Learner")
                .or(() -> roleRepository.findById(1))
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "Không tìm thấy vai trò mặc định Learner trong hệ thống"));

        // 3. Construct Account entity with hashed password
        Account account = new Account();
        account.setEmailOrPhone(request.getEmailOrPhone());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setStatus("Active");
        account.addRole(learnerRole);

        // 4. Attach UserProfile
        UserProfile userProfile = new UserProfile();
        userProfile.setFullName(request.getFullName());
        account.setUserProfile(userProfile);

        // 5. Persist account and cascaded profile
        Account savedAccount = accountRepository.save(account);

        // 6. Generate JWT token and return AuthResponse
        List<String> roleNames = List.of(learnerRole.getRoleName());
        String token = jwtUtil.generateToken(savedAccount.getEmailOrPhone(), roleNames);

        return new AuthResponse(
                token,
                "Bearer",
                savedAccount.getAccountId(),
                savedAccount.getEmailOrPhone(),
                request.getFullName(),
                roleNames
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate via AuthenticationManager (verifies credentials through CustomUserDetailsService & PasswordEncoder)
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmailOrPhone(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Email/số điện thoại hoặc mật khẩu không chính xác");
        }

        // 2. Retrieve authenticated principal and account details
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Account account = accountRepository.findByEmailOrPhone(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Email/số điện thoại hoặc mật khẩu không chính xác"));

        // 3. Verify active status
        if (!"Active".equalsIgnoreCase(account.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Tài khoản chưa được kích hoạt hoặc đã bị vô hiệu hóa");
        }

        // 4. Extract roles and profile
        List<String> roleNames = account.getRoles().stream()
                .map(Role::getRoleName)
                .toList();

        String fullName = account.getUserProfile() != null ? account.getUserProfile().getFullName() : null;

        // 5. Generate JWT token
        String token = jwtUtil.generateToken(account.getEmailOrPhone(), roleNames);

        return new AuthResponse(
                token,
                "Bearer",
                account.getAccountId(),
                account.getEmailOrPhone(),
                fullName,
                roleNames
        );
    }
}
