package com.elearning.repository;

import com.elearning.entity.Account;
import com.elearning.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
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
    @org.springframework.data.jpa.repository.Query("SELECT p FROM UserProfile p WHERE p.account.emailOrPhone = :emailOrPhone")
    Optional<UserProfile> findByAccountEmailOrPhone(@org.springframework.data.repository.query.Param("emailOrPhone") String emailOrPhone);
}
