package com.bnpp.regliss.facade;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.ObjectType;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.facade.dto.AuditDto;
import com.bnpp.regliss.facade.dto.VersionAuditDto;
import com.bnpp.regliss.facade.dto.record.RecordAuditDto;
import com.bnpp.regliss.facade.dto.version.RecordMinorChangesDto;
import com.bnpp.regliss.facade.mapper.AuditMapper;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.AuditRepository;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.repository.pagination.PaginationOptions;
import com.bnpp.regliss.service.UserAuditService;
import com.bnpp.regliss.service.permission.CurrentVersionPermissions;
import com.bnpp.regliss.service.permission.VersionPermissionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

@Slf4j
@Facade
@ReglissWebProfile
public class AuditFacade {

    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private AuditMapper mapper;

    @Autowired
    private UserAuditService userAuditService;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private CurrentVersionPermissions versionPermissions;

    public List<AuditDto> getAudits(Long objectId, ObjectType objectType) {
        List<Audit> entities = auditRepo.findAllByObjectIdAndObjectTypeOrderByIdDesc(objectId, objectType);
        return entities.stream()
                .map(audit -> mapper.toAuditDto(audit))
                .collect(Collectors.toList());
    }

    public SortedSet<VersionAuditDto> getAuditsForVersion(Long objectId, ObjectType objectType) {
        final List<AuditedAction> visibleActions = Arrays.asList(AuditedAction.VERSION_CREATION,
                AuditedAction.VERSION_MODIFICATION, AuditedAction.VERSION_VALIDATION,
                AuditedAction.VERSION_INTEGRATION, AuditedAction.VERSION_CONFIRMATION,
                AuditedAction.VERSION_RETURN_TO_VALIDATION, AuditedAction.VERSION_CERTIFICATION,
                AuditedAction.VERSION_PUBLICATION, AuditedAction.VERSION_DELIVERY, AuditedAction.VERSION_VERIFICATION);

        List<Audit> entities = auditRepo.findAllByObjectIdAndObjectTypeAndAuditedActionInOrderByIdDesc(
                objectId, objectType, visibleActions);
        return entities.stream()
                .map(mapper::toVersionAuditDto)
                .collect(toCollection(TreeSet::new));
    }

    public List<AuditDto> getAuditsForSubscriber(Long objectId, ObjectType objectType) {
        final List<AuditedAction> visibleActions = Arrays.asList(AuditedAction.SUBSCRIBER_CREATION,
                AuditedAction.SUBSCRIBER_MODIFICATION, AuditedAction.SUBSCRIBER_ACTIVATION,
                AuditedAction.SUBSCRIBER_DEACTIVATION, AuditedAction.SUBSCRIPTION_ADDITION,
                AuditedAction.SUBSCRIPTION_DELETION);

        List<Audit> entities = auditRepo.findAllByObjectIdAndObjectTypeAndAuditedActionInOrderByIdDesc(
                objectId, objectType, visibleActions);
        return entities.stream()
                .map(mapper::toAuditDto)
                .collect(toList());
    }

    public String getPreviousUuid(String uuid, long versionId){
        String extrenalReference = recordRepo.findByExternalUuid(uuid)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND)).getExternalReference();
        return recordRepo.findByExternalReferenceAndVersionId(extrenalReference,versionId).orElseThrow(() ->
                new ReglissException(ErrorCode.NOT_FOUND)).getExternalUuid();

    }
    public List<RecordAuditDto> getAuditsForRecord(String uuid, long versionId) {
        String reglissReference = recordRepo.findByUUIDAndVersionId(uuid, versionId)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND)).getReglissReference();

        Version version = versionRepo.getExactlyOne(versionId);
        versionPermissions.checkVersionAccessForViewRecords(new VersionPermissionRequest(version));

        List<Audit> entities = auditRepo.findAllByReferenceOrderByIdDesc(reglissReference);

        Set<Long> recordIds = entities.stream()
                .filter(a -> !a.getVersion().isDeleted())
                .filter(a-> a.getObjectId()!=null)
                .map(Audit::getObjectId)
                .collect(toSet());
        Map<Long, Record> loadedRecords = recordRepo.findAllById(recordIds).stream().collect(toMap(Record::getId, identity()));


        return entities.stream()
                .filter(a -> !a.getVersion().isDeleted())
                .map(a->mapper.toRecordAuditDto(a, loadedRecords.get(a.getObjectId())))
                .collect(toList());
    }

    public void auditUserLogin() { userAuditService.auditUserLogin(); }

    public Long getCountMinorUpdatesForRecordsInVersion(Long versionId){
        return auditRepo.countAudits4MinorUpdates(versionId);
    }

    public PaginatedResults<RecordMinorChangesDto> getMinorUpdatesForRecordsInVersion(Long versionId, PaginationOptions pagination) {

        long totalAuditCount = auditRepo.countTotalAuditsForVersion(versionId);
        long distinctRecordCount = auditRepo.countAudits4MinorUpdates(versionId);


        List<Audit> audits = auditRepo.queryAudits4MinorUpdates(versionId, pagination);

        Set<Long> recordIds = audits.stream().map(Audit::getObjectId).collect(toSet());
        log.debug("Loading details of a page of records...");
        Map<Long, Record> loadedRecords = recordRepo.findAllById(recordIds).stream().collect(toMap(Record::getId, identity()));

        List<RecordMinorChangesDto> minorUpdates = audits.stream()
                .map(a -> mapper.toRecordMinorUpdateDTO(a, loadedRecords.get(a.getObjectId())))
                .collect(toList());

        return new PaginatedResults<>(minorUpdates, totalAuditCount, distinctRecordCount);
    }

}
