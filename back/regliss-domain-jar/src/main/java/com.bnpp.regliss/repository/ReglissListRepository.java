package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.service.permission.ListPermissionRequest;
import com.bnpp.regliss.vo.ListBreadcrumbVO;
import com.bnpp.regliss.vo.ListImportExportVO;
import com.bnpp.regliss.vo.ListIncludedInConcatenatedListVO;
import com.bnpp.regliss.vo.MappingColumnVO;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReglissListRepository extends EntityRepository<ReglissList, Long>, ReglissListRepositoryCustom {

    String SELECT_LIST_PERMISSION_REQUEST = "SELECT new com.bnpp.regliss.service.permission.ListPermissionRequest("
            + "lst.id, lst.confidentiality, lst.scope, lst.country.id"
            + ") FROM ReglissList lst ";

    @Query(SELECT_LIST_PERMISSION_REQUEST
            + "WHERE lst.deleted = false")
    List<ListPermissionRequest> findAllNotDeletedEagerForPermissions();

    @Query(SELECT_LIST_PERMISSION_REQUEST
            + "WHERE lst.id = ?1")
    ListPermissionRequest getListPermissionRequest(long listId);

    List<ReglissList> findByDeletedFalse();

    @Query(("SELECT lst from ReglissList lst  "
            + " LEFT JOIN lst.versions v "
            + " WHERE lst.deleted = false and lst.active = true and v.certificationDate IS NOT NULL"))
    List<ReglissList> findAllActiveWithCertifiedVersions();

    int countByReferenceAndDeletedFalse(String reference);

    @Query("SELECT lst from ReglissList lst where to_number(lst.reference) = ?1 and lst.deleted = false")
    List<ReglissList> findByRefAsNumber(int ref);

    Optional<ReglissList> findByReferenceAndDeletedFalse(String reference);

    int countByShortNameIgnoreCaseAndDeletedFalse(String shortName);

    int countByNameIgnoreCaseAndDeletedFalse(String name);

    Optional<ReglissList> findById(long listId);

    @Query("SELECT COUNT(*) FROM ReglissList "
            + "WHERE id != ?1 "
            + "AND importConfiguration.importFileType = ?2 "
            + "AND deleted = false "
            + "AND importConfiguration.allowAutoUpload = true")
    int countOtherAutomaticActiveListsByFormat(long id, ImportFileType format);

    @Query("SELECT COUNT(*) FROM ReglissList "
            + "WHERE importConfiguration.importFileType = ?1 "
            + "AND deleted = false "
            + "AND importConfiguration.allowAutoUpload = true")
    int countActiveAndAutomaticListsByFormat(ImportFileType format);

    @Query("SELECT list from ReglissList list "
            + "WHERE importConfiguration.importFileType = ?1 "
            + "AND  deleted = false "
            + "AND  importConfiguration.allowAutoUpload = true "
            + "AND  active = true")
    List<ReglissList> findByDJFormatNotDeleted(ImportFileType format);

    @Query("SELECT list from ReglissList list "
            + "WHERE deleted = false "
            + "AND  importConfiguration.allowAutoUpload = true "
            + "AND  active = true")
    List<ReglissList> findAllAutomaticNotDeleted();

    @Query("SELECT l.id, l.reference || ' - ' || l.shortName FROM ReglissList l where l.deleted = false")
    List<Object[]> getAllListExportNames();

    @Query(nativeQuery=true,value="SELECT l.short_name as shortName,l.reference as reference,l.id as id FROM Regliss_list l where l.is_deleted = 0 order by lpad(reference,3,'0')")
    List<ListShortNameDto> getAllListShortNames();

    @Query("SELECT list FROM ReglissList list LEFT JOIN FETCH list.versions WHERE list.id = ?1")
    ReglissList findByIdFetchVersions(long listId);

    @Query(nativeQuery = true, value = "select 1-count(*) from Version v where v.list_id = ?1 and v.status not in ('CERTIFIED', 'DELETED')")
    int canDeativate(long listId);

    @Query(nativeQuery = true, value = "select 1-count(*) from Version v where v.list_id = ?1 and v.status in ('CERTIFIED')")
    int canDelete(long listId);

    @Query("SELECT list.id as id, list.reference as reference, list.shortName as shortName,"
            +" list.name as name, list.description as description,"
            +" list.regulatorOrInternalPolicy as regulatorOrInternalPolicy,"
            +" list.type as type, list.confidentiality as confidentiality, list.scope as scope,"
            +" list.country as country, list.owner as owner, list.issuer as issuer, list.listCategory as listCategory,"
            +" list.active as active, list.globalPlusOfac as globalPlusOfac, list.geographicData as geographicData  FROM ReglissList"
            +" list "
            +" Left JOIN list.geographicData WHERE list.id = ?1")
    Optional<ListCharacteristicsDto> getListCharacteristicsById(long listId);

    @Query("SELECT list.id as id, list.genericFormat as genericFormat, list.reglissFormat as reglissFormat, "
            + "list.vigilanceFormat as vigilanceFormat, list.safewatchFormat as safewatchFormat, list.fircosoftFormat as fircosoftFormat, "
            + "list.importConfiguration.fileNamePrefix as fileName, list.importConfiguration.importFileType as importFileType, " +
            "list.importConfiguration.checksumReception as checksumReception, "
            + "list.importConfiguration.allowIncrementalImport as allowIncrementalImport, list.importConfiguration.logReception as logReception, "
            + "list.importConfiguration.confirmationRequired as confirmationRequired, list.importConfiguration.autoConfirmationThreshold as autoConfirmationThreshold, "
            + "list.importConfiguration.allowAutoUpload as allowAutoUpload, list.importConfiguration.allowManualUpload as allowManualUpload, mf.fileName as mappingFile "
            + "FROM ReglissList list "
            + "LEFT JOIN MappingFile mf "
            + "ON list.id=mf.list.id "
            + "WHERE list.id = ?1 AND (mf.deleted = false OR mf is null)")
    Optional<ListImportExportVO> getListImportExportById(long listId);

    @Query("SELECT mc.columnLabel as columnLabel, mc.fieldCode as fieldCode FROM MappingColumn mc, MappingFile mf WHERE mf.list.id = ?1 " +
            "AND mf.deleted=false AND mf.id = mc.mappingFile.id order by column_index asc")
    List<MappingColumnVO> getMappingColumnsById(long listId);

    @Query("SELECT list.id as id, list.reference as reference, list.name as name, list.shortName as shortName, list.importConfiguration.importFileType as importFileType, "
            +"list.confidentiality as confidentiality, list.scope as scope, list.country as country, "
            +"list.importConfiguration.allowManualUpload as allowManualUpload, list.active as active "
            +"FROM ReglissList list WHERE list.id=?1")
    ListBreadcrumbVO getListBreadcrumb(long listId);

    @Query("SELECT list.id as id,list.reference as reference,list.shortName as shortName,list.name as name " +
            "FROM ReglissList list where list.active = 1 AND list.deleted = false " +
            "AND list.importConfiguration.importFileType NOT IN ('DOW_JONES_WATCHLIST', 'DOW_JONES_AME') ORDER BY list.reference ASC")
    List<ListForConcatenatedLists> getAllNotAmeOrWl();


    @Query("SELECT list.name as name, lastV.publicationDate as lastPublicationDate " +
            "FROM ReglissList list " +
            "LEFT JOIN ListLastCertifiedVersion lv ON lv.listId = list.id "+
            "LEFT JOIN Version lastV ON lv.versionId = lastV.id " +
            "WHERE list.id IN ?1")
    List<ListIncludedInConcatenatedListVO> getAllListsIncludedInConcatenatedList(List<Long> listIds);


    @Query("SELECT COUNT(list) FROM ReglissList list "
            + "WHERE list.importConfiguration.importFileType = 'CRB_LIST' "
            + "AND list.deleted = false "
            + "AND list.active = true "
            + "AND list.id != ?1 "
            + "AND (SELECT COUNT(v) FROM Version v WHERE v.list.id = list.id) > 0 ")
    Optional<Long> getCountCRBLists(Long id);

    @Query("SELECT list from ReglissList list "
            + "WHERE reference IN ?1 "
            + "AND  deleted = false "
            + "AND  active = true")
    List<ReglissList> findByListRef(List<String> ref);

    @Query("SELECT list FROM ReglissList list "
            + "WHERE list.importConfiguration.importFileType IN ('SIX_STRUCTURED_FILE','SIX_INSTRUMENTS_FILE','SIX_OPTIONS_FILE') "
            + "AND list.deleted = false "
            + "AND list.active = true "
            + "AND list.id != ?1 "
            + "AND (SELECT COUNT(v) FROM Version v WHERE v.list.id = list.id) > 0 ")
    List<ReglissList> getAllSixLists(Long id);

}

