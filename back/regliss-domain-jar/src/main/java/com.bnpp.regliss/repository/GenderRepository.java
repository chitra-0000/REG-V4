package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.record.Gender;
import com.bnpp.regliss.repository.base.EntityRepository;

public interface GenderRepository extends EntityRepository<Gender, Long>{

    Gender findByLabel(String label);
}

