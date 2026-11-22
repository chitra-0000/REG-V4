package com.bnpp.regliss.service;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.repository.AuditRepository;
import com.bnpp.regliss.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReglementationAuditService {
    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private LogRepository logRepo;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ConsultedEntitiesInSession session;

    public void auditAddReglementaryTextList(Reglementation reglementation) {
        Audit audit = ReflectionComparator.createAddInCollectionAudit(reglementation, AuditedAction.LIST_REGLEMENTATION_CREATION);
        ReglissList list = reglementation.getListOpt()
                .orElseThrow(() -> new IllegalArgumentException("List should be present for reglementation"));
        fill(audit, ObjectType.LIST, list.getReference(), list.getId());
        audit.setChildId(reglementation.getId());
        auditRepo.save(audit);
    }

    public void auditAddReglementaryTextVersion(Reglementation reglementation, Version version) {
        Audit audit = ReflectionComparator.createAddInCollectionAudit(reglementation, AuditedAction.VERSION_REGLEMENTATION_CREATION);
        fill(audit, ObjectType.VERSION, version.computeVersionReference(), version.getId());
        audit.setChildId(reglementation.getId());
        auditRepo.save(audit);
    }

    public void auditUpdateReglementaryTextList(Reglementation oldReglementation, Reglementation newReglementation) {
        List<Audit> audits = ReflectionComparator.createModifyInCollectionAudit(oldReglementation, newReglementation, AuditedAction.LIST_REGLEMENTATION_MODIFICATION);
        ReglissList list = newReglementation.getListOpt()
                .orElseThrow(() -> new IllegalArgumentException("List should be present for reglementation"));
        for (Audit audit : audits) {
            fill(audit, ObjectType.LIST, list.getReference(), list.getId());
            audit.setChildId(oldReglementation.getId());
            auditRepo.save(audit);
        }
    }

    public void auditUpdateReglementaryTextVersion(Reglementation oldReglementation, Reglementation newReglementation, Version version) {
        List<Audit> audits = ReflectionComparator.createModifyInCollectionAudit(oldReglementation, newReglementation, AuditedAction.VERSION_REGLEMENTATION_MODIFICATION);
        for (Audit audit : audits) {
            fill(audit, ObjectType.VERSION, version.computeVersionReference(), version.getId());
            audit.setChildId(oldReglementation.getId());
            auditRepo.save(audit);
        }
    }


    public void auditDeleteReglementaryTextVersion(Reglementation reglementation, Version version) {
        Audit audit = ReflectionComparator.createDeleteFromCollectionAudit(reglementation, AuditedAction.VERSION_REGLEMENTATION_DELETION);
        fill(audit,ObjectType.VERSION, version.computeVersionReference(), version.getId());
        audit.setChildId(reglementation.getId());
        auditRepo.save(audit);
    }

    public void auditDeleteReglementaryTextList(Reglementation reglementation, ReglissList list) {
        Audit audit = ReflectionComparator.createDeleteFromCollectionAudit(reglementation, AuditedAction.LIST_REGLEMENTATION_DELETION);
        fill(audit, ObjectType.LIST, list.getReference(), list.getId());
        audit.setChildId(reglementation.getId());
        auditRepo.save(audit);
    }

    private <T extends LogDetail> void fill(T audit, ObjectType objectType, String reference, Long id){
        audit.setLogDate(requestContext.getRequestTime());
        audit.setUser(requestContext.getCurrentUserRef());
        audit.setObjectType(objectType);
        audit.setReference(reference);
        audit.setObjectId(id);
    }
}
