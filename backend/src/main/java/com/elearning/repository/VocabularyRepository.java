package com.elearning.repository;

import com.elearning.entity.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for {@link Vocabulary} entity.
 * Supports exact lookup, toneless search (pinyin_raw), Hanzi filtering,
 * combined multi-criteria search, and pagination.
 */
@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    /**
     * Finds a vocabulary by its unique composite business key (hanzi + pinyin_raw).
     * Matches the physical UNIQUE constraint uk_vocab_hanzi_pinyin_raw.
     *
     * @param hanzi Chinese word characters
     * @param pinyinRaw raw toneless pinyin
     * @return Optional containing found Vocabulary or empty if not found
     */
    Optional<Vocabulary> findByHanziAndPinyinRaw(String hanzi, String pinyinRaw);

    /**
     * Finds vocabularies by exact Chinese word characters with pagination.
     *
     * @param hanzi Chinese word characters
     * @param pageable pagination parameters
     * @return Page of matching Vocabulary records
     */
    Page<Vocabulary> findByHanzi(String hanzi, Pageable pageable);

    /**
     * Finds vocabularies by raw toneless pinyin with pagination.
     *
     * @param pinyinRaw raw toneless pinyin
     * @param pageable pagination parameters
     * @return Page of matching Vocabulary records
     */
    Page<Vocabulary> findByPinyinRaw(String pinyinRaw, Pageable pageable);

    /**
     * Combined multi-criteria search by keyword across hanzi, pinyin, or pinyin_raw.
     * Supports the public catalog search endpoint GET /api/v1/vocabulary?search=...
     *
     * @param keyword search term
     * @param pageable pagination parameters
     * @return Page of matching Vocabulary records
     */
    @Query("SELECT v FROM Vocabulary v WHERE " +
            "v.hanzi LIKE %:keyword% OR " +
            "LOWER(v.pinyin) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.pinyinRaw) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Vocabulary> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Finds all vocabularies containing the given radical ID.
     *
     * @param radicalId the radical ID
     * @return List of matching Vocabulary records
     */
    @Query("SELECT v FROM Vocabulary v JOIN v.radicals r WHERE r.radicalId = :radicalId ORDER BY v.vocabId ASC")
    List<Vocabulary> findByRadicalId(@Param("radicalId") Integer radicalId);

    /**
     * Finds all vocabularies containing the given radical ID with pagination.
     *
     * @param radicalId the radical ID
     * @param pageable pagination parameters
     * @return Page of matching Vocabulary records
     */
    @Query("SELECT v FROM Vocabulary v JOIN v.radicals r WHERE r.radicalId = :radicalId")
    Page<Vocabulary> findByRadicalId(@Param("radicalId") Integer radicalId, Pageable pageable);
}
