package com.elearning.specification;

import com.elearning.dto.request.VocabularySearchCriteria;
import com.elearning.entity.Radical;
import com.elearning.entity.Vocabulary;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable JPA Specifications for filtering {@link Vocabulary} entities.
 * Supports dynamic combination of:
 * - hanzi (contains / substring)
 * - pinyin (case-insensitive contains)
 * - pinyinRaw (case-insensitive contains on shadow column pinyin_raw)
 * - radicalId (Many-to-Many join with query.distinct(true))
 * - search (unified keyword search across hanzi, pinyin, and pinyin_raw)
 */
public final class VocabularySpecification {

    private VocabularySpecification() {
        // utility class
    }

    public static Specification<Vocabulary> hasHanzi(String hanzi) {
        return (root, query, cb) -> {
            if (hanzi == null || hanzi.trim().isEmpty()) {
                return null;
            }
            return cb.like(root.get("hanzi"), "%" + hanzi.trim() + "%");
        };
    }

    public static Specification<Vocabulary> hasPinyin(String pinyin) {
        return (root, query, cb) -> {
            if (pinyin == null || pinyin.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("pinyin")), "%" + pinyin.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Vocabulary> hasPinyinRaw(String pinyinRaw) {
        return (root, query, cb) -> {
            if (pinyinRaw == null || pinyinRaw.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("pinyinRaw")), "%" + pinyinRaw.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Vocabulary> hasRadicalId(Integer radicalId) {
        return (root, query, cb) -> {
            if (radicalId == null) {
                return null;
            }
            query.distinct(true);
            Join<Vocabulary, Radical> radicalJoin = root.join("radicals");
            return cb.equal(radicalJoin.get("radicalId"), radicalId);
        };
    }

    public static Specification<Vocabulary> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String trimmed = keyword.trim();
            String lowerPattern = "%" + trimmed.toLowerCase() + "%";
            return cb.or(
                    cb.like(root.get("hanzi"), "%" + trimmed + "%"),
                    cb.like(cb.lower(root.get("pinyin")), lowerPattern),
                    cb.like(cb.lower(root.get("pinyinRaw")), lowerPattern)
            );
        };
    }

    public static Specification<Vocabulary> fromCriteria(VocabularySearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null || criteria.isEmpty()) {
                return null;
            }

            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getHanzi() != null && !criteria.getHanzi().trim().isEmpty()) {
                predicates.add(cb.like(root.get("hanzi"), "%" + criteria.getHanzi().trim() + "%"));
            }
            if (criteria.getPinyin() != null && !criteria.getPinyin().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("pinyin")), "%" + criteria.getPinyin().trim().toLowerCase() + "%"));
            }
            if (criteria.getPinyinRaw() != null && !criteria.getPinyinRaw().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("pinyinRaw")), "%" + criteria.getPinyinRaw().trim().toLowerCase() + "%"));
            }
            if (criteria.getRadicalId() != null) {
                query.distinct(true);
                Join<Vocabulary, Radical> radicalJoin = root.join("radicals");
                predicates.add(cb.equal(radicalJoin.get("radicalId"), criteria.getRadicalId()));
            }
            if (criteria.getSearch() != null && !criteria.getSearch().trim().isEmpty()) {
                String trimmed = criteria.getSearch().trim();
                String lowerPattern = "%" + trimmed.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("hanzi"), "%" + trimmed + "%"),
                        cb.like(cb.lower(root.get("pinyin")), lowerPattern),
                        cb.like(cb.lower(root.get("pinyinRaw")), lowerPattern)
                ));
            }

            if (predicates.isEmpty()) {
                return null;
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
