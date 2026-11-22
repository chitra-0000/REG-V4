package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.SubstitutionMatrixElement;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubstitutionMatrixElementRepository extends EntityRepository<SubstitutionMatrixElement, Long>{

    List<SubstitutionMatrixElement> findByFileFormat(FileExportFormat fileFormat);

    void deleteByFileFormat(FileExportFormat fileFormat);

    @Query("SELECT e FROM SubstitutionMatrixElement e")
    List<SubstitutionMatrixElement> getAllSubstitutionMatrix();
}

