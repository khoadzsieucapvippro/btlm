package com.elearning.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Helper DTO representing a vocabulary item and its target display order in a reorder request.
 */
public class VocabOrderItem {

    @NotNull(message = "ID từ vựng không được để trống")
    @Positive(message = "ID từ vựng phải là số nguyên dương")
    private Long vocabId;

    @NotNull(message = "Thứ tự hiển thị không được để trống")
    @Min(value = 0, message = "Thứ tự hiển thị không được âm")
    private Integer orderIndex;

    public VocabOrderItem() {
    }

    public VocabOrderItem(Long vocabId, Integer orderIndex) {
        this.vocabId = vocabId;
        this.orderIndex = orderIndex;
    }

    public Long getVocabId() {
        return vocabId;
    }

    public void setVocabId(Long vocabId) {
        this.vocabId = vocabId;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
