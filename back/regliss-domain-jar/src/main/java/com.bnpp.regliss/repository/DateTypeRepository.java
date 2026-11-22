package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.DateType;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.List;

public interface DateTypeRepository extends EntityRepository<DateType, Long> {

    DateType findByLabel(String label);

    List<DateType> findByBusinessConcept(ReferentialBusinessConcept businessConcept);

    List<DateType> findByBusinessConceptAndV3Only(ReferentialBusinessConcept businessConcept, boolean v3Only);

}

