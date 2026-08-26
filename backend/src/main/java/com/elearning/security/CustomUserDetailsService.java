package com.elearning.security;

import com.elearning.entity.Account;
import com.elearning.repository.AccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that loads user account details from the database for Spring Security authentication.
 * Implements Spring Security's UserDetailsService interface.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public CustomUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Login identifier cannot be null or blank");
        }

        Account account = accountRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email or phone: " + username));

        return CustomUserDetails.fromAccount(account);
    }
}
