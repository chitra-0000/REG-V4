package com.bnpp.regliss.importer;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.Address;
import com.bnpp.regliss.entity.record.AuditableRecord;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.monitoring.PerformanceMonitor;
import com.bnpp.regliss.monitoring.PerformanceMonitor.Action;
import com.bnpp.regliss.monitoring.ProgressMonitor;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.AddModSupRepository;
import com.bnpp.regliss.repository.ProgramRepository;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.service.RecordAuditService;
import com.bnpp.regliss.service.RecordService;
import com.bnpp.regliss.utils.BusinessDataControls;
import com.bnpp.regliss.utils.FormatDateUtil;
import com.bnpp.regliss.utils.RecordUtil;
import com.google.common.collect.Iterables;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang.StringUtils.EMPTY;

@Service
@ReglissBatchProfile
@Scope("prototype") // this class is getBean() for EACH xslt instance run
public class RecordUpdateEngine {

    private static final Logger log = LoggerFactory.getLogger(RecordUpdateEngine.class);

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private AddModSupRepository exportAddModSupRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private RecordCopyOnWriteBatchService copyOnWrite;

    @Autowired
    private RecordAuditService recordAuditService;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private ReattachRecordReferentials reattachLabels;

    @Autowired
    private RecordChildrenFetcher recordChildrenFetcher;

    @Autowired
    private RecordService recordService;

    @Value("${max.warnings.constraints.violation.mail}")
    private int maxWarningsConstraintViolation;

    @Value("${max.errors.constraints.violation.mail}")
    private int maxErrorsConstraintViolation;

    @Value("${max.warnings.truncated.fields.mail}")
    private int maxWarningsTruncatedFields;

    // *********** STATE ******************
    @Getter
    public long versionId; // NOSONAR for performance
    @Getter
    public long listId; // NOSONAR for performance
    private long previousVersionId; // NOSONAR for performance
    private Map<DJProgram, Long> programsCache ; // NOSONAR for performance
    private BusinessDataControls businessDataControls; // NOSONAR for performance
    private AtomicLong lastReferenceSuffix; // NOSONAR for performance
    private String maximumExistingAddressReferenceSuffix; // NOSONAR for performance
    /** record.external_reference -> record.id*/
    private Map<String, Long> externalReferencesToIdInPreviousVersion = new HashMap<>(); // NOSONAR for performance
    private boolean isManualImport; // NOSONAR for performance
    // ******************************

    private long getNextReglissReferenceSuffix() { return lastReferenceSuffix.incrementAndGet(); }

    // Works on a single thread for each import
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void initializeForImportIn(long versionId, boolean isManualImport) {
        log.info("Initializing update engine...");
        this.isManualImport = isManualImport;
        this.versionId = versionId;
        businessDataControls = new BusinessDataControls(maxErrorsConstraintViolation, maxWarningsConstraintViolation,
                maxWarningsTruncatedFields, isManualImport);
        Version version = versionRepository.getExactlyOne(versionId);
        this.listId=version.getList().getId();
        Optional<Version> prevVersion=versionRepository.getPreviousVersionByNumber(listId,version.getVersionNumber());

        this.previousVersionId = prevVersion.map(Version::getId).orElse(-1L);
        lastReferenceSuffix = getLastReferenceSuffix(version);
        synchronized (this) {
            maximumExistingAddressReferenceSuffix = getMaximumAddressReferenceSuffix(version);
        }
        log.debug("Load all record regliss reference in version");
        externalReferencesToIdInPreviousVersion = recordRepository.getExternalReferenceToIdMap(versionId);
        programsCache = loadProgramLabelToIdMap(version);
        log.info("Initialized. Loaded {} programs (full on DEBUG), {} external references", programsCache.size(),
                externalReferencesToIdInPreviousVersion.size());
        log.debug("Initial programs cache: {}", programsCache);
    }

    private String getMaximumAddressReferenceSuffix(Version version){
        PerformanceMonitor.before(Action.GET_ALL_ADDRESS_REFERENCE);
        Set<String> allPastAddressReglissReferences = recordRepository.getAllAddressReferencesByList(version.getList().getId());
        PerformanceMonitor.after(Action.GET_ALL_ADDRESS_REFERENCE);
        return RecordUtil.getMaximumAddressReference(allPastAddressReglissReferences);
    }

    private Map<DJProgram, Long> loadProgramLabelToIdMap(Version version) {
        log.debug("Load all programs for list");
        return programRepository.getAllForList(version.getList().getId()).stream()
                .collect(toMap(DJProgram::new, Program::getId,
                        (a,b)-> {throw new IllegalStateException("Two programs (" +a+" and " + b + ") have the same label");}));
    }

    private AtomicLong getLastReferenceSuffix(Version version) {
        PerformanceMonitor.before(Action.GET_MAX_REFERENCE);
        long lastReferenceSuffixVal = recordService.getMaxReferenceSuffix(version.getList());
        PerformanceMonitor.after(Action.GET_MAX_REFERENCE);
        return new AtomicLong(lastReferenceSuffixVal);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertDJRecords(List<Record> recordsToInsert) {
        try {
            recordsToInsert.stream()
                    .collect(Collectors.toMap(Record::getExternalReference, Record::getExternalReference));
        }
        catch(IllegalStateException e)
        {
            throw new ReglissException(e);
        }

        Version version = versionRepository.getExactlyOne(versionId);
        for (Record newRecord : recordsToInsert) {
            fillAddressReferencesIfEmpty(newRecord, version);
            PerformanceMonitor.before(Action.CREATE_RECORD);
            createAndValidateRecord(newRecord, version);
            PerformanceMonitor.after(Action.CREATE_RECORD);
        }
        log.debug("Inserted {}",recordsToInsert.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDJRecords(List<Record> recordsToUpdate) {
        if(recordsToUpdate.isEmpty()) {
            log.info("No records to update");
        }
        log.info("Updating {} external references. [{}..{}]", recordsToUpdate.size(),
                recordsToUpdate.get(0).getExternalReference(),
                recordsToUpdate.get(recordsToUpdate.size()-1).getExternalReference()
                );

        List<String> externalReferences = recordsToUpdate.stream().map(Record::getExternalReference).collect(toList());

        List<Long> ids = externalReferencesToIds(externalReferences);

        PerformanceMonitor.before(Action.LOAD_FULL_ENTITES);
        Map<String, Record> dbRecords = recordChildrenFetcher.getRecordsWithPrefetchedChildren(ids);
        PerformanceMonitor.after(Action.LOAD_FULL_ENTITES);

        PerformanceMonitor.before(Action.UPDATE_RECORDS);
        updateAndValidateRecords(recordsToUpdate, dbRecords);
        PerformanceMonitor.after(Action.UPDATE_RECORDS);
        log.debug("Updated {}",recordsToUpdate.size());
    }

    public BusinessDataControls getBusinessDataControls() { return businessDataControls; }

    private List<Long> externalReferencesToIds(List<String> externalReferences) {
        return externalReferences.stream()
                .map(externalReferencesToIdInPreviousVersion::get)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public Map<String, Long> getExternalReferencesToIdInPreviousVersion() {
        return Collections.unmodifiableMap(externalReferencesToIdInPreviousVersion);
    }


    public void deleteRecords(Collection<String> externalReferencesToDelete) {
        if (externalReferencesToDelete.isEmpty()) {
            log.info("No records to delete");
            return;
        }
        log.info("Deleting total {} records...", externalReferencesToDelete.size());

        ProgressMonitor monitor = new ProgressMonitor("Delete", log, externalReferencesToDelete.size());
        for (List<String> partition : Iterables.partition(externalReferencesToDelete, 1000)) {
            monitor.add(partition.size());
            myselfProxied().deleteRecordsPageInTxInternal(partition);
        }
        monitor.end();
        log.debug("Delete done");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteRecordsPageInTxInternal(List<String> externalReferencesPartition) {
        log.info("Deleting {} records...", externalReferencesPartition.size());
        List<Long> recordIds = externalReferencesToIds(externalReferencesPartition);
        log.debug("Matched {} records IDs for deletion...", externalReferencesPartition.size());
        if (recordIds.isEmpty()) {
            return; // records to be delete not found in previous version by ext ref
        }
        List<AuditableRecord> recordsToBeDeleted = recordRepository.findById(recordIds, listId);

        for (AuditableRecord record : recordsToBeDeleted) {
            recordAuditService.auditDeletion(record, versionRepository.getReference(versionId));
        }
        recordRepository.markRecordDeletion(recordIds, versionId, previousVersionId, listId);
    }

    // !! SHARED BETWEEN MANUAL AND DJ IMPORT
    private void updateAndValidateRecords(List<Record> newRecordsToBeUpdated, Map<String, Record> dbRecords) {
        Version version = versionRepository.getExactlyOne(versionId);

        List<Record> newRecords = new ArrayList<>();

        List<Long> oldRecordsModToBeRestrictedToPrevVersion = new ArrayList<>(dbRecords.size());
        Set<Long> oldRecordIds = dbRecords.values().stream().map(Record::getId).collect(toSet());
        Map<String, String> recordExternalReferenceToMaxAliasReferenceSuffix =
                recordRepository.getExternalReferenceToMaxAliasReferenceSuffix(oldRecordIds);
        for (Record newRecord : newRecordsToBeUpdated) {
            businessDataControls.addRecordValidationsInSingleThread(newRecord);
            Record oldRecord = dbRecords.get(newRecord.getExternalReference());
            oldRecordsModToBeRestrictedToPrevVersion.add(oldRecord.getId());

            if( businessDataControls.hasNoConstraintViolationsErrors() ||
                    version.getImportFileType() == ImportFileType.MANUAL_PROCESSING) {

                newRecord = prepareRecordForUpdate(version, newRecord, oldRecord, recordExternalReferenceToMaxAliasReferenceSuffix);
                newRecords.add(recordRepository.save(newRecord));
                try {
                    List<Audit> updateAudits = recordAuditService.createAuditsUpdateForImport(oldRecord, newRecord, version, isManualImport);
                if (updateAudits.isEmpty() && !isManualImport) {
                    updateAudits.add(recordAuditService.createOtherChangeBecauseOfXml(newRecord, version));
                }
                for (Audit audit : updateAudits) {
                    audit.setObjectId(newRecord.getId());
                }
                recordAuditService.postProcessAndSaveAuditsUpdateForBatch(newRecord, updateAudits, isManualImport);
                log.trace("The record externalref={} id:{} was cloned to id:{} ", oldRecord.getExternalReference(),
                        oldRecord.getId(), newRecord.getId());
            }
            catch(RuntimeException e)
            {
                log.error(" Could not import feed due to the change of description ID's for the record. " +
                        "External reference is {} for list {}", oldRecord.getExternalReference(), e);
            }
            }
        }

        log.debug("restrict to older version records :[{}] , version :{} , list :{} ",
                oldRecordsModToBeRestrictedToPrevVersion,previousVersionId,listId);
        recordRepository.moveOldModRecordsToVersion(oldRecordsModToBeRestrictedToPrevVersion,previousVersionId,listId);
    }

    private Record prepareRecordForUpdate(Version version, Record newRecord, Record oldRecord,
                                          Map<String, String> recordExternalReferenceToMaxAliasReferenceSuffix) {
        if(StringUtils.isEmpty(newRecord.getStartDateStr())) {
            newRecord.setStartDateStr(oldRecord.getStartDateStr());
        }
        newRecord.copyUnmodifiableFieldsFrom(oldRecord);

        PerformanceMonitor.before(Action.LINK_OR_CREATE_CHILDREN);
        newRecord = copyOnWrite.linkOrCreateChildren(newRecord, oldRecord, isManualImport);
        PerformanceMonitor.after(Action.LINK_OR_CREATE_CHILDREN);

        fillAddressReferencesIfEmpty(newRecord, version);
        newRecord.fillAliasesReglissReference(recordExternalReferenceToMaxAliasReferenceSuffix.get(newRecord.getExternalReference()));

        resolveDuplicatedPrograms(newRecord);

        PerformanceMonitor.before(Action.REATTACH_LABELS);
        reattachLabels.reattachRecordReferentials(newRecord);
        PerformanceMonitor.after(Action.REATTACH_LABELS);
        newRecord.getVersionLinks().updateLinksAtCloneInVersion(version);
        return newRecord;
    }

    private void fillAddressReferencesIfEmpty(Record record, Version version) {
        List<Address> addresses = record.getAddresses().stream()
                .sorted(comparing(Address::getReglissReference, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Address::getIndexOnRecord, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(toList());

        for(Address address : addresses){
            if(address.getReglissReference() == null){
                synchronized (this) {
                    String nextAddressReference = RecordUtil.computeNextAddressReference(version, maximumExistingAddressReferenceSuffix);
                    if(nextAddressReference != null){
                        address.setReglissReference(nextAddressReference);
                        maximumExistingAddressReferenceSuffix = RecordUtil.getAddressReferenceSuffix(nextAddressReference);
                    }
                }
            }
        }
    }
    /*SHARED BETWEEN AUTO AND SEMI-AUTO!!!*/
    private void createAndValidateRecord(Record record, Version version) {
        businessDataControls.addRecordValidationsInSingleThread(record);

        if(businessDataControls.hasNoConstraintViolationsErrors() ||
                version.getImportFileType() == ImportFileType.MANUAL_PROCESSING) {
            prepareRecordForInsert(record, version);
            log.trace("Creating record {} ", record.getExternalReference());
            PerformanceMonitor.before(Action.REQUEST_SAVE_ON_CREATE);
            recordRepository.save(record);
            recordAuditService.auditCreate(record, version);
            PerformanceMonitor.after(Action.REQUEST_SAVE_ON_CREATE);
        }
    }

    private Record prepareRecordForInsert(Record record, Version version) {
        long nextReglissReferenceSuffix = getNextReglissReferenceSuffix();
        if(nextReglissReferenceSuffix % 1000000 == 0){
            nextReglissReferenceSuffix = getNextReglissReferenceSuffix();
        }
        ImportFileType importFileType = version.getImportFileType();
        if (!ImportFileType.CUSTOM.equals(importFileType) && !ImportFileType.CRB_LIST.equals(importFileType)) {
            record.assignNewReglissReferenceAndRecordId(nextReglissReferenceSuffix, version);
        } else {
            record.assignNewReglissReferenceAndRecordIdIfNoValue(nextReglissReferenceSuffix, version);
        }
        record.fillAliasesReglissReference(EMPTY);
        resolveDuplicatedPrograms(record);
        if (!isManualImport) {
            reattachLabels.reattachRecordReferentials(record);
        }
        if(StringUtils.isEmpty(record.getStartDateStr())){
            String dateFromVersion = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(version.getStartDate());
            record.setStartDateStr(dateFromVersion);
        }
        record.createInVersion(version);
        return record;
    }

    private void resolveDuplicatedPrograms(Record newRecord) {
        Set<Program> newPrograms = newRecord.getPrograms().stream().map(this::resolveProgramInCache).collect(toSet());
        newRecord.clearPrograms();
        newPrograms.forEach(newRecord::addProgram);
    }

    private Program resolveProgramInCache(Program program) {
        DJProgram djProgram = new DJProgram(program);
        Long uniqueProgramId;
        synchronized (programsCache) {
            uniqueProgramId = programsCache.get(djProgram);
        }
        if (uniqueProgramId==null) {
            List<DJProgram> programsInDb = programRepository.getAllForVersion(versionId,listId).stream().map(DJProgram::new).collect(toList());
            log.error("Current program cache: {}. \n Programs inserted in DB for this version: {} ", programsCache, programsInDb);
            log.error(" program {} tested again in cache {} ",djProgram,programsCache.containsKey(djProgram));
            if(!programsCache.containsKey(djProgram)){
                programsCache.keySet()
                        .stream()
                        .filter(p->p.getLabel().equals(djProgram.getLabel()) &&
                                    Objects.equals(p.getDescription(),djProgram.getDescription()) &&
                                    Objects.equals(p.getSanctionTypeId(),djProgram.getSanctionTypeId()))
                        .forEach(p -> log.error("hash1 {} - hash2{} ; are equal:{}",p.hashCode(),djProgram.hashCode(), p.equals(djProgram)));
            }
            throw new IllegalStateException("Program " + djProgram + " should have been inserted in cache in advance");
        }

        return programRepository.getReference(uniqueProgramId);
    }


    public void mergeProgramsInAdvanceInSingleThreadInTx(Collection<Record> recordsPage) {
        List<Program> programs = recordsPage.stream().flatMap(r -> r.getPrograms().stream()).collect(toList());
        log.debug("start merge programs {} ",programsCache.size());
        synchronized (programsCache) {
            myselfProxied().mergeProgramsInAdvanceInTxInternal(programs);
            log.debug("end merge programs {}",programsCache.size());
        }

    }

    private RecordUpdateEngine myselfProxied() { return (RecordUpdateEngine) AopContext.currentProxy(); }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void mergeProgramsInAdvanceInTxInternal(List<Program> programs) {
        for (Program program : programs) {
            DJProgram djProgram = new DJProgram(program);
            if (!programsCache.containsKey(djProgram)) {
                programRepository.save(program);
                log.debug("Adding in cache: {}" , djProgram);
                programsCache.put(djProgram, program.getId());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertOrUpdateForManualImportInTx(List<Record> newRecordsPartition, ManualImportStats stats) {
        List<Record> newRecordsToBeUpdated = new ArrayList<>();
        List<Record> dbRecordsToBeUpdated = new ArrayList<>();

        Version version = versionRepository.getExactlyOne(versionId);
        List<Long> ids = externalReferencesToIds(newRecordsPartition.stream().map(Record::getExternalReference).collect(toList()));

        PerformanceMonitor.before(Action.LOAD_FULL_RECORD_DATA);
        Map<String, Record> dbRecords = recordChildrenFetcher.getRecordsWithPrefetchedChildren(ids);
        PerformanceMonitor.after(Action.LOAD_FULL_RECORD_DATA);

        for (Record newRecord : newRecordsPartition) {
            fillAddressReferencesIfEmpty(newRecord, version);
            Record dbRecord = dbRecords.get(newRecord.getExternalReference());
            if (dbRecord == null) {
                createAndValidateRecord(newRecord, version);
                stats.addCreated();
            } else {
                // modify
                newRecord.setStartDateStr(dbRecord.getStartDateStr());
                if (copyOnWrite.areEqual(newRecord, dbRecord)) {
                    log.trace("Record was not changed");
                    continue;
                }

                newRecordsToBeUpdated.add(newRecord);
                dbRecordsToBeUpdated.add(dbRecord);
                stats.addUpdated();
            }
        }
        Map<String, Record> dbRecordsMap =  dbRecordsToBeUpdated
                .stream().collect(toMap(Record::getExternalReference, Function.identity(), (r1, r2) -> r1));
        if(!newRecordsToBeUpdated.isEmpty()){
            updateAndValidateRecords(newRecordsToBeUpdated, dbRecordsMap);
        }
        log.debug("Finished saving a page to Hibernate");
    }
}

