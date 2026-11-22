package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.record.Description3;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.List;

public interface Description3Repository extends EntityRepository<Description3, Long> {

    List<Description3> findByLabel(String label);
}

