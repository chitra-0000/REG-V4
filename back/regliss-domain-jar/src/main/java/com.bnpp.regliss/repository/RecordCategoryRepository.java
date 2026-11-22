package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.List;

public interface RecordCategoryRepository extends EntityRepository<RecordCategory, Long> {

    RecordCategory findByLabel(String label);

    List<RecordCategory> findByBusinessConcept(ReferentialBusinessConcept businessConcept);

}

