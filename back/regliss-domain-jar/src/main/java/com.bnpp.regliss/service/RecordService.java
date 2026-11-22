package com.bnpp.regliss.service;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.entity.ComputedRecordNumbers;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.Address;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.repository.AddModSupRepository;
import com.bnpp.regliss.repository.AliasRepository;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.service.lastModificationDate.LastModificationDateCalculationService;
import com.bnpp.regliss.service.permission.CurrentFeaturePermissions;
import com.bnpp.regliss.utils.RecordUtil;
import com.bnpp.regliss.vo.FeaturePermission;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.bnpp.regliss.entity.referential.ReferentialBusinessConcept.ALIAS_QUALITY_WEAK;
import static org.apache.commons.lang.StringUtils.EMPTY;

@Slf4j
@Service
public class RecordService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RecordAuditService recordAuditService;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private AliasRepository aliasRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private RecordCopyOnWriteUIService copyOnWrite;

    @Autowired
    private AddModSupRepository exportAddModSupRepository;

    @Autowired
    private CurrentFeaturePermissions featurePermissions;

    @Autowired
    private LastModificationDateCalculationService lastModificationDateCalculationService;

    public void createRecord(Record record, Version version) {
        entityManager.lock(version.getList(), LockModeType.PESSIMISTIC_WRITE);

        assignRecordReferences(record, version);

        record.fillAliasesReglissReference(aliasRepository.getMaxReglissReferenceSuffixByRecordReglissReference(record.getReglissReference()).orElse(EMPTY));
        record.setPrograms(copyOnWrite.mergePrograms(record.getPrograms(), version.getList().getId(), true, version.getId()));
        fillAddressReferences(record, version);
        record.createInVersion(version);
        recordRepository.save(record);
        updateVersionStatisticForCreateRecord(record, version);
        recordAuditService.auditCreate(record, version);
    }

    private void assignRecordReferences(Record record, Version version) {
        long tentativeReglissReferenceSuffix = getMaxReferenceSuffix(version.getList()) + 1;
        record.assignNewReglissReferenceAndRecordId(tentativeReglissReferenceSuffix, version);

        if (StringUtils.isNotEmpty(record.getExternalReference())) {
            if (isNotUniqueExternalReference(version, record.getExternalReference())) {
                throw new ReglissException(ErrorCode.RECORD_EXTERNAL_REFERENCE_ALREADY_IN_USE, record.getExternalReference());
            }
        } else {
            while (isNotUniqueExternalReference(version, record.getReglissReference())) {
                tentativeReglissReferenceSuffix ++;
                record.assignNewReglissReferenceAndRecordId(tentativeReglissReferenceSuffix, version);
            }
            record.setExternalReference(record.getReglissReference());
        }
    }

    private boolean isNotUniqueExternalReference(Version version, String externalReference) {
        return recordRepository.checkUniqueExternalReferences(version.getId(), externalReference) != 0;
    }

    public String update(Record newRecord, Version version) {
        Record oldRecord = recordRepository.getExactlyOne(newRecord.getId());

        if (recordAuditService.equalByContent(oldRecord, newRecord)) {
            log.debug("Record was not changed");
            return oldRecord.getExternalUuid();
        } else {
            newRecord.copyUnmodifiableFieldsFrom(oldRecord);
            newRecord = copyOnWrite.update(newRecord, version);
            newRecord.fillAliasesReglissReference(aliasRepository.getMaxReglissReferenceSuffixByRecordReglissReference(newRecord.getReglissReference()).orElse(EMPTY));
            fillAddressReferences(newRecord, version);
            updateVersionStatisticForUpdateRecord(oldRecord, newRecord, version);
            lastModificationDateCalculationService.updateLastModificationDate(version.getId(),version.getListId());
            return newRecord.getExternalUuid();
        }
    }

    public long getMaxReferenceSuffix(ReglissList reglissList) {
        log.debug("Get all regliss references for this list..");
        long maxReglissReference = recordRepository.getAllReglissReferencesByList(reglissList.getId()).stream()
                .mapToLong(fullRegRef -> Long.parseLong(fullRegRef.substring(fullRegRef.lastIndexOf('.') + 1)))
                .max().orElse(0L);
        long maxRecordId = recordRepository.getAllRecordIdsByList(reglissList.getId()).stream()
                .mapToLong(this::getRecordIdSuffix)
                .max().orElse(0L);
        return Math.max(maxReglissReference, maxRecordId);
    }

    private Long getRecordIdSuffix(String fullRecordId) {
        Pattern p = Pattern.compile("([0-9]{3})([N-Z]){1}([0-9]{6})");
        Matcher m = p.matcher(fullRecordId);
        if(!m.matches()){
            if(log.isErrorEnabled()){
                log.error("RecordID {} does not match expected pattern: {}", fullRecordId, p.pattern());
            }
            return 0L;
        }
        String sequenceSeparator = m.group(2);
        String elementNumber = m.group(3);
        int sequenceSeparatorNumber = sequenceSeparator.charAt(0) - 'N';
        return sequenceSeparatorNumber * 1000000L + Long.parseLong(elementNumber);
    }

    public void deleteRecord(Record record, Version version) {
        recordAuditService.auditDeletion(record, version);
        if (record.wasCreatedInVersion(version.getId())) {
            record.markForPurge();
        } else {
            Version previousVersion = version.getPreviousVersion()
                    .orElseThrow(() -> new IllegalStateException("Previous version must exist at this point in code"));
            if (record.wasClonedInVersion(version.getId())) {
                log.debug("Deleting the clone and relinking to Record in previous version");
                Record previousRecord = recordRepository.findByReglissReferenceAndVersionIdAndListId(record.getReglissReference(), previousVersion.getId(),previousVersion.getList().getId())
                        .orElseThrow(() -> new IllegalStateException("Record must have existed in the previous version: not created in the current one"));

                previousRecord.deleteInVersion(version, previousVersion);
                record.markForPurge();
            } else {
                record.deleteInVersion(version, previousVersion);
            }
        }
        updateVersionStatisticForDeleteRecord(record, version);
    }

    public boolean jasperPdfDownload(Version version){
        if(version.getList().getConfidentiality() == Confidentiality.PUBLIC)
            return true;
        return featurePermissions.has(FeaturePermission.userDownloadExportFilesAllowed);
    }

    private void updateVersionStatisticForCreateRecord(Record record, Version version) {
        ComputedRecordNumbers versionStat = version.getComputedRecordNumbers();
        versionStat.setAdd(versionStat.getAdd() + 1);

        long weakAliasNo = record.getAliases().stream().filter(alias -> alias.hasQuality(ALIAS_QUALITY_WEAK)).count();
        long strongAliasNo = record.getAliases().size() - weakAliasNo;
        if(record.getType() == RecordType.PERSON){
            long computedValue= versionStat.getAddPersons()!=null?versionStat.getAddPersons():0;
            versionStat.setAddPersons(computedValue+1);
        }

        versionStat.setWeak(versionStat.getWeak() + weakAliasNo);
        versionStat.setStrong(versionStat.getStrong() + strongAliasNo);

        versionStat.setTotal(versionStat.getTotal() + 1);
        version.setComputedRecordNumbers(versionStat);

        versionRepository.save(version);
    }

    private void updateVersionStatisticForDeleteRecord(Record record, Version version) {
        ComputedRecordNumbers versionStat = version.getComputedRecordNumbers();
        if (record.wasCreatedInVersion(version.getId())) {
            versionStat.setAdd(versionStat.getAdd() - 1);
            if(record.getType() == RecordType.PERSON){
                long computedValue= versionStat.getAddPersons()!=null?versionStat.getAddPersons():0;
                versionStat.setAddPersons(computedValue-1);
            }
        } else if (record.wasClonedInVersion(version.getId())) {
            versionStat.setMod(versionStat.getMod() - 1);
            if(record.getType() == RecordType.PERSON){
                long computedValue= versionStat.getModPersons()!=null?versionStat.getModPersons():0;
                versionStat.setModPersons(computedValue-1);
            }
        }

        long weakAliasNo = record.getAliases().stream().filter(alias -> alias.hasQuality(ALIAS_QUALITY_WEAK)).count();
        long strongAliasNo = record.getAliases().size() - weakAliasNo;

        versionStat.setWeak(versionStat.getWeak() - weakAliasNo);
        versionStat.setStrong(versionStat.getStrong() - strongAliasNo);
        versionStat.setSup(versionStat.getSup() + 1);
        if(record.getType() == RecordType.PERSON){
            long computedValue= versionStat.getSupPersons()!=null?versionStat.getSupPersons():0;
            versionStat.setSupPersons(computedValue+1);
        }

        versionStat.setTotal(versionStat.getTotal() - 1);

        /*Extreme situation:
        * Modify record -> mod count is updated (+1) then in the same version delete the record:
        * we have to recalculate the count because compared to previous version the record was not modified
        * */
        versionStat.setModForSynthesis(exportAddModSupRepository.getCountModifiedRecordsForSynthesis(version.getId()));
        version.setComputedRecordNumbers(versionStat);
        versionRepository.save(version);
    }

    private void updateVersionStatisticForUpdateRecord(Record oldRecord, Record newRecord, Version version) {
        ComputedRecordNumbers versionStat = version.getComputedRecordNumbers();

        if (!oldRecord.wasCreatedInVersion(version.getId()) && !oldRecord.wasClonedInVersion(version.getId())) {
            versionStat.setMod(versionStat.getMod() + 1);
            if(oldRecord.getType() == RecordType.PERSON){
                long computedValue= versionStat.getModPersons()!=null?versionStat.getModPersons():0;
                versionStat.setModPersons(computedValue+1);
            }
        }

        long oldWeakAliasNo = oldRecord.getAliases().stream().filter(alias -> alias.hasQuality(ALIAS_QUALITY_WEAK)).count();
        long oldStrongAliasNo = oldRecord.getAliases().size() - oldWeakAliasNo;

        long newWeakAliasNo = newRecord.getAliases().stream().filter(alias -> alias.hasQuality(ALIAS_QUALITY_WEAK)).count();
        long newStrongAliasNo = newRecord.getAliases().size() - newWeakAliasNo;

        versionStat.setWeak(versionStat.getWeak() - oldWeakAliasNo + newWeakAliasNo);
        versionStat.setStrong(versionStat.getStrong() - oldStrongAliasNo + newStrongAliasNo);

        versionStat.setModForSynthesis(exportAddModSupRepository.getCountModifiedRecordsForSynthesis(version.getId()));
        version.setComputedRecordNumbers(versionStat);
        versionRepository.save(version);
    }

    private void fillAddressReferences(Record record, Version version){
        Set<Address> addresses = RecordUtil.getAddresses(record);
        String maxExistingAddressReferenceSuffix = getMaximumExistingAddressReferenceSuffix(version);
        for(Address address : addresses){
            if(address.getReglissReference() == null){
                String nextAddressReference = RecordUtil.computeNextAddressReference(version, maxExistingAddressReferenceSuffix);
                if(nextAddressReference != null){
                    address.setReglissReference(nextAddressReference);
                }
                maxExistingAddressReferenceSuffix = RecordUtil.getAddressReferenceSuffix(nextAddressReference);
            }
        }
    }

    private String getMaximumExistingAddressReferenceSuffix(Version version){
        Set<String> allPastAddressReglissReferences = recordRepository.getAllAddressReferencesByList(version.getList().getId());
        return  RecordUtil.getMaximumAddressReference(allPastAddressReglissReferences);
    }
}
