package com.bnpp.regliss.search;

import com.bnpp.regliss.repository.pagination.PaginationOptions;

public class SubscriberSearchCriteria {

    public String name;
    public String flow;
    public String pole;

    public PaginationOptions paginationOptions = new PaginationOptions(15);
}
