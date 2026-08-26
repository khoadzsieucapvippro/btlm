package com.elearning.dto.response;

import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Standard Pagination Response structure matching .agents/API.md section 1.3.
 *
 * @param <T> Item element type
 */
public class PageResponse<T> {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<T> items = new ArrayList<>();

    public PageResponse() {
        this.items = new ArrayList<>();
    }

    public PageResponse(int page, int size, long totalElements, int totalPages, List<T> items) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.items = items != null ? items : new ArrayList<>();
    }

    public static <T> PageResponse<T> from(Page<T> springPage) {
        if (springPage == null) {
            return new PageResponse<>(0, 0, 0L, 0, Collections.emptyList());
        }
        return new PageResponse<>(
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.getContent() != null ? springPage.getContent() : Collections.emptyList()
        );
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "PageResponse{" +
                "page=" + page +
                ", size=" + size +
                ", totalElements=" + totalElements +
                ", totalPages=" + totalPages +
                ", items=" + items +
                '}';
    }
}
