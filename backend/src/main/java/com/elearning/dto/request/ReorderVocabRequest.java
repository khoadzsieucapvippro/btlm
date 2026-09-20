package com.elearning.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Request DTO for reordering vocabulary items within a lesson (PUT /api/v1/creator/lessons/{id}/reorder).
 * Accepts either:
 * - A simple ordered list of vocab IDs (orderedVocabIds), or
 * - A structured list of items (items with vocabId and orderIndex).
 */
public class ReorderVocabRequest {

    private List<@NotNull(message = "ID từ vựng không được để trống") @Positive(message = "ID từ vựng phải là số nguyên dương") Long> orderedVocabIds;

    @Valid
    private List<@NotNull(message = "Phần tử sắp xếp không được để trống") @Valid VocabOrderItem> items;

    public ReorderVocabRequest() {
    }

    public ReorderVocabRequest(List<Long> orderedVocabIds) {
        this.orderedVocabIds = orderedVocabIds;
    }

    public static ReorderVocabRequest of(List<Long> orderedVocabIds) {
        return new ReorderVocabRequest(orderedVocabIds);
    }

    public static ReorderVocabRequest ofItems(List<VocabOrderItem> items) {
        ReorderVocabRequest request = new ReorderVocabRequest();
        request.setItems(items);
        return request;
    }

    /**
     * Resolves the canonical list of vocabulary IDs in the desired order.
     */
    @JsonIgnore
    public List<Long> getResolvedVocabIds() {
        if (orderedVocabIds != null && !orderedVocabIds.isEmpty()) {
            return orderedVocabIds;
        }
        if (items != null && !items.isEmpty()) {
            List<VocabOrderItem> sortedItems = new ArrayList<>(items);
            sortedItems.sort(Comparator.comparing(VocabOrderItem::getOrderIndex));
            return sortedItems.stream().map(VocabOrderItem::getVocabId).toList();
        }
        return List.of();
    }

    public List<Long> getOrderedVocabIds() {
        return orderedVocabIds;
    }

    public void setOrderedVocabIds(List<Long> orderedVocabIds) {
        this.orderedVocabIds = orderedVocabIds;
    }

    public List<VocabOrderItem> getItems() {
        return items;
    }

    public void setItems(List<VocabOrderItem> items) {
        this.items = items;
    }
}
