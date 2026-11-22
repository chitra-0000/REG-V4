package com.bnpp.regliss.service;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.app.RecordVerificationInfo;
import com.bnpp.regliss.repository.AuditRepository;
import com.bnpp.regliss.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VersionAuditService {

    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private LogRepository logRepo;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ConsultedEntitiesInSession session;

    public void auditCreate(Version version) {
        Audit audit = new Audit(AuditedAction.VERSION_CREATION);
        fill(audit, version, version.getCreationDate());
        auditRepo.save(audit);
    }

    public void auditDeletion(Version version) {
        Audit audit = new Audit(AuditedAction.VERSION_DELETION);
        fill(audit, version, requestContext.getRequestTime());
        auditRepo.save(audit);
    }

    public void auditGet(Version version) {
        boolean unseenVersion = session.addConsultedVersion(version.getId());
        if (unseenVersion) {
            Log log = new Log(AuditedAction.VERSION_CONSULTATION);
            fill(log, version, requestContext.getRequestTime());
            logRepo.save(log);
        }
    }

    public void auditUpdate(Version oldVersion, Version newVersion) {
        List<Audit> audits = ReflectionComparator.compare(oldVersion, newVersion);
        for (Audit audit : audits) {
            audit.setAuditedAction(AuditedAction.VERSION_MODIFICATION);
            fill(audit, oldVersion, requestContext.getRequestTime());
            auditRepo.save(audit);
        }
    }

    public void auditValidate(VersionStatus previousStatus, Version version) {
        Audit audit = new Audit(AuditedAction.VERSION_VALIDATION);
        fill(audit, version, version.getValidationDate());
        fillValuesForChangedStatus(audit, previousStatus, version);
        auditRepo.save(audit);
    }

    public void auditIntegration(Version version) {
        Audit audit = new Audit(AuditedAction.VERSION_INTEGRATION);
        fill(audit, version, version.getValidationDate());
        fillValuesForChangedStatus(audit, VersionStatus.DRAFT, version);
        auditRepo.save(audit);
    }

    public void auditCertify(Version version) {
        Audit audit = new Audit(AuditedAction.VERSION_CERTIFICATION);
        fill(audit, version, version.getCertificationDate());
        fillValuesForChangedStatus(audit, VersionStatus.VALIDATED, version);
        auditRepo.save(audit);
    }

    public void auditConfirm(Version version, String confirmationMessage) {
        Audit audit = new Audit(AuditedAction.VERSION_CONFIRMATION);
        fill(audit, version, version.getCertificationDate());
        fillValuesForChangedStatus(audit, VersionStatus.VALIDATED, version);
        audit.setNewValue(confirmationMessage);
        auditRepo.save(audit);
    }

    public void auditPublish(Version version, User sysUser) {
        Audit audit = fillAuditPublish(version, version.getPublicationDate());
        audit.setUser(sysUser);
        auditRepo.save(audit);
    }

    public void auditPublish(Version version, LocalDateTime publicationDate) {
        auditRepo.save(fillAuditPublish(version, publicationDate));
    }

    private Audit fillAuditPublish(Version version, LocalDateTime publicationDate) {
        Audit audit = new Audit(AuditedAction.VERSION_PUBLICATION);
        fill(audit, version, publicationDate);
        fillValuesForChangedStatus(audit, VersionStatus.CERTIFIED, version);
        return audit;
    }

    public void auditDeliver(VersionStatus previousStatus, Version version) {
        Audit audit = new Audit(AuditedAction.VERSION_DELIVERY);
        fill(audit, version, requestContext.getRequestTime());
        fillValuesForChangedStatus(audit, previousStatus, version);
        auditRepo.save(audit);
    }

    public void auditRecordVerificationInfo(RecordVerificationInfo oldRecordVerificationInfo, RecordVerificationInfo newRecordVerificationInfo, Version version) {
        List<Audit> audits = ReflectionComparator.compare(oldRecordVerificationInfo, newRecordVerificationInfo);
        for (Audit audit : audits) {
            audit.setAuditedAction(AuditedAction.VERSION_RECORD_VERIFICATION);
            fill(audit, version, requestContext.getRequestTime());
            audit.setChildId(newRecordVerificationInfo.getId());
            auditRepo.save(audit);
        }
    }

    private void fillValuesForChangedStatus(Audit audit, VersionStatus previousStatus, Version version) {
        audit.setOldValue(previousStatus.toString());
        audit.setNewValue(version.getStatus().toString());
    }

    public void auditReturn(Version version, String oldReturnComment) {
        Audit audit = new Audit(AuditedAction.VERSION_RETURN_TO_VALIDATION);
        audit.setAuditedField(AuditedFieldCode.VERSION_RETURN_COMMENT);
        audit.setOldValue(oldReturnComment);
        audit.setNewValue(version.getReturnComment());
        fill(audit, version, version.getReturnDate());
        auditRepo.save(audit);
    }

    public void auditImportFile(Version version) {
        Audit audit = new Audit(AuditedAction.VERSION_IMPORT_FILE);
        audit.setNewValue(version.getFileName());
        fill(audit, version, requestContext.getRequestTime());
        auditRepo.save(audit);
    }

    private <T extends LogDetail> void fill(T audit, Version version, LocalDateTime auditDate) {
        audit.setLogDate(auditDate);
        audit.setUser(requestContext.getCurrentUserEager());
        audit.setObjectType(ObjectType.VERSION);
        audit.setReference(version.computeVersionReference());
        audit.setObjectId(version.getId());
    }

    public void auditVerify(Version version) {
        Audit audit = new Audit(AuditedAction.VERSION_VERIFICATION);
        fill(audit, version, requestContext.getRequestTime());
        audit.setNewValue(version.getVerificationMessage());
        auditRepo.save(audit);
    }

    public void deleteAllAuditsForVersionFromDB(Long versionId) { auditRepo.deleteAllAuditsForVersion(versionId); }

    public void deleteAllAuditsForTheRecordsOfVersion(Long versionId){
        auditRepo.deleteAllAuditsForTheRecordsOfVersion(versionId);
    }
}
