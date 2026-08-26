package com.elearning.repository;

import com.elearning.entity.Radical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for {@link Radical} entity.
 * Supports CRUD operations and character lookup for 214 Kangxi radicals.
 */
@Repository
public interface RadicalRepository extends JpaRepository<Radical, Integer> {

    /**
     * Finds a radical by its unique character symbol.
     *
     * @param character the radical Chinese character
     * @return Optional containing found Radical or empty if not found
     */
    Optional<Radical> findByCharacter(String character);

    /**
     * Checks whether a radical with the given character exists.
     *
     * @param character the radical Chinese character
     * @return true if exists, false otherwise
     */
    boolean existsByCharacter(String character);
}
