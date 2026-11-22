package com.bnpp.regliss.repository.pagination;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.function.Function;

public class PaginatedResults<T> {
    public final List<T> data;
    public final long totalItems;
    private final Long distinctRecordCount;

    public PaginatedResults(List<T> data,long totalItems, Long distinctRecordCount) {
        this.data = data;
        this.totalItems=totalItems;
        this.distinctRecordCount = distinctRecordCount;

    }
    public PaginatedResults(List<T> data, long totalItems) {
        this(data, totalItems, null);
    }

    public PaginatedResults(List<T> data, int totalItems) {
        this(data, (long)totalItems, null);
    }

    public static <U> PaginatedResults<U> empty() {
        return new PaginatedResults<U> (emptyList(), 0);
    }

    public <U> PaginatedResults<U> map(Function<T, U> mapperFunction) {
        return new PaginatedResults<U>(data.stream().map(mapperFunction).collect(toList()), totalItems, distinctRecordCount);
    }

}

