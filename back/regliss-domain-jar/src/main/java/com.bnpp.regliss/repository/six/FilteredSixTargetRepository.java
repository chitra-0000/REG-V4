package com.bnpp.regliss.repository.six;

import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.entity.six.entity.FilteredSixTarget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilteredSixTargetRepository extends EntityRepository<FilteredSixTarget, Long>, JpaRepository<FilteredSixTarget, Long> {
}


