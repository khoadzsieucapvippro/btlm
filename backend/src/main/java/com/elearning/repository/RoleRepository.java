package com.elearning.repository;

import com.elearning.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for {@link Role} entity.
 * Provides lookup by role name.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    /**
     * Finds a role by its unique role name (e.g. ROLE_LEARNER, ROLE_ADMIN).
     *
     * @param roleName name of the role
     * @return Optional containing found Role or empty if not found
     */
    Optional<Role> findByRoleName(String roleName);
}
