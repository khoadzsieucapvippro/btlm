package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.VocabularySearchCriteria;
import com.elearning.dto.response.VocabularyDetailResponse;
import com.elearning.dto.response.VocabularyResponse;
import com.elearning.entity.Radical;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.RadicalRepository;
import com.elearning.repository.VocabularyRepository;
import com.elearning.service.VocabularyService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("Task 4B.1: VocabularyService End-to-End Integration Tests (MySQL + Real Radicals)")
class VocabularyServiceIntegrationTests {

    @Autowired
    private VocabularyService vocabularyService;

    @Autowired
    private VocabularyRepository vocabularyRepository;

    @Autowired
    private RadicalRepository radicalRepository;

    @Autowired
    private EntityManager entityManager;

    private Vocabulary vocabXue;
    private Vocabulary vocabXiu;
    private Vocabulary vocabZhong;

    @BeforeEach
    void setUpData() {
        // Find existing seeded Kangxi radicals: ID 9 ('人') and ID 75 ('木')
        Radical radicalRen = radicalRepository.findById(9).orElseThrow();
        Radical radicalMu = radicalRepository.findById(75).orElseThrow();
        Radical radicalZi = radicalRepository.findById(39).orElseThrow(); // '子'

        // Vocab 1: 学 (xué / xue) associated with radical '子' (ID 39)
        vocabXue = new Vocabulary("学", "xué", "xue", "Học", "Học tập, học vấn");
        vocabXue.setExampleSentence("他在北京大学学习汉语。");
        vocabXue.setExampleTranslation("Anh ấy đang học tiếng Trung ở Đại học Bắc Kinh.");
        vocabXue.addRadical(radicalZi);
        entityManager.persist(vocabXue);

        // Vocab 2: 休 (xiū / xiu) associated with BOTH radical '人' (ID 9) AND '木' (ID 75)
        vocabXiu = new Vocabulary("休", "xiū", "xiu", "Hưu", "Nghỉ ngơi, thôi, ngừng");
        vocabXiu.setExampleSentence("我今天休息。");
        vocabXiu.setExampleTranslation("Hôm nay tôi nghỉ ngơi.");
        vocabXiu.addRadical(radicalRen);
        vocabXiu.addRadical(radicalMu);
        entityManager.persist(vocabXiu);

        // Vocab 3: 中国 (zhōngguó / zhongguo) no radicals linked
        vocabZhong = new Vocabulary("中国", "zhōngguó", "zhongguo", "Trung Quốc", "Nước Trung Quốc");
        entityManager.persist(vocabZhong);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("GIVEN no criteria WHEN searchVocabularies THEN returns paginated vocabularies")
    void testSearchWithoutCriteriaReturnsAll() {
        Page<VocabularyResponse> result = vocabularyService.searchVocabularies(null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(result.getContent()).extracting(VocabularyResponse::getHanzi)
                .contains("学", "休", "中国");
    }

    @Test
    @DisplayName("GIVEN hanzi search WHEN searchVocabularies THEN matches exact and substring words")
    void testSearchByHanzi() {
        // Substring search "中" should match "中国"
        VocabularySearchCriteria criteria = new VocabularySearchCriteria();
        criteria.setHanzi("中");

        Page<VocabularyResponse> result = vocabularyService.searchVocabularies(criteria, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(VocabularyResponse::getHanzi)
                .containsExactly("中国");
    }

    @Test
    @DisplayName("GIVEN pinyin search with tone marks WHEN searchVocabularies THEN matches accented pinyin")
    void testSearchByPinyinAccented() {
        VocabularySearchCriteria criteria = new VocabularySearchCriteria();
        criteria.setPinyin("xiū");

        Page<VocabularyResponse> result = vocabularyService.searchVocabularies(criteria, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(VocabularyResponse::getHanzi)
                .containsExactly("休");
    }

    @Test
    @DisplayName("GIVEN pinyinRaw toneless search WHEN searchVocabularies THEN matches via shadow column pinyin_raw")
    void testSearchByPinyinRawToneless() {
        VocabularySearchCriteria criteria = new VocabularySearchCriteria();
        criteria.setPinyinRaw("xue");

        Page<VocabularyResponse> result = vocabularyService.searchVocabularies(criteria, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(VocabularyResponse::getHanzi)
                .containsExactly("学");
    }

    @Test
    @DisplayName("GIVEN radicalId filter WHEN searchVocabularies THEN returns matching vocabularies without duplicates")
    void testSearchByRadicalIdNoDuplicates() {
        // Filter by radical 9 ('人') -> should match '休'
        VocabularySearchCriteria criteria = new VocabularySearchCriteria();
        criteria.setRadicalId(9);

        Page<VocabularyResponse> result = vocabularyService.searchVocabularies(criteria, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getHanzi()).isEqualTo("休");
    }

    @Test
    @DisplayName("GIVEN nonexistent radicalId WHEN searchVocabularies THEN returns empty page")
    void testSearchByNonexistentRadicalId() {
        VocabularySearchCriteria criteria = new VocabularySearchCriteria();
        criteria.setRadicalId(214); // 龠 (none of our test vocabs have this)

        Page<VocabularyResponse> result = vocabularyService.searchVocabularies(criteria, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN combined criteria (hanzi + radicalId) WHEN searchVocabularies THEN enforces both filters (AND logic)")
    void testSearchByCombinedCriteria() {
        // '休' has radical 75 ('木') -> matches
        VocabularySearchCriteria matchingCriteria = new VocabularySearchCriteria();
        matchingCriteria.setHanzi("休");
        matchingCriteria.setRadicalId(75);

        Page<VocabularyResponse> matchResult = vocabularyService.searchVocabularies(matchingCriteria, PageRequest.of(0, 10));
        assertThat(matchResult.getContent()).extracting(VocabularyResponse::getHanzi)
                .containsExactly("休");

        // '学' does NOT have radical 75 ('木') -> empty
        VocabularySearchCriteria nonMatchingCriteria = new VocabularySearchCriteria();
        nonMatchingCriteria.setHanzi("学");
        nonMatchingCriteria.setRadicalId(75);

        Page<VocabularyResponse> nonMatchResult = vocabularyService.searchVocabularies(nonMatchingCriteria, PageRequest.of(0, 10));
        assertThat(nonMatchResult.getTotalElements()).isZero();
        assertThat(nonMatchResult.getContent()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN unified search keyword WHEN searchVocabularies THEN matches across hanzi, pinyin, or pinyin_raw")
    void testSearchByUnifiedKeyword() {
        // Keyword matches pinyin_raw "zhongguo"
        VocabularySearchCriteria criteria1 = VocabularySearchCriteria.byKeyword("zhong");
        Page<VocabularyResponse> res1 = vocabularyService.searchVocabularies(criteria1, PageRequest.of(0, 10));
        assertThat(res1.getContent()).extracting(VocabularyResponse::getHanzi).contains("中国");

        // Keyword matches accented pinyin "xué"
        VocabularySearchCriteria criteria2 = VocabularySearchCriteria.byKeyword("xué");
        Page<VocabularyResponse> res2 = vocabularyService.searchVocabularies(criteria2, PageRequest.of(0, 10));
        assertThat(res2.getContent()).extracting(VocabularyResponse::getHanzi).contains("学");

        // Keyword matches Hanzi "休"
        VocabularySearchCriteria criteria3 = VocabularySearchCriteria.byKeyword("休");
        Page<VocabularyResponse> res3 = vocabularyService.searchVocabularies(criteria3, PageRequest.of(0, 10));
        assertThat(res3.getContent()).extracting(VocabularyResponse::getHanzi).contains("休");
    }

    @Test
    @DisplayName("GIVEN existing vocabId WHEN getVocabularyById THEN returns detail with associated radicals")
    void testGetVocabularyByIdSuccess() {
        VocabularyDetailResponse detail = vocabularyService.getVocabularyById(vocabXiu.getVocabId());

        assertThat(detail).isNotNull();
        assertThat(detail.getVocabId()).isEqualTo(vocabXiu.getVocabId());
        assertThat(detail.getHanzi()).isEqualTo("休");
        assertThat(detail.getPinyin()).isEqualTo("xiū");
        assertThat(detail.getPinyinRaw()).isEqualTo("xiu");
        assertThat(detail.getMeaningHanViet()).isEqualTo("Hưu");
        assertThat(detail.getMeaningVi()).isEqualTo("Nghỉ ngơi, thôi, ngừng");
        assertThat(detail.getCreatedAt()).isNotNull();
        assertThat(detail.getUpdatedAt()).isNotNull();

        // Radicals should be sorted by radicalId: 9 ('人(亻)') before 75 ('木')
        assertThat(detail.getRadicals()).hasSize(2);
        assertThat(detail.getRadicals().get(0).getRadicalId()).isEqualTo(9);
        assertThat(detail.getRadicals().get(0).getCharacter()).isEqualTo("人(亻)");
        assertThat(detail.getRadicals().get(1).getRadicalId()).isEqualTo(75);
        assertThat(detail.getRadicals().get(1).getCharacter()).isEqualTo("木");
    }

    @Test
    @DisplayName("GIVEN nonexistent vocabId WHEN getVocabularyById THEN throws BusinessException NOT_FOUND")
    void testGetVocabularyByIdNotFound() {
        assertThatThrownBy(() -> vocabularyService.getVocabularyById(999999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("GIVEN null vocabId WHEN getVocabularyById THEN throws BusinessException VALIDATION_ERROR")
    void testGetVocabularyByIdNull() {
        assertThatThrownBy(() -> vocabularyService.getVocabularyById(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                });
    }
}
