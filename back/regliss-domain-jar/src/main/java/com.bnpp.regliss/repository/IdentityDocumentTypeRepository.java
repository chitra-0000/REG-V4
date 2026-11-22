package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.IdentityDocumentType;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.List;

public interface IdentityDocumentTypeRepository extends EntityRepository<IdentityDocumentType, Long> {
    List<IdentityDocumentType> findByBusinessConcept(ReferentialBusinessConcept businessConcept);

}

