package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.DJExportFilter;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.List;

public interface DJExportFilterRepository extends EntityRepository<DJExportFilter, Long> {
    List<DJExportFilter> findByActiveTrue();
    boolean existsDJExportFilterByFilterTagAndDeletedFalse(String filterTag);

    List<DJExportFilter> getFiltersByListIdAndDeletedFalse(Long listId);
}

