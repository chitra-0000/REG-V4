package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.RecordCumulatedXml;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.search.RecordSearchVersionCriteria;
import com.bnpp.regliss.statistics.VersionStatistics;
import com.bnpp.regliss.vo.*;
import com.bnpp.regliss.vo.export.ExportElementData;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;

import java.time.LocalDate;
import java.util.*;

/all/
public interface RecordRepositoryCustom {
    String jpqlRecordInVersionJoin = "JOIN Version ver ON record.versionLinks.listId=ver.list.id AND ver.id = :idNewVersion ";
    String jpqlRecordInVersionCondition = " record.versionLinks.fromVersionId<=:idNewVersion AND record.versionLinks.toVersionId>=:idNewVersion ";
    String jpqlCreatedRecordsCondition =" record.versionLinks.createdInVersionId = :idNewVersion AND record.versionLinks.clonedInVersionId IS NULL";
    String jpqlRecordTypePerson = " record.type ='PERSON'";
    String jpqlUpdatedRecordsCondition = " record.versionLinks.clonedInVersionId = :idNewVersion ";

    String jpqlDeletedRecords =                 //NOSONAR
            " FROM Record record "
            + " WHERE record.versionLinks.deletedInVersionId = :idNewVersion ";

    PaginatedResults<Record> searchForVersion(RecordSearchVersionCriteria criteria,Long listId);

    Long countCreatedRecordsForVersion(Long idNewVersion, Long idList );

    Long countCreatedPersonsForVersion(Long idNewVersion, Long idList );

    Long countUpdatedRecordsForVersion(Long idNewVersion, Long idList);

    Long countUpdatedPersonsForVersion(Long idNewVersion, Long idList);

    Long countDeletedRecordsForVersion(Long idNewVersion, Long idList);

    Long countDeletedPersonsForVersion(Long idNewVersion, Long idList);

    List<Program> getProgramsForChangedRecords(Long idNewVersion, Long idList);

    Map<String, RecordCumulatedXml> getExternalReferenceToXmlSnippets(List<Long> recordIds);

    //void deleteChildren(Class<?> entityName, Long versionId);

    //void linkClonedRecords(long versionId, Map<Long, Long> originalToClonedRecordId);

    int countRecordInAnyVersionByExternalReference(String externalReference);

    Map<String, Long> getExternalReferencesToUploadToRecordDJId(Long versionId, Long listId);

    Map<String, Long> getExternalReferenceToIdMap(Long versionId);

    Map<RecordFilteringIdentifiers, List<RecordAssociationVO>> getRecordsToRcaAssociationExternalReferencesMap(AssociationExternalReferenceCanonicFactory extReferenceCanonic,
                                                                                                               List<RecordCategory> rcaCategories, Version version);

    Map<RecordIdType, List<AssociationExternalReference>> getFullRecordsToAssociationExternalReferencesMap(Version version);

    //Map<Long, List<AssociationExternalReference>> getDeltaRecordsToAssociationExternalReferencesMap(Version version);

    Map<Long, Set<VersionNrStartDateVO>> getVersionsInWhichTheRecordWasModified(Collection<Long> asList);

    Optional<LocalDate> getDeletionDate(Record record);

    VersionStatistics countVersionStatistics(Long versionId,Long listId);

    Map<String, String> getExternalReferenceToMaxAliasReferenceSuffix(Set<Long> recordIds);

    List<RecordDjExportData> getDjXmlExportDataInPartitionByConfidentiality(List<Long> recordIdsPartition, ReglissList list);

    Map<Long, ExportElementData> getAssociationXmlSnippetsInPartitionByRecordTypes(List<Long> recordIdsPartition);

    List<RecordDjExportData> getDjCsvExportDataInPartitionByRecordTypesAndConfidentiality(List<Long> recordIdsPartition, ReglissList list);

    void markRecordsForPurgeFromVersion(Long versionId,Long listId, Integer timeout);
    void markRecordsForPurgeFromVersion(Long versionId,Long listId);

    void revertRecordsToVersion(Long revertedVersionId,Long listId);
    void revertRecordsToVersion(Long revertedVersionId,Long listId,Integer timeout);

}

