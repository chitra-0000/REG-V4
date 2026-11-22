package com.bnpp.regliss.repository.pagination;

import org.springframework.data.domain.Sort;

public class PaginationOptions {
    public int pageNumber = 1;
    public Integer pageSize;
    public Sort.Direction sortOrder = Sort.Direction.ASC;
    public String sortCol;

    public PaginationOptions(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /*USED BY JACKSON*/
    protected PaginationOptions(){}

    /*USED BY JACKSON for UI, UI angular libraries sends sort direction with lowercase letters.
    Adapt values to java naming conventions using JACKSON getter/setter deserialization for SortOrder
    */
    public String getSortOrder() {
        return sortOrder != null ? sortOrder.name().toLowerCase() : null;
    }
    public void setSortOrder(String sortOrder) {
        if (sortOrder != null ) {
            this.sortOrder = Sort.Direction.valueOf(sortOrder.toUpperCase());
        }
    }

    @Override
    public String toString() {
        return "page #" + pageNumber + " (of " + pageSize + " rows), sorted by " + sortCol + " " + sortOrder;
    }
}

