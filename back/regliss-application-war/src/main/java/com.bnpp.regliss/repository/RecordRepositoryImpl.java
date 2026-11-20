package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.RecordCumulatedXml;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.CodeType;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.exporter.writer.format.dj.DJAssociationXmlExportElementData;
import com.bnpp.regliss.monitoring.PerformanceMonitor;
import com.bnpp.regliss.repository.PaginationService.SortByEnum;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.search.RecordSearchVersionCriteria;
import com.bnpp.regliss.statistics.VersionPersonStatistics;
import com.bnpp.regliss.statistics.VersionStatistics;
import com.bnpp.regliss.utils.FeedIdHelper;
import com.bnpp.regliss.utils.ReflectionUtils;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import com.bnpp.regliss.vo.*;
import com.bnpp.regliss.vo.export.ExportElementData;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.*;

@Slf4j
public class RecordRepositoryImpl extends BaseRepositoryImpl<Record> implements RecordRepositoryCustom {
    private static final String  AND  = " AND ";
    private static final String ID_NEW_VERSION = "idNewVersion";
    private static final String ID_LIST = "idList";

    @Autowired
    private PaginationService paginationService;

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private ProgramRepository programRepo;

    public PaginatedResults<Record> searchForVersion(RecordSearchVersionCriteria criteria,Long listId) {
        ReflectionUtils.trimAllStringFields(criteria);
        Map<String, Object> params = new HashMap<>();
        String jpql = "FROM Record r  ";
        jpql += " WHERE" +
                " r.versionLinks.listId = :idList " +
                " AND r.versionLinks.fromVersionId <=:idVersion " +
                " AND r.versionLinks.toVersionId >= :idVersion " ;
        params.put("idVersion", criteria.idVersion);
        params.put(ID_LIST, listId);


        if (StringUtils.isNotBlank(criteria.name)) {
            jpql += AND +" ( "+ TextNormalizerQueryUtil.createStringComparisonJpql("r.nameNormalized", "name", criteria.name) + " ";
            jpql += " OR " + TextNormalizerQueryUtil.createStringComparisonJpql("r.maidenNameNormalized", "name", criteria.name) ;
            if(criteria.isAlias){
                jpql += " OR EXISTS ( SELECT al from r.aliases al WHERE " + TextNormalizerQueryUtil.createStringComparisonJpql("al.fullNameNormalized", "name", criteria.name)+")";
            }
            jpql += " ) ";
            params.put("name", TextNormalizerQueryUtil.normalize(criteria.name));
        }

        if(StringUtils.isNotBlank(criteria.codeStr)){


            if (EnumSet.of(CodeType.IMO, CodeType.REGLISS_REFERENCE, CodeType.RECORD_ID, CodeType.EXTERNAL_REFERENCE,CodeType.SEARCH_CODE).contains(criteria.codeType)) {
                jpql += AND+ TextNormalizerQueryUtil.createStringComparisonJpql(" r."+criteria.codeType.getJpql(), "code", criteria.codeStr);
                params.put("code",criteria.codeStr);
            }else {
                jpql += " AND EXISTS (SELECT c FROM r."+criteria.codeType.getJpql()+" c "
                +" WHERE "
                +TextNormalizerQueryUtil.createStringComparisonJpql(" UPPER(c.code) ", "code", criteria.codeStr) +" ) " ;
                params.put("code",criteria.codeStr.toUpperCase());
            }
        }

        if (StringUtils.isNotBlank(criteria.program)) {
            jpql += " AND EXISTS (SELECT p FROM r.programs p WHERE "
                    + TextNormalizerQueryUtil.createStringComparisonJpql("p.labelNormalized", "program", criteria.program) + " ) ";
            params.put("program", TextNormalizerQueryUtil.normalize(criteria.program));
        }

        if (!criteria.recordTypes.isEmpty()) {
            jpql += " AND r.type IN :recordTypes ";
            params.put("recordTypes", criteria.recordTypes);
        }

        PageRequest pageRequest = paginationService.toPageRequestWithAtMostOneDefaultOrder(criteria.paginationOptions, SEARCH_IN_VERSION_SORT_MAPPING);
        log.info("Searching for 1 page");
        List<Long> resultIds = paginationService.createPaginatedQuery("SELECT r.id " + jpql, params, pageRequest).getResultList();
        log.info("Counting total");
        long totalCount = paginationService.countTotal("SELECT COUNT(*) " + jpql, params);

        if (resultIds.isEmpty()) {
            return new PaginatedResults<>(emptyList(), 0);
        }
        log.info("Loading record data for 1 page");
        Set<Record> recordData = recordRepo.getRecordsForVersionInListSearchFetchNeededData(resultIds, listId);
        log.info("Search done");
        Map<Long,Record> recordDataMap = recordData.stream().collect(toMap(Record::getId, r->r));
        List<Record> results = resultIds.stream().map(recordDataMap::get).collect(toList());
        return new PaginatedResults<>(results, totalCount);
    }

    static final Map<String, Object> SEARCH_IN_VERSION_SORT_MAPPING = Maps.newHashMap(
            ImmutableMap.of("type", new SortByEnum<>("r.type", RecordType.class),
                    "record", "UPPER(r.name)",
                    "reference", "UPPER(r.reglissReference)")
    );

    private final static String jpqlCreatedRecords =" FROM Record record " //NOSONAR
                + " WHERE versionLinks.listId = :idList "
                + AND + jpqlCreatedRecordsCondition;

    private final static String jpqlUpdatedRecords = " FROM Record record" //NOSONAR
                        + " WHERE versionLinks.listId = :idList "
                        + AND + jpqlUpdatedRecordsCondition;

    private Map<String, Object> constructParameterMap(Long idNewVersion, Long idList) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(ID_NEW_VERSION, idNewVersion);
        parameters.put(ID_LIST, idList);
        return parameters;
    }

    @Override
    public Long countCreatedRecordsForVersion(Long idNewVersion, Long idList) {
        return paginationService.countTotal(
                "SELECT COUNT(record) " + jpqlCreatedRecords,  //NOSONAR
                constructParameterMap(idNewVersion, idList));
    }

    @Override
    public Long countCreatedPersonsForVersion(Long idNewVersion, Long idList) {
        return paginationService.countTotal(
                "SELECT COUNT(record) " + jpqlCreatedRecords + AND+ jpqlRecordTypePerson,  //NOSONAR
                constructParameterMap(idNewVersion, idList));
    }

    @Override
    public Long countUpdatedRecordsForVersion(Long idNewVersion, Long idList) {
        return paginationService.countTotal("SELECT COUNT(record) " + jpqlUpdatedRecords,
                constructParameterMap(idNewVersion, idList));
    }

    @Override
    public Long countUpdatedPersonsForVersion(Long idNewVersion, Long idList) {
        return paginationService.countTotal("SELECT COUNT(record) " + jpqlUpdatedRecords + AND+ jpqlRecordTypePerson,
                constructParameterMap(idNewVersion, idList));
    }


    private final static String jpqlDeletedRecords =
            " FROM Record record "                                      //NOSONAR
            + " where versionLinks.deletedInVersionId = :idNewVersion "
            + " AND versionLinks.listId = :idList ";


    @Override
    public Long countDeletedRecordsForVersion(Long idNewVersion, Long idList) {
        Map<String, Object> params = constructParameterMap(idNewVersion, idList);
        return paginationService.countTotal("SELECT COUNT(record) " + jpqlDeletedRecords, params);
    }

    @Override
    public Long countDeletedPersonsForVersion(Long idNewVersion, Long idList) {
        Map<String, Object> params = constructParameterMap(idNewVersion, idList);
        return paginationService.countTotal("SELECT COUNT(record) " + jpqlDeletedRecords + AND+ jpqlRecordTypePerson, params);
    }

    @Override
    public List<Program> getProgramsForChangedRecords(Long idNewVersion, Long idList) {
        return new ArrayList<>(programRepo.getCreatedModifiedDeletedRecordsProgramsForVersion(idNewVersion,idList));
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, RecordCumulatedXml> getExternalReferenceToXmlSnippets(List<Long> recordIds) {
        if (recordIds.isEmpty()) {
            return emptyMap();
        }

        return recordRepo.getExternalReferenceToXmlSnippetsRaw(recordIds)
                .stream()
                .collect(toMap(arr -> (String)arr[0], arr -> new RecordCumulatedXml((String)arr[1], (String)arr[2])));
    }

    @Override
    public Optional<LocalDate> getDeletionDate(Record record) {
        List<Object[]> deletionInfos = recordRepo.getDeletionDateRaw(record.getReglissReference(), record.getVersionLinks().getListId());
        if(deletionInfos.isEmpty()) {
            return Optional.empty();
        }
        ReglissList list = listRepo.getExactlyOne(record.getVersionLinks().getListId());
        Object[] deletionInfo = deletionInfos.get(0);

        switch (list.getImportFileType()) {
            case DOW_JONES_WATCHLIST:
            case DOW_JONES_CORE:
            case DOW_JONES_AME:
                return getAutoUploadDeletionDate(list, deletionInfo);

            case MANUAL_PROCESSING:
            case CRB_LIST:
            case NOT_SUPPORTED:
                return getManualUploadDeletionDate(deletionInfo);

            case CUSTOM:
            case SIX_MAIN_FILE:
                Optional<LocalDate> autoUploadDeletionDate = getAutoUploadDeletionDate(list, deletionInfo);
                Optional<LocalDate> manualUploadDeletionDate = getManualUploadDeletionDate(deletionInfo);
                return autoUploadDeletionDate.isPresent() ? autoUploadDeletionDate : manualUploadDeletionDate;

            default:
                throw new IllegalArgumentException("Import file type not supported: " + list.getImportFileType());
        }
    }

    private Optional<LocalDate> getManualUploadDeletionDate(Object[] deletionInfo) {
        return Optional.of(((Timestamp) deletionInfo[0]).toLocalDateTime().toLocalDate());
    }

    private Optional<LocalDate> getAutoUploadDeletionDate(ReglissList list, Object[] deletionInfo) {
        String autoUploadedFileName = (String) deletionInfo[1];

        if (autoUploadedFileName != null) {
            return FeedIdHelper.feedIdFromFilename(autoUploadedFileName, list.getImportFileType()).getDateOpt();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public int countRecordInAnyVersionByExternalReference(String externalReference) {
        return ((Long)entityManager.createQuery("SELECT COUNT(r) FROM Record r WHERE r.externalReference = :extRef")
                .setParameter("extRef", externalReference)
                .getSingleResult()).intValue();
    }

    @Override
    public Map<String, Long> getExternalReferencesToUploadToRecordDJId(Long versionId, Long listId) {
        return recordRepo.getExternalReferencesToUploadToRecordDJIdRaw(versionId,listId).stream()
                .collect(toMap(arr -> (String) arr[0], arr -> ((Number) arr[1]).longValue()));
    }

    @Override
    public Map<String, Long> getExternalReferenceToIdMap(Long versionId) {
        return recordRepo.getExternalReferenceToIdRaw(versionId).stream()
                .collect(toMap(arr -> (String) arr[0], arr -> ((Number) arr[1]).longValue()));
    }

    @Override
    public Map<RecordFilteringIdentifiers, List<RecordAssociationVO>> getRecordsToRcaAssociationExternalReferencesMap( AssociationExternalReferenceCanonicFactory extReferenceCanonic,
                                                                                                                      List<RecordCategory> rcaCategories, Version version) {
        Function<Object[], RecordFilteringIdentifiers> extractRecordIdentifiers = arr -> new RecordFilteringIdentifiers(new RecordIdType(((Number) arr[0]).longValue(),
                RecordType.valueOf((String)arr[1]), arr[5]!=null ? ((Number) arr[5]).longValue():-1L,arr[6]!=null ?((Number) arr[6]).longValue(): -1L),
                extReferenceCanonic.getCanonic((String) arr[2]));
        Function<Object[], RecordAssociationVO> extractRecordAssociationVO = arr -> new RecordAssociationVO(extReferenceCanonic.getCanonic((String) arr[3]),
                arr[4] != null ? ((Number) arr[4]).longValue() : null);

        return recordRepo.getRecordsToRcaAssociationExternalReferencesRaw(rcaCategories, version.getId(), version.getList().getId()).stream()
                .collect(groupingBy(extractRecordIdentifiers,
                        mapping(extractRecordAssociationVO, toList())));
    }

    @Override
    public Map<RecordIdType, List<AssociationExternalReference>> getFullRecordsToAssociationExternalReferencesMap(Version version) {
        AssociationExternalReferenceCanonicFactory associationExtRefFactory = new AssociationExternalReferenceCanonicFactory();

        return recordRepo.getFullRecordsToAssociationExternalReferencesRaw(version.getId(),version.getList().getId()).stream()
                .collect(groupingBy(arr -> new RecordIdType((Long) arr[0], (RecordType)arr[1], arr[3]!=null ? ((Number) arr[3]).longValue():-1L, arr[4]!=null?((Number) arr[4]).longValue():-1L),
                        mapping(arr -> associationExtRefFactory.getCanonic((String) arr[2]), toList())));
    }

    @Override
    public Map<Long, Set<VersionNrStartDateVO>> getVersionsInWhichTheRecordWasModified(Collection<Long> recordIds) {
        log.info("Fetching version details for {} records...", recordIds.size());
        Function<Object[], VersionNrStartDateVO> extractVersionNrStartDateVO =
                arr -> new VersionNrStartDateVO((Long) arr[1], (LocalDate) arr[2]);

        Map<Long, Set<VersionNrStartDateVO>> modifiedVersions = new HashMap<>();
        for (List<Long> recordIdsPartition : Iterables.partition(recordIds, 1000)) {
            PerformanceMonitor.before(PerformanceMonitor.Action.EXPORT_LOAD_HISTORICAL_VERSIONS);
            List<Object[]> createdResults = recordRepo.getHistoricalVersionsForRecordIds_createdRaw(recordIdsPartition);
            List<Object[]> updatedResults = recordRepo.getHistoricalVersionsForRecordIds_updatedRaw(recordIdsPartition);

            List<Object[]> allResults = new ArrayList<>(createdResults.size() + updatedResults.size());
            allResults.addAll(createdResults);
            allResults.addAll(updatedResults);

            Map<Long, Set<VersionNrStartDateVO>> versionDetailsForThisChunk = allResults.stream()
                    .collect(groupingBy(arr ->(Long) arr[0], mapping(extractVersionNrStartDateVO, toSet())));
            modifiedVersions.putAll(versionDetailsForThisChunk);
            PerformanceMonitor.after(PerformanceMonitor.Action.EXPORT_LOAD_HISTORICAL_VERSIONS);
        }
        log.debug("Done fetching record historical versions for {} records", modifiedVersions.size());
        return modifiedVersions;
    }

    @Override
    public VersionStatistics countVersionStatistics(Long versionId, Long listId) {
        long createdRecords = countCreatedRecordsForVersion(versionId, listId);
        long updatedRecords = countUpdatedRecordsForVersion(versionId, listId);
        long deletedRecords = countDeletedRecordsForVersion(versionId, listId);
        long totalRecords = recordRepo.countByVersion(versionId);
        long strongRecords = recordRepo.getCountAliasForVersionNotQuality(versionId, listId, ReferentialBusinessConcept.ALIAS_QUALITY_WEAK);
        long weakRecords = recordRepo.getCountAliasForVersionAndQuality(versionId, listId,ReferentialBusinessConcept.ALIAS_QUALITY_WEAK);
        long addedPersons=countCreatedPersonsForVersion(versionId,listId);
        long updatedPersons=countUpdatedPersonsForVersion(versionId,listId);
        long deletedPersons=countDeletedPersonsForVersion(versionId,listId);
        VersionPersonStatistics versionPersonStatistics=new VersionPersonStatistics(addedPersons,updatedPersons,deletedPersons);
        return new VersionStatistics(createdRecords, updatedRecords, deletedRecords, totalRecords, strongRecords, weakRecords,versionPersonStatistics);
    }

    @Override
    public Map<String, String> getExternalReferenceToMaxAliasReferenceSuffix(Set<Long> recordIds) {
        return recordRepo.getExternalReferenceToMaxAliasReferenceSuffixRaw(recordIds)
                .stream()
                .collect(HashMap::new, (m, e) -> m.put((String)e[0], (String)e[1]), HashMap::putAll);
    }

    @Override
    public List<RecordDjExportData> getDjXmlExportDataInPartitionByConfidentiality(List<Long> recordIdsPartition, ReglissList list) {
        Long listId = list.getId();
        if(list.getConfidentiality() == Confidentiality.CONFIDENTIAL) {
            return recordRepo.getConfidentialRecordsForDJXmlExport(recordIdsPartition, listId);
        }
        return recordRepo.getNonConfidentialRecordsForDJXmlExport(recordIdsPartition, listId);
    }

    @Override
    public Map<Long, ExportElementData> getAssociationXmlSnippetsInPartitionByRecordTypes(List<Long> recordIdsPartition){
        return toXmlAssociationSnippets(recordRepo.getAssociationXmlSnippetsRaw(recordIdsPartition));
    }

    private Map<Long, ExportElementData> toXmlAssociationSnippets(List<Object[]> raw) {
        return raw.stream()
                .filter(arr -> arr[1] != null)
                .collect(toMap(arr -> (Long) arr[0], arr -> new DJAssociationXmlExportElementData((Long) arr[0], (String) arr[1])));
    }

    @Override
    public List<RecordDjExportData> getDjCsvExportDataInPartitionByRecordTypesAndConfidentiality(List<Long> recordIdsPartition, ReglissList list) {
        return recordRepo.getRecordsForWatchlistCsvExport(recordIdsPartition, list.getId());
    }

    @Override
    public void markRecordsForPurgeFromVersion(Long versionId, Long listId, Integer timeout) {

        entityManager.createNamedQuery("markRecordsForPurge",Record.class)
            .setHint("javax.persistence.query.timeout",timeout)
            .setParameter(1, versionId)
            .setParameter(2, listId)
            .executeUpdate();
    }
    @Override
    public void markRecordsForPurgeFromVersion(Long versionId,Long listId) {
        entityManager.createNamedQuery("markRecordsForPurge", Record.class)
                .setParameter(1, versionId)
                .setParameter(2, listId)
                .executeUpdate();
    }

    @Override
    public void revertRecordsToVersion(Long revertedVersionId,Long listId, Integer timeout){
        entityManager.createNamedQuery("revertRecordsToVersion",Record.class)
                .setHint("javax.persistence.query.timeout",timeout)
                .setParameter(1, revertedVersionId)
                .setParameter(2, listId)
                .executeUpdate();
    }

    @Override
    public void revertRecordsToVersion(Long revertedVersionId,Long listId){
        entityManager.createNamedQuery("revertRecordsToVersion", Record.class)
                .setParameter(1, revertedVersionId)
                .setParameter(2, listId)
                .executeUpdate();
    }
}
