package com.bnpp.regliss.repository;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.ChildType;
import com.bnpp.regliss.entity.app.RecordVerificationChanges;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.List;
import java.util.Optional;

public interface RecordVerificationChangesRepository extends EntityRepository<RecordVerificationChanges, Long> {

    Optional<RecordVerificationChanges> findByVersionIdAndRecordIdAndChildIdAndChildTypeAndAuditedAction(
            Long versionId,
            Long recordId,
            Long childId,
            ChildType childType,
            AuditedAction auditedAction
    );

    List<RecordVerificationChanges> findByVersionId(Long versionId);

}
