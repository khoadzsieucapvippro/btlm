package com.elearning;

import com.elearning.dto.request.CreateVocabularyRequest;
import com.elearning.dto.request.VocabularySearchCriteria;
import com.elearning.dto.response.VocabularyDetailResponse;
import com.elearning.dto.response.VocabularyResponse;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.VocabularyService;
import com.elearning.service.impl.ExcelParserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("Task R3.3A: Pinyin Normalization & Chinese Text Search Integrity Verification")
class PinyinNormalizationAndSearchIntegrityTests {

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private VocabularyService vocabularyService;

    @Nested
    @DisplayName("1. Umlaut ü / u / v & Disambiguation Collision Audit")
    class UmlautAndCollisionAuditTests {

        @Test
        @DisplayName("GIVEN lǜ (绿) and lù (路) WHEN saving to database THEN verify zero collision because hanzi differs")
        void testLuVsLvZeroCollision() {
            String pinyinRawLu1 = ExcelParserServiceImpl.toPinyinRaw("lǜ");
            String pinyinRawLu2 = ExcelParserServiceImpl.toPinyinRaw("lù");

            assertThat(pinyinRawLu1).isEqualTo("lu");
            assertThat(pinyinRawLu2).isEqualTo("lu");

            // Save 绿 (lǜ)
            Vocabulary vocabLv = new Vocabulary("绿", "lǜ", pinyinRawLu1, "Lục", "Màu xanh lá cây");
            Vocabulary savedLv = vocabularyRepository.save(vocabLv);

            // Save 路 (lù)
            Vocabulary vocabLu = new Vocabulary("路", "lù", pinyinRawLu2, "Lộ", "Con đường, đường đi");
            Vocabulary savedLu = vocabularyRepository.save(vocabLu);
            vocabularyRepository.flush();

            assertThat(savedLv.getVocabId()).isNotNull();
            assertThat(savedLu.getVocabId()).isNotNull();
            assertThat(savedLv.getVocabId()).isNotEqualTo(savedLu.getVocabId());
        }

        @Test
        @DisplayName("GIVEN nǚ (女) and nǔ (努) WHEN saving to database THEN verify zero collision")
        void testNuVsNvZeroCollision() {
            String pinyinRawNv = ExcelParserServiceImpl.toPinyinRaw("nǚ");
            String pinyinRawNu = ExcelParserServiceImpl.toPinyinRaw("nǔ");

            assertThat(pinyinRawNv).isEqualTo("nu");
            assertThat(pinyinRawNu).isEqualTo("nu");

            Vocabulary vocabNv = new Vocabulary("女", "nǚ", pinyinRawNv, "Nữ", "Phụ nữ, con gái");
            Vocabulary vocabNu = new Vocabulary("努", "nǔ", pinyinRawNu, "Nỗ", "Nỗ lực, cố gắng");

            vocabularyRepository.save(vocabNv);
            vocabularyRepository.save(vocabNu);
            vocabularyRepository.flush();

            assertThat(vocabNv.getVocabId()).isNotNull();
            assertThat(vocabNu.getVocabId()).isNotNull();
        }

        @Test
        @DisplayName("GIVEN exact duplicate (hanzi, pinyin_raw) WHEN saving THEN database rejects with conflict")
        void testExactDuplicateHanziAndPinyinRawConflict() {
            Vocabulary vocab1 = new Vocabulary("学习", "xuéxí", "xuexi", "Học tập", "Học tập, rèn luyện");
            vocabularyRepository.save(vocab1);
            vocabularyRepository.flush();

            // Attempt to create identical (hanzi, pinyin_raw) via Service
            CreateVocabularyRequest duplicateReq = new CreateVocabularyRequest(
                    "学习", "xuéxí", "xuexi", "Học tập", "Học tập"
            );

            assertThatThrownBy(() -> vocabularyService.createVocabulary(duplicateReq))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("đã tồn tại");
        }
    }

    @Nested
    @DisplayName("2. Pinyin Diacritics & Normalization Invariants")
    class PinyinNormalizationInvariantsTests {

        @Test
        @DisplayName("GIVEN all 4 tone variants of ü (ǖ, ǘ, ǚ, ǜ, ü) WHEN normalizing to pinyinRaw THEN all cleanly map to 'u'")
        void testAllUmlautTonesNormalization() {
            assertThat(ExcelParserServiceImpl.toPinyinRaw("lǖ")).isEqualTo("lu");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("lǘ")).isEqualTo("lu");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("lǚ")).isEqualTo("lu");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("lǜ")).isEqualTo("lu");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("lü")).isEqualTo("lu");

            assertThat(ExcelParserServiceImpl.toPinyinRaw("nǖ")).isEqualTo("nu");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("nǘ")).isEqualTo("nu");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("nǚ")).isEqualTo("nu");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("nǜ")).isEqualTo("nu");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("nü")).isEqualTo("nu");
        }

        @Test
        @DisplayName("GIVEN standard multi-syllable pinyin with spaces/capitals WHEN normalizing THEN preserve spaces and toneless lower-case")
        void testMultiSyllablePinyinNormalization() {
            assertThat(ExcelParserServiceImpl.toPinyinRaw("Túshūguǎn")).isEqualTo("tushuguan");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("Běijīng Dàxué")).isEqualTo("beijing daxue");
            assertThat(ExcelParserServiceImpl.toPinyinRaw("Zhōnghuá Rénmín Gònghéguó")).isEqualTo("zhonghua renmin gongheguo");
        }

        @Test
        @DisplayName("GIVEN CJK Unified Ideographs WHEN checking NFC vs NFD normalization THEN Hanzi is strictly invariant")
        void testHanziUnicodeNormalizationInvariance() {
            String originalHanzi = "中国人学习汉语漢字";

            String nfc = Normalizer.normalize(originalHanzi, Normalizer.Form.NFC);
            String nfd = Normalizer.normalize(originalHanzi, Normalizer.Form.NFD);
            String nfkc = Normalizer.normalize(originalHanzi, Normalizer.Form.NFKC);
            String nfkd = Normalizer.normalize(originalHanzi, Normalizer.Form.NFKD);

            assertThat(nfc).isEqualTo(originalHanzi);
            assertThat(nfd).isEqualTo(originalHanzi);
            assertThat(nfkc).isEqualTo(originalHanzi);
            assertThat(nfkd).isEqualTo(originalHanzi);
        }
    }

    @Nested
    @DisplayName("3. Chinese Text Search & Query Semantics")
    class ChineseSearchSemanticsTests {

        @Test
        @DisplayName("GIVEN vocabulary '学习' (xuéxí) WHEN searching by Hanzi, accented Pinyin, or toneless Pinyin THEN all return the record")
        void testSearchByDifferentRepresentations() {
            Vocabulary vocab = new Vocabulary("学习", "xuéxí", "xuexi", "Học tập", "Học tập kiến thức mới");
            vocabularyRepository.save(vocab);
            vocabularyRepository.flush();

            // 1. Search by Hanzi
            VocabularySearchCriteria critHanzi = new VocabularySearchCriteria();
            critHanzi.setSearch("学习");
            Page<VocabularyResponse> resHanzi = vocabularyService.searchVocabularies(critHanzi, PageRequest.of(0, 10));
            assertThat(resHanzi.getContent()).extracting(VocabularyResponse::getHanzi).contains("学习");

            // 2. Search by Accented Pinyin
            VocabularySearchCriteria critPinyin = new VocabularySearchCriteria();
            critPinyin.setSearch("xuéxí");
            Page<VocabularyResponse> resPinyin = vocabularyService.searchVocabularies(critPinyin, PageRequest.of(0, 10));
            assertThat(resPinyin.getContent()).extracting(VocabularyResponse::getHanzi).contains("学习");

            // 3. Search by Toneless PinyinRaw
            VocabularySearchCriteria critRaw = new VocabularySearchCriteria();
            critRaw.setSearch("xuexi");
            Page<VocabularyResponse> resRaw = vocabularyService.searchVocabularies(critRaw, PageRequest.of(0, 10));
            assertThat(resRaw.getContent()).extracting(VocabularyResponse::getHanzi).contains("学习");
        }

        @Test
        @DisplayName("GIVEN vocabulary '女' (nǚ) WHEN searching by 'nu' or 'nǚ' THEN find record cleanly")
        void testSearchUmlautVocabulary() {
            Vocabulary vocab = new Vocabulary("女", "nǚ", "nu", "Nữ", "Phụ nữ");
            vocabularyRepository.save(vocab);
            vocabularyRepository.flush();

            // Search by 'nu'
            VocabularySearchCriteria critNu = new VocabularySearchCriteria();
            critNu.setSearch("nu");
            Page<VocabularyResponse> resNu = vocabularyService.searchVocabularies(critNu, PageRequest.of(0, 10));
            assertThat(resNu.getContent()).extracting(VocabularyResponse::getHanzi).contains("女");

            // Search by 'nǚ'
            VocabularySearchCriteria critNv = new VocabularySearchCriteria();
            critNv.setSearch("nǚ");
            Page<VocabularyResponse> resNv = vocabularyService.searchVocabularies(critNv, PageRequest.of(0, 10));
            assertThat(resNv.getContent()).extracting(VocabularyResponse::getHanzi).contains("女");
        }
    }
}
