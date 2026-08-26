package com.elearning.repository;

import com.elearning.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for {@link Account} entity.
 * Provides standard CRUD and queries for account lookup and verification.
 * Note: Uses explicit JPQL @Query for findByEmailOrPhone and existsByEmailOrPhone
 * because the entity property name 'emailOrPhone' contains the reserved keyword 'Or',
 * which causes Spring Data's derived query parser to split into 'email' OR 'phone'.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Finds an account by its unique email or phone identifier.
     *
     * @param emailOrPhone user email address or phone number
     * @return Optional containing found Account or empty if not found
     */
    @Query("SELECT a FROM Account a WHERE a.emailOrPhone = :emailOrPhone")
    Optional<Account> findByEmailOrPhone(@Param("emailOrPhone") String emailOrPhone);

    /**
     * Checks if an account exists with the given email or phone.
     *
     * @param emailOrPhone user email address or phone number
     * @return true if exists, false otherwise
     */
    @Query("SELECT (COUNT(a) > 0) FROM Account a WHERE a.emailOrPhone = :emailOrPhone")
    boolean existsByEmailOrPhone(@Param("emailOrPhone") String emailOrPhone);
}
