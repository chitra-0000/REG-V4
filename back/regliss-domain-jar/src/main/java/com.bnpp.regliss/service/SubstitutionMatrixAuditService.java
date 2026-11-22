package com.bnpp.regliss.service;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.LogDetail;
import com.bnpp.regliss.entity.ObjectType;
import com.bnpp.regliss.entity.SubstitutionMatrixElement;
import com.bnpp.regliss.repository.AuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SubstitutionMatrixAuditService {
    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private ReglissRequestContext requestContext;


    private <T extends LogDetail> T fill(T audit, SubstitutionMatrixElement element) {
        audit.setUser(requestContext.getCurrentUserRef());
        audit.setLogDate(requestContext.getRequestTime());
        audit.setObjectType(ObjectType.SUBSTITUTION_MATRIX);
        audit.setReference(element.getFileFormat().toString());
        audit.setObjectId(element.getId());
        return audit;
    }

    public void audit(List<SubstitutionMatrixElement> oldMatrixElements, List<SubstitutionMatrixElement> newMatrixElements) {
        auditRepo.saveAll(createAudits(oldMatrixElements, newMatrixElements));
    }

    public void addAudit(SubstitutionMatrixElement newMatrixElements) {
        Audit audit = ReflectionComparator.createAddInCollectionAudit(newMatrixElements, AuditedAction.SUBSTITUTION_MATRIX_CREATION);
        fill(audit, newMatrixElements);
        auditRepo.save(audit);
    }

    public void deleteAudit(SubstitutionMatrixElement newMatrixElements) {
        Audit audit = ReflectionComparator.createDeleteFromCollectionAudit(newMatrixElements, AuditedAction.SUBSTITUTION_MATRIX_DELETE);
        fill(audit, newMatrixElements);
        auditRepo.save(audit);
    }

    List<Audit> createAudits(List<SubstitutionMatrixElement> oldMatrixElements,
            List<SubstitutionMatrixElement> newMatrixElements) {
        Set<SubstitutionMatrixElement> oldCollection = new HashSet<>(oldMatrixElements);
        Set<SubstitutionMatrixElement> newCollection = new HashSet<>(newMatrixElements);

        EntityCollectionDiff<SubstitutionMatrixElement> diff = new EntityCollectionDiff<>(oldCollection, newCollection, e -> Arrays.asList(e.getId()));
        List<Audit> audits = new ArrayList<>();
        for (SubstitutionMatrixElement toCreate : diff.getCreatedElements()) {
            Audit audit = ReflectionComparator.createAddInCollectionAudit(toCreate, AuditedAction.SUBSTITUTION_MATRIX_CREATION);
            fill(audit, toCreate);
            audits.add(audit);
        }

        for (SubstitutionMatrixElement toDelete : diff.getDeletedElements()) {
            Audit audit = ReflectionComparator.createDeleteFromCollectionAudit(toDelete, AuditedAction.SUBSTITUTION_MATRIX_DELETE);
            fill(audit, toDelete);
            audits.add(audit);
        }

        return audits;
    }
}
