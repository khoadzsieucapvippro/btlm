package com.elearning;

import com.elearning.entity.Radical;
import com.elearning.entity.Vocabulary;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.impl.ExcelParserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("Task R3.3: Chinese Domain, Kangxi Radicals & Linguistic Data Integrity Audit")
class ChineseDomainDataAuditTests {

    @Autowired
    private RadicalRepository radicalRepository;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Nested
    @DisplayName("1. 214 Kangxi Radicals Database Integrity Audit")
    class KangxiRadicalsIntegrityTests {

        @Test
        @DisplayName("GIVEN database seed WHEN querying all radicals THEN exactly 214 records exist with contiguous IDs 1..214")
        void testRadicalCountAndIds() {
            List<Radical> radicals = radicalRepository.findAll(Sort.by(Sort.Direction.ASC, "radicalId"));

            assertThat(radicals).hasSize(214);

            for (int i = 0; i < 214; i++) {
                int expectedId = i + 1;
                assertThat(radicals.get(i).getRadicalId()).isEqualTo(expectedId);
            }
        }

        @Test
        @DisplayName("GIVEN 214 radicals WHEN auditing character field THEN all 214 characters are unique and non-blank")
        void testRadicalCharactersUniqueAndNonBlank() {
            List<Radical> radicals = radicalRepository.findAll(Sort.by(Sort.Direction.ASC, "radicalId"));
            Set<String> uniqueCharacters = new HashSet<>();

            for (Radical radical : radicals) {
                String character = radical.getCharacter();
                assertThat(character).isNotNull().isNotBlank();
                assertThat(uniqueCharacters.add(character))
                        .withFailMessage("Duplicate radical character found: %s at ID %d", character, radical.getRadicalId())
                        .isTrue();
            }
            assertThat(uniqueCharacters).hasSize(214);
        }

        @Test
        @DisplayName("GIVEN 214 radicals WHEN auditing Unicode code points THEN identify CJK Unified Ideographs vs Radical Symbols vs Supplements")
        void testRadicalUnicodeCodePointsAudit() {
            List<Radical> radicals = radicalRepository.findAll(Sort.by(Sort.Direction.ASC, "radicalId"));

            int unifiedIdeographCount = 0;
            int compoundDisplayCount = 0;
            List<String> compoundList = new ArrayList<>();

            for (Radical radical : radicals) {
                String character = radical.getCharacter();
                if (character.contains("(") && character.contains(")")) {
                    compoundDisplayCount++;
                    compoundList.add(String.format("ID %d: %s", radical.getRadicalId(), character));
                } else {
                    int firstCodePoint = character.codePointAt(0);
                    // CJK Unified Ideographs block: 0x4E00 - 0x9FFF
                    if (firstCodePoint >= 0x4E00 && firstCodePoint <= 0x9FFF) {
                        unifiedIdeographCount++;
                    }
                }
            }

            // Verify empirical findings:
            // Exactly 43 radicals in the seed data use compound display strings with parentheses e.g. 人(亻), 刀(刂)
            // The remaining 171 are single CJK Unified Ideographs (43 + 171 = 214)
            assertThat(compoundDisplayCount).isEqualTo(43);
            assertThat(unifiedIdeographCount).isEqualTo(171);
            assertThat(compoundDisplayCount + unifiedIdeographCount).isEqualTo(214);
        }

        @Test
        @DisplayName("GIVEN 214 radicals WHEN auditing Pinyin data THEN verify zero missing Pinyin records after R3.8 correction")
        void testRadicalPinyinAudit() {
            List<Radical> radicals = radicalRepository.findAll(Sort.by(Sort.Direction.ASC, "radicalId"));

            List<Integer> missingPinyinIds = new ArrayList<>();
            List<Integer> polyphoneSlashPinyinIds = new ArrayList<>();

            for (Radical radical : radicals) {
                String pinyin = radical.getPinyin();
                if (pinyin == null || pinyin.isBlank()) {
                    missingPinyinIds.add(radical.getRadicalId());
                } else if (pinyin.contains("/")) {
                    polyphoneSlashPinyinIds.add(radical.getRadicalId());
                }
            }

            // R3.8: All 214 radicals now have valid Pinyin; missingPinyinIds must be empty
            assertThat(missingPinyinIds).isEmpty();

            // Explicit verification of corrected radicals 49 (己 -> jǐ) and 172 (隹 -> zhuī)
            Radical radical49 = radicalRepository.findById(49)
                    .orElseThrow(() -> new AssertionError("Radical 49 not found"));
            assertThat(radical49.getCharacter()).isEqualTo("己");
            assertThat(radical49.getPinyin()).isEqualTo("jǐ");
            assertThat(radical49.getMeaningHanViet()).isEqualTo("Kỷ");
            assertThat(radical49.getMeaningVi()).isEqualTo("");

            Radical radical172 = radicalRepository.findById(172)
                    .orElseThrow(() -> new AssertionError("Radical 172 not found"));
            assertThat(radical172.getCharacter()).isEqualTo("隹");
            assertThat(radical172.getPinyin()).isEqualTo("zhuī");
            assertThat(radical172.getMeaningHanViet()).isEqualTo("Chuy");
            assertThat(radical172.getMeaningVi()).isEqualTo("");

            // Record 23 (匸) has slash 'xǐ/xì'
            assertThat(polyphoneSlashPinyinIds).contains(23);
        }

        @Test
        @DisplayName("GIVEN 214 radicals WHEN auditing Vietnamese meaning fields THEN meaning_han_viet is 100% present and meaning_vi is 100% empty string")
        void testRadicalVietnameseMeaningAudit() {
            List<Radical> radicals = radicalRepository.findAll(Sort.by(Sort.Direction.ASC, "radicalId"));

            int emptyHanVietCount = 0;
            int emptyMeaningViCount = 0;

            for (Radical radical : radicals) {
                if (radical.getMeaningHanViet() == null || radical.getMeaningHanViet().isBlank()) {
                    emptyHanVietCount++;
                }
                if (radical.getMeaningVi() == null || radical.getMeaningVi().isBlank()) {
                    emptyMeaningViCount++;
                }
            }

            // Han-Viet transliteration is 100% complete (214/214)
            assertThat(emptyHanVietCount).isEqualTo(0);

            // meaning_vi is currently empty string '' for all 214 records
            assertThat(emptyMeaningViCount).isEqualTo(214);
        }
    }

    @Nested
    @DisplayName("2. Pinyin Raw Conversion & Search Semantics Audit")
    class PinyinRawSemanticsTests {

        @Test
        @DisplayName("GIVEN various pinyin tone representations WHEN converting to pinyinRaw THEN verify toneless conversion")
        void testToPinyinRawConversion() {
            // Standard tone marks
            assertThat(ExcelParserServiceImpl.toPinyinRaw("hǎo")).isEqualTo("hao");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("xué")).isEqualTo("xue");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("zhōng")).isEqualTo("zhong");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("guó")).isEqualTo("guo");

            // Umlaut ü cases
            assertThat(ExcelParserServiceImpl.toPinyinRaw("nǚ")).isEqualTo("nu");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("lǜ")).isEqualTo("lu");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("lü")).isEqualTo("lu");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("nv")).isEqualTo("nu");

            // Neutral tone
            assertThat(ExcelParserServiceImpl.toPinyinRaw("de")).isEqualTo("de");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("ma")).isEqualTo("ma");

            // Whitespace and casing
            assertThat(ExcelParserServiceImpl.toPinyinRaw(" Nǐ Hǎo ")).isEqualTo("ni hao");
        }
    }

    @Nested
    @DisplayName("3. Unicode Code Point & Radical Identity Classification Audit")
    class UnicodeIdentityClassificationTests {

        @Test
        @DisplayName("GIVEN Kangxi Radical 1 '一' WHEN distinguishing Unicode code points THEN prove U+4E00 vs U+2F00 distinctness")
        void testUnicodeCodePointDistinction() {
            int cjkUnifiedIdeographOne = "一".codePointAt(0); // U+4E00
            int kangxiRadicalSymbolOne = "\u2F00".codePointAt(0); // U+2F00

            assertThat(cjkUnifiedIdeographOne).isEqualTo(0x4E00);
            assertThat(kangxiRadicalSymbolOne).isEqualTo(0x2F00);
            assertThat(cjkUnifiedIdeographOne).isNotEqualTo(kangxiRadicalSymbolOne);

            // Radicals table seed uses U+4E00 (CJK Unified Ideograph), NOT U+2F00
            Radical radical1 = radicalRepository.findById(1).orElseThrow();
            assertThat(radical1.getCharacter().codePointAt(0)).isEqualTo(0x4E00);
        }

        @Test
        @DisplayName("GIVEN Kangxi Radical 9 '人(亻)' WHEN analyzing presentation string THEN identify primary ideograph and component variant")
        void testCompoundRadicalPresentationAnalysis() {
            Radical radical9 = radicalRepository.findById(9).orElseThrow();
            String charString = radical9.getCharacter();

            assertThat(charString).isEqualTo("人(亻)");

            int primaryChar = charString.codePointAt(0);
            int variantChar = charString.codePointAt(2); // index 0: '人', 1: '(', 2: '亻'

            // '人' is CJK Unified Ideograph U+4EBA
            assertThat(primaryChar).isEqualTo(0x4EBA);

            // '亻' is CJK Unified Ideograph U+4EBB (Standing person radical form)
            assertThat(variantChar).isEqualTo(0x4EBB);
        }

        @Test
        @DisplayName("GIVEN Kangxi Radical 157 '足(𧾷)' WHEN analyzing supplementary ideograph THEN verify 4-byte surrogate pair handling")
        void testSupplementaryIdeographSurrogatePair() {
            Radical radical157 = radicalRepository.findById(157).orElseThrow();
            String charString = radical157.getCharacter();

            assertThat(charString).startsWith("足(");

            // Extract the variant code point inside parentheses
            int openParen = charString.indexOf('(');
            int closeParen = charString.indexOf(')');
            String inside = charString.substring(openParen + 1, closeParen);

            // '𧾷' is CJK Unified Ideographs Extension B: U+27FB7 (Supplementary Plane 2, code point > 0xFFFF)
            int codePoint = inside.codePointAt(0);
            assertThat(codePoint).isEqualTo(0x27FB7);

            // In Java UTF-16, U+27FB7 occupies 2 char code units (surrogate pair)
            assertThat(inside.length()).isEqualTo(2);
            assertThat(Character.isSupplementaryCodePoint(codePoint)).isTrue();
        }
    }

    @Nested
    @DisplayName("4. Vocabulary Linguistic Domain & Relationship Integrity Audit")
    class VocabularyDomainAndRelationshipAuditTests {

        @Test
        @DisplayName("GIVEN vocabulary creation WHEN testing polysemy vs homophones THEN verify composite uniqueness on (hanzi, pinyin_raw)")
        void testVocabularyUniquenessAndPolysemy() {
            // Case 1: Same hanzi, DIFFERENT pinyin_raw -> Allowed (e.g. 行 xíng vs háng)
            Vocabulary vocab1 = new Vocabulary("行", "xíng", "xing", "Hành", "Đi, làm, thực hiện");
            Vocabulary vocab2 = new Vocabulary("行", "háng", "hang", "Hàng", "Hàng lối, ngân hàng");

            vocabularyRepository.save(vocab1);
            vocabularyRepository.save(vocab2);
            vocabularyRepository.flush();

            assertThat(vocab1.getVocabId()).isNotNull();
            assertThat(vocab2.getVocabId()).isNotNull();
            assertThat(vocab1.getVocabId()).isNotEqualTo(vocab2.getVocabId());

            // Case 2: Different hanzi, SAME pinyin_raw -> Allowed (homophones e.g. 他 tā vs 她 tā)
            Vocabulary vocab3 = new Vocabulary("他", "tā", "ta", "Tha", "Anh ấy");
            Vocabulary vocab4 = new Vocabulary("她", "tā", "ta", "Tha", "Cô ấy");

            vocabularyRepository.save(vocab3);
            vocabularyRepository.save(vocab4);
            vocabularyRepository.flush();

            assertThat(vocab3.getVocabId()).isNotNull();
            assertThat(vocab4.getVocabId()).isNotNull();
        }

        @Test
        @DisplayName("GIVEN vocabulary with linked radicals WHEN persisting VOCAB_RADICAL THEN verify composite PK and referential integrity")
        void testVocabRadicalRelationshipIntegrity() {
            Radical radHuman = radicalRepository.findById(9).orElseThrow(); // 人(亻)
            Radical radMouth = radicalRepository.findById(30).orElseThrow(); // 口

            Vocabulary vocab = new Vocabulary("你", "nǐ", "ni", "Nhĩ", "Bạn, anh, chị (ngôi thứ 2)");
            vocab.addRadical(radHuman);

            Vocabulary saved = vocabularyRepository.save(vocab);
            vocabularyRepository.flush();

            assertThat(saved.getRadicals()).hasSize(1);
            assertThat(saved.getRadicals()).contains(radHuman);

            // Add second radical
            saved.addRadical(radMouth);
            vocabularyRepository.save(saved);
            vocabularyRepository.flush();

            assertThat(saved.getRadicals()).hasSize(2);
            assertThat(saved.getRadicals()).contains(radHuman, radMouth);
        }

        @Test
        @DisplayName("GIVEN database schema WHEN auditing HSK metadata THEN prove HSK level column is NOT present in schema")
        void testHskMetadataNotImplemented() {
            // Documenting fact: HSK is not present in Vocabulary entity or database table
            // No getHskLevel() or setHskLevel() exists on Vocabulary class.
            boolean hasHskField = false;
            for (java.lang.reflect.Field field : Vocabulary.class.getDeclaredFields()) {
                if (field.getName().toLowerCase().contains("hsk")) {
                    hasHskField = true;
                    break;
                }
            }
            assertThat(hasHskField).isFalse();
        }
    }
}
