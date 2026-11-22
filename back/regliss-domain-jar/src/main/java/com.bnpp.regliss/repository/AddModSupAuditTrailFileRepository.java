package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.AddModSupAuditTrailFile;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AddModSupAuditTrailFileRepository extends EntityRepository<AddModSupAuditTrailFile, Long> {

    @Query("SELECT a1 FROM AddModSupAuditTrailFile a1 WHERE a1.generationDate = (SELECT max(a2.generationDate) FROM AddModSupAuditTrailFile a2 WHERE a2.version.id =?1 and a2.deprecated = false)")
    Optional<AddModSupAuditTrailFile>  findLatestNonDeprecatedByVersionId(Long id);

    @Query("SELECT a1 FROM AddModSupAuditTrailFile a1 WHERE a1.generationDate = (SELECT max(a2.generationDate) FROM AddModSupAuditTrailFile a2 WHERE a2.version.id =?1 and a2.generationDate < ?2)")
    Optional<AddModSupAuditTrailFile>  getPreviousAddModSupAuditTrailFileByGenerationDate(Long id, LocalDateTime generationDate);

}

