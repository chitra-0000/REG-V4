package com.bnpp.regliss.service;


import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.*;
import com.bnpp.regliss.repository.AuditRepository;
import com.bnpp.regliss.repository.ProgramRepository;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.utils.AuditUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

@Service
public class RecordCopyOnWriteUIService {

    private static final Logger log = LoggerFactory.getLogger(RecordCopyOnWriteUIService.class);

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private RecordAuditService recordAuditService;

    @Autowired
    private ProgramRepository programRepo;

    @Autowired
    AuditRepository auditRepo;

    @Autowired
    private EntityManager em;

    public Record update(Record newRecord, Version version) {
        Record originalRecordSnapshot = AuditUtil.copy(recordRepository.getExactlyOne(newRecord.getId()));

        List<Audit> audits = recordAuditService.createAuditsUpdate(originalRecordSnapshot, newRecord, version);

        newRecord = updateOrCloneRecord(newRecord, version);

        relinkClonedProgramsToOldParents(version, audits);

        recordAuditService.postProcessAndSaveAuditsUpdate(audits, version);

        return newRecord;
    }

    private void relinkClonedProgramsToOldParents(Version version, List<Audit> audits) {
        Map<Long, Program> originalProgramMap = audits.stream()
                .filter(audit -> audit.getAuditedAction() == AuditedAction.RECORD_PROGRAM_MODIFICATION)
                .collect(toMap(Audit::getChildId, audit -> (Program) audit.getChild(), (program, identicalProgram) -> program));

        for (Map.Entry<Long, Program> originalProgramEntry : originalProgramMap.entrySet()) {
            Program originalProgramInstance = originalProgramEntry.getValue();
            Long currentProgramId = originalProgramInstance.getId();
            boolean programCloned = !originalProgramEntry.getKey().equals(currentProgramId);

            if (programCloned) {
                Program originalProgram = programRepo.getExactlyOne(originalProgramEntry.getKey());
                relinkProgramParents(originalProgram, originalProgramInstance.getId(), version);
            }
        }
    }

    private void relinkProgramParents(Program originalProgram, Long clonedProgramId, Version version) {
        // Records from this Version that are still linked to the original Program
        Set<Record> recordsToRelink = recordRepository.getAllByVersionAndProgram(version.getId(), originalProgram);
        log.debug("Will relink all Records in {} linked to Program {} to point to Program {}", version, originalProgram.getId(), clonedProgramId);
        for (Record recordToRelink : recordsToRelink) {
            recordToRelink = AuditUtil.copy(recordRepository.getExactlyOne(recordToRelink.getId()));
            log.debug("Relinking record: {}", recordToRelink.getId());
            recordToRelink.removeProgram(originalProgram);
            recordToRelink.addProgram(programRepo.getExactlyOne(clonedProgramId));

            updateOrCloneRecord(recordToRelink, version);
        }
        log.debug("Relinked all Records in {} linked to Program {} to point to Program {}", version, originalProgram.getId(), clonedProgramId);
    }

    private boolean recordWillBeCloned(Record record, Version version) {
        log.debug("{} needs cloning? ", record.getVersionLinks().printActiveInterval());

        return !record.isActiveSinceVersion(version.getId());
    }

    private Record updateOrCloneRecord(Record newRecord, Version version) {
        Record oldRecord = recordRepository.getExactlyOne(newRecord.getId());

        boolean recordWillBeCloned = recordWillBeCloned(oldRecord,version);

        mergeOrCloneChildren(newRecord, oldRecord, recordWillBeCloned, version);
        // merge top record AFTER updating children, to avoid CASCADING
        if (recordWillBeCloned) {
            cloneAndPersistRecord(newRecord, oldRecord, version);
        } else {
            log.debug("The record id:{} will be merged over", newRecord.getId());
            newRecord = em.merge(newRecord);
            log.debug("Linking new record id:{} to: {} ", newRecord.getId(), version);
        }
        return newRecord;
    }

    private void cloneAndPersistRecord(Record newRecord, Record oldRecord, Version version) {
        log.debug("The record id:{} will be cloned", newRecord.getId());
        Version previousVersion = version.getPreviousVersion()
                .orElseThrow(() -> new IllegalStateException("Previous version must exist at this point in code"));
        newRecord.setId(null);
        newRecord.cloneInVersionFromRecord(version, previousVersion, oldRecord);
        em.persist(newRecord);

        log.info("The record id:{} was cloned to id:{}", oldRecord.getId(), newRecord.getId());
    }

    private void mergeOrCloneChildren(Record newRecord, Record databaseRecord, boolean recordWillBeCloned,
            Version version) {
        log.debug("Merge or clone children of record: {} in version {}", newRecord.getId(), version.getId());
        mergeOrCloneChildren(newRecord, databaseRecord, Record::getAliases, Record::setAliases, recordWillBeCloned, version.getId());
        mergeOrCloneChildren(newRecord, databaseRecord, Record::getSanctionReferences, Record::setSanctionReferences, recordWillBeCloned, version.getId());
        mergeOrCloneChildren(newRecord, databaseRecord, Record::getSources, Record::setSources, recordWillBeCloned, version.getId());
        mergeOrCloneChildren(newRecord, databaseRecord, Record::getRelations, Record::setRelations, recordWillBeCloned, version.getId());

        mergeOrCloneChildren(newRecord, databaseRecord, Record::getCategories, Record::setCategories, recordWillBeCloned, version.getId());
        mergeOrCloneChildren(newRecord, databaseRecord, Record::getSanctionTypes, Record::setSanctionTypes, recordWillBeCloned, version.getId());
        mergeOrCloneChildren(newRecord, databaseRecord, Record::getDescriptions3, Record::setDescriptions3, recordWillBeCloned, version.getId());
        mergeOrCloneChildren(newRecord, databaseRecord, Record::getSanctionReferences, Record::setSanctionReferences, recordWillBeCloned, version.getId());

        mergeOrCloneChildren(newRecord, databaseRecord, Record::getCountriesDetails, Record::setCountriesDetails, recordWillBeCloned, version.getId());
        mergeOrCloneChildren(newRecord, databaseRecord, Record::getIdentityDocuments, Record::setIdentityDocuments, recordWillBeCloned, version.getId());
        mergeOrCloneChildren(newRecord, databaseRecord, Record::getDatesDetails, Record::setDatesDetails, recordWillBeCloned, version.getId());
        mergeOrCloneChildren(newRecord, databaseRecord, Record::getAddresses, Record::setAddresses, recordWillBeCloned, version.getId());
        mergeOrCloneChildren(newRecord, databaseRecord, Record::getIbanCodes, Record::setIbanCodes, recordWillBeCloned, version.getId());

        newRecord.setPrograms(mergePrograms(newRecord.getPrograms(), version.getList().getId(), recordWillBeCloned, version.getId()));

        switch (databaseRecord.getType()) {
        case PERSON:
            mergeOrCloneChildren(newRecord, databaseRecord, PersonRecord::getBiographies, PersonRecord::setBiographies, recordWillBeCloned, version.getId());
            mergeOrCloneChildren(newRecord, databaseRecord, PersonRecord::getPersonFunctions, PersonRecord::setPersonFunctions, recordWillBeCloned, version.getId());
            mergeOrCloneChildren(newRecord, databaseRecord, PersonRecord::getPersonImages, PersonRecord::setPersonImages, recordWillBeCloned, version.getId());
            break;
            /* falls through */
        case ENTITY:
            mergeOrCloneChildren(newRecord, databaseRecord, EntityRecord::getBicCodes, EntityRecord::setBicCodes, recordWillBeCloned, version.getId());
            mergeOrCloneChildren(newRecord, databaseRecord, EntityRecord::getCusipCodes, EntityRecord::setCusipCodes, recordWillBeCloned, version.getId());
            mergeOrCloneChildren(newRecord, databaseRecord, EntityRecord::getIsinCodes, EntityRecord::setIsinCodes, recordWillBeCloned, version.getId());
            break;
            /* falls through */
        default:
            /* no additional children */
        }
    }

    public Set<Program> mergePrograms(Set<Program> newPrograms, long idList, boolean recordWillBeCloned, Long versionId) {
        Set<Program> oldPrograms = programRepo.getAllForList(idList);
        log.debug("List programs: \n{}\nNew Programs:\n{}", oldPrograms, newPrograms);
        return mergeChildren(newPrograms, oldPrograms, recordWillBeCloned, versionId);
    }

    /*unchecked*/
    private <T, C extends AbstractSimpleEntity & RecordAwareChild> void mergeOrCloneChildren(Record newRecord,
            Record databaseRecord, Function<T, Set<C>> getter, BiConsumer<T, Set<C>> setter,
            boolean recordWillBeCloned,Long versionId) {
        Set<C> newChildren = getter.apply((T) newRecord);
        Set<C> oldChildren = getter.apply((T) databaseRecord);
        Set<C> mergedChildren = mergeChildren(newChildren, oldChildren, recordWillBeCloned, versionId);
        setter.accept((T) newRecord, mergedChildren);
    }

    private <T extends AbstractSimpleEntity & RecordAwareChild> Set<T> mergeChildren(Set<T> newChildren,
            Set<T> oldChildren, boolean recordWillBeCloned, Long versionId) {
        EntityCollectionDiff<T> diff = new EntityCollectionDiff<>(oldChildren, newChildren, e -> Arrays.asList(e.getId()));

        Set<T> mergedChildren = new HashSet<>();

        log.debug("Diff between old:{} and new:{} is {}", oldChildren, newChildren, diff);

        for (T toCreate : diff.getCreatedElements()) {
            log.debug("Will persist child: {}", toCreate);
            em.persist(toCreate);
            log.debug("Persisted child: {}", toCreate);
            mergedChildren.add(toCreate);
        }

        for (Map.Entry<T, T> toModify : diff.getUpdatedElements().entrySet()) {
            T databaseChild = toModify.getKey();
            T newChild = toModify.getValue();
            boolean changed = !ReflectionComparator.compare(newChild, databaseChild).isEmpty();
            if (!changed) {
                log.debug("Child not changed, linking to previous: {}", databaseChild);
                mergedChildren.add(databaseChild);
            } else {
                // merge or clone child
                log.debug("Database child records: {}", databaseChild.getRecords());
                if (log.isDebugEnabled()) {
                    log.debug("Database child records versions: {}", databaseChild.getRecords().stream()
                            .map(r -> r.getVersionLinks().printActiveInterval())
                            .collect(toList()));
                }

                Set<Record> versionsOfParents = databaseChild.getRecords().stream()
                        .filter(record -> !record.isActiveSinceVersion(versionId))
                        .collect(toSet());
                boolean childNeedsToBeCloned = recordWillBeCloned || !versionsOfParents.isEmpty();
                if (childNeedsToBeCloned) { // the parent Record could need to be CLONED (1st change in a VER) or NOT (2nd change in a Ver)
                    log.debug("Child changed, will clone: {}", databaseChild);
                    newChild.setId(null);
                    em.persist(newChild);
                    log.debug("Created child: {}", newChild);
                } else {
                    log.debug("Will merge over child: {}", databaseChild);
                    //recordWillBeCloned is always true here
                    Optional<? extends Record> oldParentToUnlink = getOldParentToUnlink(databaseChild, newChild);
                    oldParentToUnlink.ifPresent(oldParent -> {
                        log.debug("Will remove parent {} from db child: {}" , oldParent, databaseChild);
                        databaseChild.removeFromParent(oldParent);
                        em.flush();
                    });

                    newChild = em.merge(newChild);
                    log.debug("Merged child: {}", newChild);
                }
                mergedChildren.add(newChild);
            }
        }

        for (T toDelete : diff.getDeletedElements()) {
            log.debug("Will unlink child: {}", toDelete);
            // NOTHING
        }
        return mergedChildren;
    }

    private <T extends AbstractSimpleEntity & RecordAwareChild> Optional<? extends Record> getOldParentToUnlink(T databaseChild, T newChild) {
        Record newParent = newChild.getRecords().iterator().next();
        Long newParentId = newParent.getId();
        return databaseChild.getRecords().stream()
                .filter(r -> r.getId().equals(newParentId))
                .findAny();
    }

}
