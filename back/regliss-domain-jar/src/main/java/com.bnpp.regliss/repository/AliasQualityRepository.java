package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.AliasQuality;
import com.bnpp.regliss.repository.base.EntityRepository;

public interface AliasQualityRepository extends EntityRepository<AliasQuality, Long> {
    AliasQuality findByBusinessConcept(ReferentialBusinessConcept businessConcept);
}

