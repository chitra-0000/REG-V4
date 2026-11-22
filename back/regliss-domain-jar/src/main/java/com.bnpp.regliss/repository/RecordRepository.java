package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.record.AuditableRecord;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.vo.RecordIdType;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RecordRepository extends EntityRepository<Record, Long>, RecordRepositoryCustom {

    @Query("SELECT r FROM Record r "
            + "LEFT JOIN FETCH r.categories "
            + "LEFT JOIN FETCH r.bicCodes "
            + "LEFT JOIN FETCH r.isinCodes "
            + "LEFT JOIN FETCH r.cusipCodes "
            + "LEFT JOIN FETCH r.ibanCodes "
            + "LEFT JOIN FETCH r.programs "
            + "WHERE r.versionLinks.listId=?2 AND r.id IN ?1")
    Set<Record> getRecordsForVersionInListSearchFetchNeededData(Collection<Long> recordIds,Long listId);

    @Query("SELECT r.id, v FROM Record r "
            + "JOIN Version  v ON r.versionLinks.listId = v.list.id "
            + "LEFT JOIN FETCH v.list list "
            + "LEFT JOIN FETCH list.regulatorOrInternalPolicy autho "
            + " WHERE v.id IN (:idVersions) AND "
            +" r.versionLinks.fromVersionId<=v.id AND r.versionLinks.toVersionId>=v.id")
    Set<Object[]> getFullRecordsForOfac(@Param("idVersions") List<Long> idVersions);

    @Query("SELECT new com.bnpp.regliss.vo.RecordIdType(r.id, r.type, r.versionLinks.createdInVersionId,r.versionLinks.clonedInVersionId) FROM Record r " +
            "WHERE r.versionLinks.fromVersionId<=?1 and r.versionLinks.toVersionId>=?1 and r.versionLinks.listId=?2")
    Set<RecordIdType> getFullRecordIdsForGeneratedFile(Long idVersions,Long listId);

    @Query("SELECT new com.bnpp.regliss.vo.RecordIdType(record.id, record.type, record.versionLinks.createdInVersionId,record.versionLinks.clonedInVersionId) FROM Record record "
            + " WHERE "
            + " record.versionLinks.fromVersionId=:idNewVersion"
            + " AND record.versionLinks.listId=:idList")
    Set<RecordIdType> getAddModRecordIdsForGeneratedFile(@Param("idNewVersion") Long idNewVersion, @Param("idList") Long idList);

    @Query("SELECT record.id FROM Record record "
            + " WHERE "
            + jpqlCreatedRecordsCondition
            + " AND record.versionLinks.listId=:idList")
    Set<Long> getAddRecordIdsForGeneratedFile(@Param("idNewVersion") Long idNewVersion, @Param("idList") Long idList);

    @Query("SELECT record.id FROM Record record "
            + " WHERE "
            + jpqlUpdatedRecordsCondition
            + " AND record.versionLinks.listId=:idList")
    Set<Long> getModRecordIdsForGeneratedFile(@Param("idNewVersion") Long idNewVersion, @Param("idList") Long idList);

    @Query("SELECT record.id " +
            jpqlDeletedRecords
            + " AND record.versionLinks.listId=:idList")
    Set<Long> getSuppRecordIdsForGeneratedFile(@Param("idNewVersion") Long idNewVersion, @Param("idList") Long idList);

    @Query("SELECT new com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData(r.recordDJ.xmlSnippet, r.externalUuid, r.id) "
            + "FROM Record r "
            + "WHERE r.id IN ?1 "
            + "AND r.versionLinks.listId= ?2")
    List<RecordDjExportData> getNonConfidentialRecordsForDJXmlExport(List<Long> recordIdsPartition, Long listId);

    @Query("SELECT new com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData(r.recordDJ.xmlSnippet, r.reglissReference, r.id) "
            + "FROM Record r "
            + "WHERE r.id IN ?1 "
            + "AND r.versionLinks.listId= ?2")
    List<RecordDjExportData> getConfidentialRecordsForDJXmlExport(List<Long> recordIdsPartition, Long listId);

    @Query("SELECT new com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData(r.recordDJ.csvSnippet, r.externalUuid, r.id) "
            + "FROM Record r "
            + "WHERE r.id IN ?1 "
            + "AND r.versionLinks.listId= ?2")
    List<RecordDjExportData> getRecordsForWatchlistCsvExport(List<Long> recordIdsPartition, Long listId);

    @Query("SELECT r3.id, r.startDateStr "
            + " FROM Record r INNER JOIN Record r3 ON r.reglissReference = r3.reglissReference"
            + " WHERE  "
            + " r.versionLinks.clonedInVersionId is null "
            + " AND r.versionLinks.fromVersionId = r.versionLinks.createdInVersionId "
            + " AND r3.id IN ?1")
    List<Object[]> getFirstPublishedRecordForOfficialDateByRecordIds(List<Long> ids);

    @Query("SELECT r.id, v.versionNumber, v.startDate"
            + " FROM Record r "
            + " INNER JOIN Record rr ON rr.reglissReference = r.reglissReference and rr.versionLinks.listId = r.versionLinks.listId"
            + " INNER JOIN Version v on v.id = rr.versionLinks.clonedInVersionId "
            + " WHERE r.id IN ?1")
    List<Object[]> getHistoricalVersionsForRecordIds_updatedRaw(List<Long> ids);

    @Query("SELECT r.id, v.versionNumber, v.startDate"
            + " FROM Record r "
            + " INNER JOIN Version v ON v.id = r.versionLinks.createdInVersionId "
            + " WHERE r.id IN ?1")
    List<Object[]> getHistoricalVersionsForRecordIds_createdRaw(List<Long> ids);

    @Query("SELECT r FROM Record r JOIN Version v ON r.versionLinks.listId=v.list.id and v.id=?2 AND r.versionLinks.fromVersionId <= ?2 AND r.versionLinks.toVersionId >= ?2 " +
            "WHERE r.externalUuid = ?1  ")
    Optional<Record> findByUUIDAndVersionId(String uuid, Long versionId);

    @Query("SELECT r FROM Record r JOIN Version v ON r.versionLinks.listId=v.list.id and v.id=?2 AND r.versionLinks.fromVersionId <= ?2 AND r.versionLinks.toVersionId >= ?2 " +
            "WHERE r.externalReference = ?1  ")
    Optional<Record> findByExternalReferenceAndVersionId(String externalReference, Long versionId);
    @Query("SELECT r FROM Record r JOIN Version v  ON r.versionLinks.listId=v.list.id AND r.versionLinks.fromVersionId <= v.id AND r.versionLinks.toVersionId >= v.id JOIN v.list lst " +
            " WHERE r.recordId = ?1 AND v.versionNumber = ?2 and lst.reference=?3 and v.status<>com.bnpp.regliss.entity.VersionStatus.DELETED")
    Optional<Record> findByRecordIdAndVersionNoAndListReference(String recordId, Long versionNo,String listReference);

    @Query("SELECT r FROM Record r JOIN Version v  ON r.versionLinks.listId=v.list.id AND r.versionLinks.fromVersionId <= v.id AND r.versionLinks.toVersionId >= v.id JOIN v.list lst " +
            "WHERE r.externalReference = ?1 AND v.versionNumber = ?2 and lst.reference=?3 and v.status<>com.bnpp.regliss.entity.VersionStatus.DELETED")
    Optional<Record> findByRecordExternalRefferenceAndVersionNoAndListReference(String recordExtRef, Long versionNo,String listReference);

    Optional<Record> findByExternalUuid(String uuid);

    @Query("SELECT r FROM Record r WHERE r.externalUuid = ?1 "
            + " AND r.versionLinks.listId = ?2 ")
    Optional<Record> findByExternalUuidAndListId(String uuid, long listId);

    @Query("SELECT r FROM Record r WHERE r.reglissReference = ?1 "
            + " AND r.versionLinks.fromVersionId <=?2 "
            + " AND r.versionLinks.toVersionId >= ?2 "
            + " AND r.versionLinks.listId = ?3 ")
    Optional<Record> findByReglissReferenceAndVersionIdAndListId(String reglissrefernce, Long versionId, Long listId);

    @Query(nativeQuery = true, value = "SELECT v.certification_date, v.file_name "
            + "FROM record r "
            + "LEFT OUTER JOIN version v ON r.DELETED_IN_VERSION = v.id AND r.list_id=v.list_id "
            + "LEFT OUTER JOIN version_dj vdj ON v.version_dj_id =vdj.id "
            + "WHERE r.list_id= ?2  "
            + "AND v.certification_date is not null "
            + "AND r.regliss_reference = ?1")
    List<Object[]> getDeletionDateRaw(String recordReglissReference,Long listId);


    @Query(nativeQuery=true, value = "SELECT COUNT(*) FROM Record r JOIN version v ON r.list_id=v.list_id and v.id=?1 WHERE from_Version <= ?1 AND to_Version >= ?1 ")
    int countByVersion(Long versionId);

    @Query(nativeQuery = true, value = "SELECT DISTINCT(r.regliss_Reference) FROM record r where LIST_ID = ?1")
    Set<String> getAllReglissReferencesByList(Long listId);

    @Query("SELECT DISTINCT(r.recordId) FROM Record r WHERE r.versionLinks.listId = ?1")
    Set<String> getAllRecordIdsByList(Long listId);

    @Query(nativeQuery=true, value = "SELECT DISTINCT(a.REGLISS_REFERENCE) FROM RECORD r JOIN RECORD_ADDRESS ra on r.ID=ra.RECORD_ID JOIN ADDRESS a on a.ID=ra.ADDRESS_ID WHERE r.LIST_ID = ?1")
    Set<String> getAllAddressReferencesByList(Long listId);

    @Query("SELECT COUNT(r) FROM Record r JOIN Version v ON r.versionLinks.listId=v.list.id AND v.id=?1  WHERE r.versionLinks.fromVersionId<=?1 AND r.versionLinks.toVersionId>=?1 " +
            "and r.externalReference = ?2")
    int checkUniqueExternalReferences(Long versionId, String externalReference);

    @Query("SELECT r FROM Record r "
            + "JOIN Version v ON r.versionLinks.listId=v.list.id AND v.id=?1 "
            + "JOIN r.programs p "
            + " WHERE "
            +" r.versionLinks.fromVersionId<=?1 AND r.versionLinks.toVersionId>=?1"
            + " AND p = ?2")
    Set<Record> getAllByVersionAndProgram(Long versionId, Program originalProgram);

    /** Only used for testing. In prod there might be millions of records in a version */
    @Query("SELECT r FROM Record r JOIN Version v ON r.versionLinks.listId=v.list.id AND v.id=?1 WHERE "
            +" r.versionLinks.fromVersionId<=?1 AND r.versionLinks.toVersionId>=?1  ")
    List<Record> ___get_All_RecordsForVersion(Long versionId);

    @Query("SELECT count(a) from Alias a join a.records r  WHERE r.versionLinks.fromVersionId <= ?1 AND r.versionLinks.toVersionId>= ?1 AND r.versionLinks.listId=?2 " +
            "AND a.quality.businessConcept = ?3")
    Long getCountAliasForVersionAndQuality(Long idVersions, Long listId, ReferentialBusinessConcept quality);

    @Query("SELECT count(a) from Alias a join a.records r  WHERE r.versionLinks.fromVersionId <= ?1 AND r.versionLinks.toVersionId>= ?1 AND r.versionLinks.listId=?2 " +
            "AND a.quality.businessConcept != ?3")
    Long getCountAliasForVersionNotQuality(Long idVersions, Long listid, ReferentialBusinessConcept quality);


    @Modifying
    @Query(nativeQuery = true, value = "UPDATE RECORD SET TO_VERSION = ?3, DELETED_IN_VERSION = ?2 WHERE list_id=?4 and  ID IN ?1")
    void markRecordDeletion(List<Long> recordIds, Long versionId,Long previousVersionId, Long listId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "UPDATE RECORD SET LAST_MODIFICATION_DATE = ?1 WHERE ID IN (?2)")
    void saveToDB(String date , List<String> ids );


    @Query("SELECT r.externalReference, r.recordDJ.xmlSnippet, r.recordDJ.associationXml FROM Record r WHERE r.id IN ?1")
    List<Object[]> getExternalReferenceToXmlSnippetsRaw(List<Long> recordIds);

    @Query("SELECT r.id, r.recordDJ.associationXml FROM Record r WHERE r.id IN ?1 ")
    List<Object[]> getAssociationXmlSnippetsRaw(List<Long> recordIds);

    @Query("SELECT r.externalReference, " +
            "(SELECT max(a.reglissReference.suffix) FROM Alias a WHERE a.reglissReference.recordReglissReference = r.reglissReference " +
            "AND length(a.reglissReference.suffix) = (select max(length(ai.reglissReference.suffix)) from Alias ai where ai.reglissReference.recordReglissReference = r.reglissReference))" +
            "FROM Record r " +
            "WHERE r.id IN ?1")
    List<Object[]> getExternalReferenceToMaxAliasReferenceSuffixRaw(Set<Long> recordIds);

    @Query(nativeQuery = true, value = "SELECT r.external_Reference, r.id FROM Record r JOIN version v ON r.list_id=v.list_id and v.id=?1 WHERE FROM_VERSION  <= ?1 " +
            "and TO_VERSION>=?1")
    List<Object[]> getExternalReferenceToIdRaw(Long versionId);

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE RECORD SET TO_VERSION = ?2  WHERE list_id=?3 and ID IN ?1")
    void moveOldModRecordsToVersion(List<Long> oldRecordsModToBeRestrictedToPrevVersion, Long versionId, Long listId);

    @Query("FROM Record r WHERE r.versionLinks.listId=?2 and r.id IN (?1)")
    List<AuditableRecord> findById(List<Long> recordIds, Long listId);


    @Procedure("PURGE_ORPHAN_RECORD_DATA")
    String purgeOrphanRecordData();

    @Query("SELECT r.externalReference, r.recordDJ.id FROM Record r WHERE r.versionLinks.listId=?2 AND ( r.versionLinks.clonedInVersionId = ?1 OR (r.versionLinks.createdInVersionId = ?1 " +
            "and r.versionLinks.clonedInVersionId is null) )")
    List<Object[]> getExternalReferencesToUploadToRecordDJIdRaw(Long versionId,Long listId);

    @Query("SELECT r FROM Record r "
            + "LEFT JOIN FETCH r.associations "
            + "LEFT JOIN FETCH r.addresses "
            + "LEFT JOIN FETCH r.categories "
            + "LEFT JOIN FETCH r.sanctionTypes "
            + "LEFT JOIN FETCH r.aliases "
            + "LEFT JOIN FETCH r.genders "
            + "WHERE r.id=?1")
    Record findByIdFetchSomeChildrenForTest(Long recordId);

    @Query(nativeQuery = true,
            value = "SELECT r.ID, r.RECORD_TYPE, r.EXTERNAL_REFERENCE, rca_assoc.assExtRef, rca_assoc.assRelationType, r.CREATED_IN_VERSION_ID, r.CLONED_IN_VERSION_ID " +
                    "FROM RECORD r " +
                    "LEFT JOIN RECORD_ASSOCIATION ra ON ra.RECORD_ID = r.ID " +
                    "LEFT JOIN (SELECT a.ID as assId, a.EXTERNAL_REFERENCE as assExtRef, a.RELATION_TYPE as assRelationType " +
                    "           FROM RECORD ri " +
                    "           JOIN ASSOCIATION a ON a.EXTERNAL_REFERENCE = ri.EXTERNAL_REFERENCE " +
                    "           JOIN RECORD_CATEGORIES rc ON ri.ID = rc.RECORD_ID " +
                    "           WHERE rc.CATEGORY_ID IN ?1 " +
                    "           AND ri.FROM_VERSION <= ?2 AND ri.TO_VERSION>=?2 AND ri.LIST_ID = ?3) rca_assoc " +
                    "           ON rca_assoc.assId = ra.RECORD_ASSOCIATION_ID " +
                    "WHERE r.FROM_VERSION <= ?2 AND r.TO_VERSION>=?2 AND r.LIST_ID = ?3")
    List<Object[]> getRecordsToRcaAssociationExternalReferencesRaw(List<RecordCategory> rcaCategoryId, Long versionId, Long listId);

    @Query("SELECT r.id, r.type, association.externalReference, r.versionLinks.createdInVersionId, r.versionLinks.clonedInVersionId "
            + "FROM Record r "
            + "LEFT JOIN r.associations association "
            + "WHERE r.versionLinks.fromVersionId <= ?1 AND r.versionLinks.toVersionId>=?1 AND r.versionLinks.listId = ?2 ")
    List<Object[]> getFullRecordsToAssociationExternalReferencesRaw(Long versionId, Long listId);

    @Query("Select r FROM Record r join Version v on r.versionLinks.listId = v.list.id and v.id = ?2 " +
        " where r.versionLinks.fromVersionId <= ?2 and r.versionLinks.toVersionId >= ?2 and r.externalReference = ?1")
    Record getRecordByExternalReferenceAndVersionId(String externalReference, long versionId);

    @Query("SELECT r FROM Record r WHERE r.versionLinks.listId=?3 AND "
            +" r.versionLinks.fromVersionId<=?2 AND r.versionLinks.toVersionId>=?2 and r.versionLinks.createdInVersionId>?1")
    List<Record> getAddedRecordsBetweenVersions(Long versionId1, Long versionId2, Long listId);

    @Query("SELECT r FROM Record r WHERE r.versionLinks.listId=?3 AND "
            +" r.versionLinks.createdInVersionId<=?1 AND r.versionLinks.toVersionId>=?2 and r.versionLinks.clonedInVersionId is not null and r.versionLinks.clonedInVersionId>?1 " +
            "and r.versionLinks.clonedInVersionId<=?2")
    List<Record> getModifiedRecordsBetweenVersions(Long versionId1, Long versionId2, Long listId);

    @Query("SELECT r FROM Record r  WHERE r.externalReference IN "
            +"(SELECT r.externalReference FROM Record r WHERE r.versionLinks.listId=?3 and  r.versionLinks.createdInVersionId<=?1 and r.versionLinks.deletedInVersionId is not null " +
            "and r.versionLinks.deletedInVersionId>?1 and r.versionLinks.deletedInVersionId<=?2) " +
            +"and r.versionLinks.fromVersionId<=?1 AND r.versionLinks.toVersionId>=?1 "
            +"and r.versionLinks.listId=?3")
    List<Record> getDeletedRecordsBetweenVersions1(Long versionId1, Long versionId2, Long listId);

    @Query(nativeQuery = true, value = "WITH ext_ref AS " +
            "( SELECT EXTERNAL_REFERENCE  from RECORD " +
            " WHERE  LIST_ID=?3 and CREATED_IN_VERSION_ID<=?1 and DELETED_IN_VERSION IS NOT NULL and DELETED_IN_VERSION>?1 and DELETED_IN_VERSION<=?2) " +
            "SELECT * FROM RECORD r, ext_ref ref " +
            "WHERE r.EXTERNAL_REFERENCE IN ref.EXTERNAL_REFERENCE AND r.FROM_VERSION<=?1 and r.TO_VERSION>=?1 and r.LIST_ID=?3")
    List<Record> getDeletedRecordsBetweenVersions(Long versionId1, Long versionId2, Long listId);
}


