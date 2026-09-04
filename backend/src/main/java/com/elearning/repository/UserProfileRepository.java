package com.elearning.repository;

import com.elearning.entity.Account;
import com.elearning.entity.UserProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for {@link UserProfile} entity.
 * Supports standard profile management and lookup by associated Account.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * Finds user profile associated with the given account.
     *
     * @param account associated account entity
     * @return Optional containing found UserProfile or empty if not found
     */
    Optional<UserProfile> findByAccount(Account account);

    /**
     * Finds user profile associated with the given account's emailOrPhone.
     *
     * @param emailOrPhone email or phone of the associated account
     * @return Optional containing found UserProfile or empty if not found
     */
    @Query("SELECT p FROM UserProfile p WHERE p.account.emailOrPhone = :emailOrPhone")
    Optional<UserProfile> findByAccountEmailOrPhone(@Param("emailOrPhone") String emailOrPhone);

    /**
     * Finds user profile associated with the given account's emailOrPhone with pessimistic write lock (SELECT ... FOR UPDATE).
     * Used for per-user quota synchronization to prevent TOCTOU race conditions (BE-CONC-002).
     *
     * @param emailOrPhone email or phone of the associated account
     * @return Optional containing found UserProfile or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM UserProfile p WHERE p.account.emailOrPhone = :emailOrPhone")
    Optional<UserProfile> findByAccountEmailOrPhoneWithLock(@Param("emailOrPhone") String emailOrPhone);
}
