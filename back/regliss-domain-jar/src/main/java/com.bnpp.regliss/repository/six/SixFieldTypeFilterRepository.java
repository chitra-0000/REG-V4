package com.bnpp.regliss.repository.six;

import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.entity.six.entity.SixFieldTypeFilter;

import java.util.List;

public interface SixFieldTypeFilterRepository extends EntityRepository<SixFieldTypeFilter, Long> {

    List<SixFieldTypeFilter> findAllByOrderByFilterType();

}

