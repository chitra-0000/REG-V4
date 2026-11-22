package com.bnpp.regliss.service;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.AuditedCollection;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.record.AuditableRecord;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.repository.AuditRepository;
import com.bnpp.regliss.repository.LogRepository;
import com.bnpp.regliss.repository.ProgramRepository;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.utils.AuditUtil;
import com.bnpp.regliss.utils.EntityUtils;
import com.bnpp.regliss.utils.ReflectionCachedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Service
public class RecordAuditService {

    private static final Logger log = LoggerFactory.getLogger(RecordAuditService.class);

    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private LogRepository logRepo;

    @Autowired
    private ProgramRepository programRepo;

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ConsultedEntitiesInSession session;

    public void cloneAudit(Record oldRecord, Record newRecord) {
        List<Audit> audits = auditRepo.findAllByObjectIdAndObjectType(oldRecord.getId(), ObjectType.RECORD);
        List<Log> logs = logRepo.findAllByObjectIdAndObjectType(oldRecord.getId(), ObjectType.RECORD);

        for (Audit originalAudit : audits) {
            Audit clonedAudit = new Audit(originalAudit);
            clonedAudit.setObjectId(newRecord.getId());
            clonedAudit.setReference(newRecord.getReglissReference());
            auditRepo.save(clonedAudit);
        }

        for (Log originalLog : logs) {
            Log clonedLog = new Log(originalLog);
            clonedLog.setObjectId(newRecord.getId());
            clonedLog.setReference(newRecord.getReglissReference());
            logRepo.save(clonedLog);
        }
    }

    public boolean equalByContent(Record record1, Record record2) {
        List<Audit> audits = ReflectionComparator.compare(record1, record2);
        return audits.isEmpty();
    }

    public List<Audit> createAuditsUpdate(Record oldRecord, Record newRecord, Version version) {
        List<Audit> audits = ReflectionComparator.compare(oldRecord, newRecord);
        fillAuditsUpdate(oldRecord, newRecord, version, audits);
        return audits;
    }

    public List<Audit> createAuditsUpdateForImport(Record oldRecord, Record newRecord, Version version, boolean isManualImport) {
        boolean isCustomImport = version.getImportFileType().isCustomFileType();
        List<Audit> audits = ReflectionComparator.compareForImport(oldRecord, newRecord, isManualImport, isCustomImport);
        fillAuditsUpdate(oldRecord, newRecord, version, audits);
        return audits;
    }

    private void fillAuditsUpdate(Record oldRecord, Record newRecord, Version version, List<Audit> audits) {
        for (Audit audit : audits) {
            if (audit.getAuditedAction() == null) {
                audit.setAuditedAction(AuditedAction.RECORD_MODIFICATION);
            }
            fill(audit, newRecord, oldRecord.getName(), version);
        }

        audits.addAll(createAuditsForOtherProgramParents(newRecord, version, audits));

        if (log.isTraceEnabled()) {
            log.trace("Creating audits : \n {}", audits.stream().map(Object::toString).collect(joining("\n")));
        }
    }

    private List<Audit> createAuditsForOtherProgramParents(Record newRecord, Version version, List<Audit> audits) {
        List<Audit> otherProgramParentsAudits = new ArrayList<>();
        List<Audit> auditsForProgramModifications = audits.stream()
                .filter(audit -> audit.getAuditedAction() == AuditedAction.RECORD_PROGRAM_MODIFICATION)
                .collect(toList());
        for (Audit programModificationAudit : auditsForProgramModifications) {
            Program originalProgram = programRepo.getExactlyOne(programModificationAudit.getChild().getId());
            Set<Record> otherParents = recordRepo.getAllByVersionAndProgram(version.getId(), originalProgram);
            otherParents.remove(newRecord);
            otherProgramParentsAudits.addAll(getAuditsForTheOtherProgramParentsInVersion(programModificationAudit, otherParents, newRecord));
        }
        return otherProgramParentsAudits;
    }

    private List<Audit> getAuditsForTheOtherProgramParentsInVersion(Audit programModificationAudit,
            Set<Record> otherParentsInVersion, Record newRecord) {
        List<Audit> audits = new ArrayList<>();
        for (Record otherParent : otherParentsInVersion) {
            Audit audit = new Audit(programModificationAudit);
            audit.setObject(otherParent);
            audit.setReference(otherParent.getReglissReference());
            Program newChild = newRecord.getPrograms().stream()
                    .filter(p -> audit.getChild().getId().equals(p.getId()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Program cannot be null"));
            audit.setChild(newChild);
            audits.add(audit);
        }
        return audits;
    }

    public void auditCreate(Record record, Version version) {
        Audit audit = new Audit(AuditedAction.RECORD_CREATION);
        fill(audit, record, record.getName(), version);
        auditRepo.save(audit);
    }

    public void auditDeletion(AuditableRecord record, Version version) {
        Audit audit = new Audit(AuditedAction.RECORD_DELETION);
        fill(audit, record, record.getName(), version);
        auditRepo.save(audit);
    }

    public void auditGet(Record record, Version version) {
        boolean unseenRecord = session.addConsultedRecord(record.getId());
        if (unseenRecord) {
            Log logConsult = new Log(AuditedAction.RECORD_CONSULTATION);
            fill(logConsult, record, record.getName(), version);
            logRepo.save(logConsult);
        }
    }

    private <T extends LogDetail> void fill(T audit, AuditableRecord record, String modifiedRecordName, Version version) {
        audit.setLogDate(requestContext.getRequestTime());
        audit.setUser(requestContext.getCurrentUserRef());
        audit.setObjectType(ObjectType.RECORD);
        audit.setReference(record.getReglissReference());
        audit.setObjectId(record.getId());
        audit.setVersion(version);
        audit.setRecordName(modifiedRecordName);
    }

    public void postProcessAndSaveAuditsUpdate(List<Audit> audits, Version version) {
        for (Audit audit : audits) {
            if (audit.getChild() != null) {
                audit.setChildId(audit.getChild().getId());
                // inserted(if cloned) or merged over
            }

            Optional<Record> record = recordRepo.findByReglissReferenceAndVersionIdAndListId(audit.getObject().getReglissReference(), version.getId(), version.getList().getId());
            audit.setObjectId(record.map(Record::getId).orElse(null));
            // inserted(if cloned) or merged over
        }
        auditRepo.saveAll(audits);
    }

    public void postProcessAndSaveAuditsUpdateForBatch(Record newRecord, List<Audit> audits, boolean isManualImport) {
        List<AuditedAction> collectionsActionsToExclude = getActionsToExclude(newRecord, isManualImport);
        List<Audit> auditsToSave = audits.stream()
                .filter(audit -> !collectionsActionsToExclude.contains(audit.getAuditedAction()))
                .collect(toList());
        for (Audit audit : auditsToSave) {
            if (audit.getChild() != null) {
                audit.setChildId(audit.getChild().getId());
            }
            EntityUtils.truncateStringFields(audit);
        }

        auditRepo.saveAll(auditsToSave);
    }

    private List<AuditedAction> getActionsToExclude(Record newRecord, boolean isManualImport) {
        List<AuditedAction> collectionsUpdateActions = ReflectionCachedUtils.getAnnotatedFields(newRecord.getClass(),
                AuditedCollection.class).stream()
                .map(field -> field.getAnnotation(AuditedCollection.class).updateAction())
                .collect(toList());
        if(isManualImport) {
            return collectionsUpdateActions.stream()
                    .filter(action -> action != AuditedAction.RECORD_ALIAS_MODIFICATION)
                    .collect(toList());
        }
        return collectionsUpdateActions;
    }

    public void logSearch(Object criteriaObject) {
        Log logSearch = new Log(AuditedAction.RECORD_SEARCH);
        logSearch.setLogDate(requestContext.getRequestTime());
        logSearch.setUser(requestContext.getCurrentUserRef());
        logSearch.setObjectType(ObjectType.RECORD);
        logSearch.setNewValue(AuditUtil.criteriaToAuditString(criteriaObject));
        logRepo.save(logSearch);
    }

    /*At import time, record xml snippets are compared to determine differences between 2 records
    * However there are sometimes when the values of the record did not change but the xml snippet
    * structure or formatting change (different order of tag elements or trailing white spaces, etc)
    * For these cases there was a special business request to track the changes in the audit of the record
    * even though its values did not actually change.
    * */
    public Audit createOtherChangeBecauseOfXml(Record record, Version version) {
        Audit audit = new Audit();
        audit.setAuditedField(AuditedFieldCode.RECORD_OTHER_CHANGES);
        audit.setAuditedAction(AuditedAction.RECORD_MODIFICATION);
        audit.setOldValue("<oldxml>");
        audit.setNewValue("<newxml>");
        fill(audit, record, record.getName(),version);
        return audit;
    }

}
