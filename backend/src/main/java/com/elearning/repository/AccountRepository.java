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

    /**
     * Finds the account authentication and authorization summary by email or phone identifier.
     * Lightweight projection query fetching only status and authorization_version columns
     * for high-performance authentication and immediate role revocation verification (BE-AUTH-004).
     *
     * @param emailOrPhone user email address or phone number
     * @return Optional containing AccountAuthSummary or empty if not found
     */
    @Query("SELECT new com.elearning.repository.AccountAuthSummary(a.status, a.authorizationVersion) FROM Account a WHERE a.emailOrPhone = :emailOrPhone")
    Optional<AccountAuthSummary> findAuthSummaryByEmailOrPhone(@Param("emailOrPhone") String emailOrPhone);

    /**
     * Finds the account status by its unique email or phone identifier.
     * Lightweight projection query fetching only the status column for fast authentication verification.
     *
     * @param emailOrPhone user email address or phone number
     * @return Optional containing account status string or empty if not found
     */
    @Query("SELECT a.status FROM Account a WHERE a.emailOrPhone = :emailOrPhone")
    Optional<String> findStatusByEmailOrPhone(@Param("emailOrPhone") String emailOrPhone);

    /**
     * Finds an account by its primary key using a pessimistic write lock (SELECT ... FOR UPDATE).
     * Serializes concurrent modifications on the target account.
     *
     * @param accountId target account ID
     * @return Optional containing locked Account or empty if not found
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId")
    Optional<Account> findByIdForUpdate(@Param("accountId") Long accountId);

    /**
     * Finds an account by ID with its roles eagerly fetched.
     *
     * @param accountId target account ID
     * @return Optional containing Account with roles
     */
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.roles WHERE a.accountId = :accountId")
    Optional<Account> findByIdWithRoles(@Param("accountId") Long accountId);

    /**
     * Finds accounts with optional status and search filtering with pagination.
     *
     * @param status   optional status filter (Active, Inactive, Banned)
     * @param search   optional search query matching email/phone or full name
     * @param pageable pagination parameters
     * @return Page of Account entities
     */
    @Query(value = "SELECT DISTINCT a FROM Account a LEFT JOIN a.userProfile up " +
                   "WHERE (:status IS NULL OR a.status = :status) " +
                   "AND (:search IS NULL OR LOWER(a.emailOrPhone) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(up.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                   "ORDER BY a.accountId ASC",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Account a LEFT JOIN a.userProfile up " +
                        "WHERE (:status IS NULL OR a.status = :status) " +
                        "AND (:search IS NULL OR LOWER(a.emailOrPhone) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(up.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<Account> findAllWithFilters(
            @Param("status") String status,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable);
}
