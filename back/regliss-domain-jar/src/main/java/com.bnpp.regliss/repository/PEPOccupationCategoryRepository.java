package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.record.PEPOccupationCategory;
import com.bnpp.regliss.repository.base.EntityRepository;

public interface PEPOccupationCategoryRepository extends EntityRepository<PEPOccupationCategory, Long> {

    PEPOccupationCategory findByLabel(String code);
}

