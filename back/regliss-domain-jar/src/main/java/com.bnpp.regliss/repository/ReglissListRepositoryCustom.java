package com.bnpp.regliss.repository;

import java.util.List;
import java.util.Set;

import com.bnpp.regliss.search.ReglissListSearchCriteria;
import com.bnpp.regliss.search.ReglissListSearchResult;


public interface ReglissListRepositoryCustom {
    List<ReglissListSearchResult> searchOptimized(ReglissListSearchCriteria criteria, Set<Long> visibleListIds);
}

