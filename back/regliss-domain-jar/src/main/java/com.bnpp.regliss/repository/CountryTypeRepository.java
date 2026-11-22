package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.CountryType;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.List;

public interface CountryTypeRepository extends EntityRepository<CountryType, Long> {

    CountryType findByLabel(String label);

    List<CountryType> findByBusinessConcept(ReferentialBusinessConcept businessConcept);

}

