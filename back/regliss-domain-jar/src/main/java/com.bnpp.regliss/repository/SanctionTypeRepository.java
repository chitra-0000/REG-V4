package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.record.SanctionType;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.List;

public interface SanctionTypeRepository extends EntityRepository<SanctionType, Long> {

    List<SanctionType> findByLabel(String label);
}

