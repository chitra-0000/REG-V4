package com.bnpp.regliss.search;

import com.bnpp.regliss.repository.pagination.PaginationOptions;

public class UserSearchCriteria {

    public String username;
    public String firstName;
    public String lastName;
    public Long countryId;
    public String pole;
    public String entity;
    public String businessLine;
    public String listName;
    public PaginationOptions paginationOptions = new PaginationOptions(15);

}
