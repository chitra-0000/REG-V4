package com.bnpp.regliss.search;

import com.bnpp.regliss.entity.ReglissListType;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.Scope;
import com.bnpp.regliss.repository.pagination.PaginationOptions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReglissListSearchCriteria {

    public String name;
    public String reference;
    public List<ReglissListType> type = new ArrayList<>();
    public Long regulatorOrInternalPolicyId;
    public Long countryId;
    public List<Scope> scopeId = new ArrayList<>();
    public List<Long> categoryIds = new ArrayList<>();
    public List<Confidentiality> confidentialityId = new ArrayList<>();
    public PaginationOptions paginationOptions = new PaginationOptions(15);
    public boolean authenticatedLowLevel;
    public LocalDate publicationDateFrom;
    public LocalDate publicationDateTo;

}
