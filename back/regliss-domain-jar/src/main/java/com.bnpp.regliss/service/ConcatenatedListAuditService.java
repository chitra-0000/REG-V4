package com.bnpp.regliss.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.ExportFile.GenerationStatus;
import com.bnpp.regliss.entity.FileDelivery;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.LogDetail;
import com.bnpp.regliss.entity.ObjectType;
import com.bnpp.regliss.entity.VersionStatus;
import com.bnpp.regliss.repository.AuditRepository;

@Service
public class ConcatenatedListAuditService {

    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private ReglissRequestContext requestContext;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishOneGeneratedFileInTx(GeneratedFile generatedFile) {
        Audit audit = new Audit(AuditedAction.OFAC_PUBLICATION);
        fill(audit, generatedFile.getId());
        VersionStatus previousStatus = null;
        VersionStatus newStatus = getGenerationStatus(generatedFile.getGenerationStatus());
        fillValuesForChangedStatus(audit, previousStatus, newStatus);
        auditRepo.save(audit);
    }

    public void deliverOneGeneratedFile(FileDelivery delivery) {
        VersionStatus previousStatus = VersionStatus.PUBLISHED_OK;
        VersionStatus newStatus = getDeliveryStatus(delivery.getStatus());
        Audit audit = new Audit(AuditedAction.OFAC_DELIVERY);
        fill(audit, delivery.getId());
        fillValuesForChangedStatus(audit, previousStatus, newStatus);
        auditRepo.save(audit);
    }

    private VersionStatus getGenerationStatus(GenerationStatus generationStatus) {
        if (generationStatus == GenerationStatus.KO) {
            return VersionStatus.PUBLISHED_KO;
        } else {
            return VersionStatus.PUBLISHED_OK;
        }
    }

    private VersionStatus getDeliveryStatus(FileDelivery.Status fileDeliveryStatus) {
        if (fileDeliveryStatus == FileDelivery.Status.KO) {
            return VersionStatus.DELIVERED_KO;
        } else {
            return VersionStatus.DELIVERED_OK;
        }
    }

    private void fillValuesForChangedStatus(Audit audit, VersionStatus previousStatus, VersionStatus newStatus) {
        if (previousStatus != null) {
            audit.setOldValue(previousStatus.toString());
        }
        audit.setNewValue(newStatus.toString());
    }

    private <T extends LogDetail> void fill(T audit, Long objectId) {
        audit.setLogDate(requestContext.getRequestTime());
        audit.setUser(requestContext.getCurrentUserRef());
        audit.setObjectType(ObjectType.FILE);
        audit.setObjectId(objectId);
    }
}
