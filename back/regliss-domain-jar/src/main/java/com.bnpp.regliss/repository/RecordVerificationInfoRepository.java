package com.bnpp.regliss.repository;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.ChildType;
import com.bnpp.regliss.entity.app.RecordVerificationInfo;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.Optional;

public interface RecordVerificationInfoRepository extends EntityRepository<RecordVerificationInfo, Long> {
    Optional<RecordVerificationInfo> findByVersionIdAndRecordIdAndChildIdAndChildTypeAndAuditedAction(
        Long versionId,
        Long recordId,
        Long childId,
        ChildType childType,
        AuditedAction auditedAction
    );
}

