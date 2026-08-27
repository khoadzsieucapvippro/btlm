package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.response.RadicalDetailResponse;
import com.elearning.dto.response.RadicalResponse;
import com.elearning.exception.BusinessException;
import com.elearning.service.RadicalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("Task 4A.1: RadicalService End-to-End Integration Tests (with real 214 Kangxi Radicals)")
class RadicalServiceIntegrationTests {

    @Autowired
    private RadicalService radicalService;

    @Test
    @DisplayName("GIVEN database seeded with 214 radicals WHEN getAllRadicals THEN returns exactly 214 in canonical order")
    void testGetAllRadicals214CountAndOrder() {
        List<RadicalResponse> radicals = radicalService.getAllRadicals();

        assertThat(radicals).isNotNull();
        assertThat(radicals).hasSize(214);

        // Verify canonical Kangxi ordering (1 to 214)
        for (int i = 0; i < radicals.size(); i++) {
            assertThat(radicals.get(i).getRadicalId()).isEqualTo(i + 1);
        }

        // Verify first radical (radical_id = 1)
        RadicalResponse first = radicals.get(0);
        assertThat(first.getRadicalId()).isEqualTo(1);
        assertThat(first.getCharacter()).isEqualTo("一");
        assertThat(first.getPinyin()).isEqualTo("yī");
        assertThat(first.getMeaningHanViet()).isEqualTo("Nhất");

        // Verify 214th radical (radical_id = 214)
        RadicalResponse last = radicals.get(213);
        assertThat(last.getRadicalId()).isEqualTo(214);
        assertThat(last.getCharacter()).isEqualTo("龠");
        assertThat(last.getMeaningHanViet()).isEqualTo("Dược");
    }

    @Test
    @DisplayName("GIVEN database seeded with 214 radicals WHEN getAllRadicals with Pageable THEN returns paginated results")
    void testGetAllRadicalsPagination() {
        Page<RadicalResponse> page0 = radicalService.getAllRadicals(PageRequest.of(0, 20));

        assertThat(page0).isNotNull();
        assertThat(page0.getTotalElements()).isEqualTo(214);
        assertThat(page0.getTotalPages()).isEqualTo(11);
        assertThat(page0.getNumber()).isZero();
        assertThat(page0.getSize()).isEqualTo(20);
        assertThat(page0.getContent()).hasSize(20);
        assertThat(page0.getContent().get(0).getCharacter()).isEqualTo("一");

        Page<RadicalResponse> page10 = radicalService.getAllRadicals(PageRequest.of(10, 20));
        assertThat(page10.getNumber()).isEqualTo(10);
        assertThat(page10.getContent()).hasSize(14); // 214 - 200 = 14
        assertThat(page10.getContent().get(13).getRadicalId()).isEqualTo(214);
        assertThat(page10.getContent().get(13).getCharacter()).isEqualTo("龠");
    }

    @Test
    @DisplayName("GIVEN existing radical ID WHEN getRadicalById THEN returns detailed response from database")
    void testGetRadicalByIdRealDb() {
        RadicalDetailResponse detail = radicalService.getRadicalById(1);

        assertThat(detail).isNotNull();
        assertThat(detail.getRadicalId()).isEqualTo(1);
        assertThat(detail.getCharacter()).isEqualTo("一");
        assertThat(detail.getPinyin()).isEqualTo("yī");
        assertThat(detail.getMeaningHanViet()).isEqualTo("Nhất");
        assertThat(detail.getCreatedAt()).isNotNull();
        assertThat(detail.getUpdatedAt()).isNotNull();
        assertThat(detail.getRelatedVocabularies()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN existing radical character WHEN getRadicalByCharacter THEN returns detailed response from database")
    void testGetRadicalByCharacterRealDb() {
        RadicalDetailResponse detail = radicalService.getRadicalByCharacter("一");

        assertThat(detail).isNotNull();
        assertThat(detail.getRadicalId()).isEqualTo(1);
        assertThat(detail.getCharacter()).isEqualTo("一");
        assertThat(detail.getMeaningHanViet()).isEqualTo("Nhất");
    }

    @Test
    @DisplayName("GIVEN nonexistent ID WHEN getRadicalById THEN throws NOT_FOUND BusinessException")
    void testGetRadicalByIdNotFoundRealDb() {
        assertThatThrownBy(() -> radicalService.getRadicalById(9999))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("GIVEN nonexistent character WHEN getRadicalByCharacter THEN throws NOT_FOUND BusinessException")
    void testGetRadicalByCharacterNotFoundRealDb() {
        assertThatThrownBy(() -> radicalService.getRadicalByCharacter("龍"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("GIVEN radical ID WHEN getRelatedVocabularies THEN returns empty list for seeded radicals")
    void testGetRelatedVocabulariesRealDb() {
        List<RadicalDetailResponse.RelatedVocabularyDto> related = radicalService.getRelatedVocabularies(1);

        assertThat(related).isNotNull();
        assertThat(related).isEmpty();
    }
}
