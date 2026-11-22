package com.bnpp.regliss.repository;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.ObjectType;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditRepository extends EntityRepository<Audit, Long>, AuditRepositoryCustom {

    List<Audit> findAllByObjectIdAndObjectTypeOrderByIdDesc(Long objectId, ObjectType objectType);

    List<Audit> findAllByObjectIdAndObjectType(Long objectId, ObjectType objectType);

    List<Audit> findAllByObjectIdAndObjectTypeAndAuditedActionInOrderByIdDesc(Long objectId, ObjectType objectType,
            List<AuditedAction> historyActions);

    List<Audit> findAllByObjectIdAndObjectTypeAndVersionOrderByIdDesc(Long objectId, ObjectType objectType, Version version);

    List<Audit> findAllByReferenceOrderByIdDesc(String reference);

    @Modifying
    @Query("DELETE FROM Audit a WHERE  a.version.id =?1 OR (a.objectType = 'VERSION' AND a.objectId = ?1)")
    void deleteAllAuditsForVersion(Long versionId);

    @Modifying
    @Query("DELETE FROM Audit a WHERE  a.version.id =?1 AND a.objectType = 'RECORD'")
    void deleteAllAuditsForTheRecordsOfVersion(Long versionId);
    
    List<Audit> findAllByVersionId(long versionId);

    Audit findTop1ByObjectIdAndObjectTypeAndAuditedAction(Long objectId, ObjectType objectType,AuditedAction auditedActions);
}

