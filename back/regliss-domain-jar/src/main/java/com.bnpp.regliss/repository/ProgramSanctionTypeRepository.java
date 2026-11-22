package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.ProgramSanctionType;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.List;

public interface ProgramSanctionTypeRepository extends EntityRepository<ProgramSanctionType, Long> {

    ProgramSanctionType findByLabel(String label);

    List<ProgramSanctionType> findByBusinessConcept(ReferentialBusinessConcept programOfacProgramId);
}

