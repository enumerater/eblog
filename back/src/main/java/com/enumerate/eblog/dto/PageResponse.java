package com.enumerate.eblog.dto;

import java.util.List;

/**
 * 通用分页响应
 */
public class PageResponse<T> {

    /** 当前页数据 */
    private List<T> content;

    /** 当前页码（从 0 开始） */
    private int page;

    /** 每页大小 */
    private int size;

    /** 总记录数 */
    private long totalElements;

    /** 总页数 */
    private int totalPages;

    /** 是否有下一页 */
    private boolean hasNext;

    /** 是否有上一页 */
    private boolean hasPrevious;

    public PageResponse() {}

    public PageResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
        this.hasNext = page + 1 < totalPages;
        this.hasPrevious = page > 0;
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }

    public boolean isHasPrevious() { return hasPrevious; }
    public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
}