package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.AddModSupExportFile;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AddModSupExportFileRepository extends EntityRepository<AddModSupExportFile, Long> {

    @Query("SELECT f FROM AddModSupExportFile f WHERE  f.id =?1")
    Optional<AddModSupExportFile> getVersionSynthesisFileById(Long id);
}

