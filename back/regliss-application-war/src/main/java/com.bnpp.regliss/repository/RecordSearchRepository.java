package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.ReglissListType;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.record.CodeType;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.search.RecordSearchCriteria;
import com.bnpp.regliss.utils.PerformanceUtil;
import com.bnpp.regliss.utils.ReflectionUtils;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import com.bnpp.regliss.vo.RecordAndVersionForSearchVO;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

import javax.persistence.Query;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

/*This class is a repository that contains a lot of String literals mostly used in the construction of native sql.
 * In this case trying to factor into constants all the repeated literals
 * that appear in queries will just complicate them further.
 * For this reason we mark sonar rule: <String literals should not be duplicated -> squid:S1192> as false positive in this
 * class. Also extensive testing was performed on this queries through com.bnpp.regliss.repository.RecordSearchBehaviorLocalIT
 * behavior driven tests.
 */
@Service
public class RecordSearchRepository {
    private static final String AND = " AND ";
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordSearchRepository.class);
    private static final String LIST_TYPES = "listTypes";

    @Autowired
    private PaginationService paginationService;

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private ReglissListRepository listRepo;

    private static class RecordSearchQueryData {
        public String query = "";
        public Map<String, Object> params = new HashMap<>();
    }
    public static class CertifiedVersion{
        public Long listId;
        public Long versionId;
        public Long versionNo;
        public CertifiedVersion(Object[] arr) {
            listId=((Number)arr[0]).longValue();
            versionId=((Number)arr[1]).longValue();
            versionNo=((Number)arr[2]).longValue();
        }
        public Long getListId() { return listId; }
    }

    /unchecked/
    public List<RecordAndVersionForSearchVO> queryForPage(RecordSearchCriteria criteria, Set<Long> visibleListIds) {
        visibleListIds = getSearchedLists(visibleListIds,criteria);
        if (visibleListIds.isEmpty()) {
            return emptyList();
        }
        List<Object[]> latestCertifiedVersionsList = versionRepo.getAllLatestCertifiedVersions();
        Map<Long,CertifiedVersion>latestCertifiedVersionsMap= latestCertifiedVersionsList.stream().map( CertifiedVersion::new).collect(Collectors.toMap(CertifiedVersion::getListId, e->e));
        RecordSearchQueryData queryData = constructNativeQuery(criteria, visibleListIds);

        Sort defaultSort = Sort.by(Direction.ASC, "EXTERNAL_REFERENCE", "FROM_VERSION_ID");
        PageRequest pageRequest = paginationService.toPageRequestWithMultipleDefaultOrders(criteria.paginationOptions, searchSortMapping(), defaultSort);

        Query query = paginationService.createNativePaginatedQuery(
                "SELECT r.id, r.FROM_VERSION_ID , r.FROM_VERSION_NO, r.TO_VERSION_ID , r.TO_VERSION_NO,  r.DELETED_IN_VERSION_ID, r.confidentiality, r.lst_short_name, r.record_id , r.list_id "
                        + queryData.query,
                queryData.params, pageRequest);

        LOGGER.debug("Launching record search query...");
        List<Object[]> resultsObjs =PerformanceUtil.runAndMeasure("RecordSearchQuery ", query::getResultList);

        LOGGER.debug("Query returned {} items", resultsObjs.size());

        Set<Long> recordIds = resultsObjs.stream().map(arr -> ((Number)arr[0]).longValue()).collect(toSet());
        LOGGER.debug("Loading details of a page of records...");
        Map<Long, Record> loadedRecords = recordRepo.findAllById(recordIds).stream().collect(toMap(Record::getId, identity()));

        return resultsObjs.stream()
                .map(arr -> mapSearchResultToVO(loadedRecords, arr,latestCertifiedVersionsMap))
                .collect(toCollection(LinkedList::new));
    }

    private RecordAndVersionForSearchVO mapSearchResultToVO(Map<Long, Record> loadedRecords, Object[] arr,Map<Long,CertifiedVersion> latestCertifiedVersionsMap) {
        RecordAndVersionForSearchVO recordAndVersionVO = new RecordAndVersionForSearchVO();
        CertifiedVersion lastCertifiedVersion = latestCertifiedVersionsMap.get(((Number)arr[9]).longValue());
        recordAndVersionVO.setRecord(loadedRecords.get(((Number) arr[0]).longValue()));
        recordAndVersionVO.setFromVersionId(((Number) arr[1]).longValue());
        recordAndVersionVO.setFromVersionNumber(toLongOrNull(arr[2]));
        recordAndVersionVO.setToVersionId(mapToToVersionId(toLongOrNull(arr[3]), lastCertifiedVersion));
        recordAndVersionVO.setToVersionNumber(mapToToVersionNo(toLongOrNull(arr[4]), lastCertifiedVersion));
        recordAndVersionVO.setActive(isRecordActive(((Number) arr[5])));
        recordAndVersionVO.setDeletedInVersionId(toLongOrNull(arr[5]));
        recordAndVersionVO.setListConfidentiality(Confidentiality.valueOf(arr[6].toString()));
        recordAndVersionVO.setListShortName((String) arr[7]);
        return recordAndVersionVO;
    }

    private Long mapToToVersionId(Long versionId,CertifiedVersion latestCertifiedVersion){
        return versionId==null?latestCertifiedVersion.versionId:versionId;
    }
    private Long mapToToVersionNo(Long versionNo ,CertifiedVersion latestCertifiedVersion){
        return versionNo==null?latestCertifiedVersion.versionNo:versionNo;
    }

    private Long toLongOrNull(Object field) {
        if (field==null) return null;
        return ((Number)field).longValue();
    }
    private boolean isRecordActive(Number deletedInVersionId) { return deletedInVersionId==null; }

    public long queryForTotal(RecordSearchCriteria criteria, Set<Long> visibleListIds) {
        visibleListIds = getSearchedLists(visibleListIds,criteria);
        if (visibleListIds.isEmpty()) {
            return 0L;
        }
        RecordSearchQueryData queryData = constructNativeQuery(criteria, visibleListIds);

        return paginationService.countTotalNative("SELECT COUNT(r.record_id) " + queryData.query, queryData.params);
    }

    private RecordSearchQueryData constructNativeQuery(RecordSearchCriteria criteria, Set<Long> visibleListIds) {
        ReflectionUtils.trimAllStringFields(criteria);

        RecordSearchQueryData queryData = new RecordSearchQueryData();

        String viewName = "Record_search_last_View";
        if (criteria.includePreviousVersions || criteria.versionNumber != null){
            viewName = "Record_search_all_View";
        }
        queryData.query +="FROM "+viewName +" r";
        queryData.query += " WHERE 1=1 ";

        if (criteria.authenticatedLowLevel) {
            queryData.query += " AND r.confidentiality IN ('PUBLIC', 'RESTRICTED')";
        }

        LOGGER.debug("Visible list ids: {}", visibleListIds);
        queryData.query += " AND r.list_id IN (:visibleListIds) ";

        queryData.params.put("visibleListIds", visibleListIds);

        queryForMatchingRecordIds(criteria, queryData);
        queryForMatchingVersionIds(criteria, queryData);

        return queryData;
    }

    private Long reglissRefToListIdOptimizer(RecordSearchCriteria criteria) {
        if (criteria.codeType != null && StringUtils.isNotBlank(criteria.codeStr) && criteria.codeType.equals(CodeType.REGLISS_REFERENCE) &&
                criteria.codeStr.indexOf('.') != -1 && !criteria.codeStr.contains("%")) {
            String reference = criteria.codeStr.substring(0, criteria.codeStr.indexOf('.'));
            Optional<ReglissList> optList = listRepo.findByReferenceAndDeletedFalse(reference);
            if (optList.isPresent()) {
                return optList.get().getId();
            }
        }

        return null;
    }

    private void queryForMatchingVersionIds(RecordSearchCriteria criteria, RecordSearchQueryData queryData) {
        if (!criteria.listTypes.isEmpty()) {
            queryData.query += AND + TextNormalizerQueryUtil.createStringListComparisonJpql("r.LIST_TYPE", LIST_TYPES);
            queryData.params.put(LIST_TYPES, criteria.listTypes.stream().map(ReglissListType::name).collect(Collectors.toList()));
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        if (criteria.activeFrom != null) {
            queryData.query += " AND r.end_date >= TO_DATE(:activeFromDate, 'YYYY-MM-DD') ";
            queryData.params.put("activeFromDate", criteria.activeFrom.format(dateFormatter));
        }

        if (criteria.activeTo != null) {
            queryData.query += " AND r.start_date <= TO_DATE(:activeToDate, 'YYYY-MM-DD') ";
            queryData.params.put("activeToDate", criteria.activeTo.format(dateFormatter));
        }

        if (criteria.versionNumber != null) {
            queryData.query += " AND (r.FROM_VERSION_NO <= :versionNumber AND (r.TO_VERSION_NO is null or r.TO_VERSION_NO >= :versionNumber)) ";
            queryData.params.put("versionNumber", criteria.versionNumber);
        }
    }

    private void queryForMatchingRecordIds(RecordSearchCriteria criteria, RecordSearchQueryData queryData) {

        if (StringUtils.isNotBlank(criteria.name)) {
            criteria.name = StringUtils.replace(criteria.name, "'", "''");
            queryData.query += " AND ("+ TextNormalizerQueryUtil.createStringComparisonJpql("r.record_name_normalized", "name",criteria.name);
            queryData.query += " OR "+ TextNormalizerQueryUtil.createStringComparisonJpql("r.maiden_name_normalized", "name",criteria.name);
            queryData.query += " OR EXISTS (SELECT DISTINCT ra.record_id " +
                    "FROM RECORD_ALIAS ra LEFT JOIN ALIAS a on ra.alias_id = a.id " +
                    "WHERE r.id = ra.record_id AND " + TextNormalizerQueryUtil.createStringComparisonJpql("a.full_name_normalized", "name", criteria.name) + " ) ";
            queryData.query += ")";
            queryData.params.put("name", TextNormalizerQueryUtil.normalize(criteria.name));
        }

        if(StringUtils.isNotBlank(criteria.codeStr) && CodeType.SEARCH_CODE != criteria.codeType){
                queryData.params.put("code", criteria.codeStr);

                if (EnumSet.of(CodeType.IMO, CodeType.REGLISS_REFERENCE, CodeType.RECORD_ID, CodeType.EXTERNAL_REFERENCE).contains(criteria.codeType)) {
                    queryData.query += AND+ TextNormalizerQueryUtil.createStringComparisonJpql(" r."+criteria.codeType, "code", criteria.codeStr);
                }else {
                    queryData.query += " AND EXISTS (SELECT c.id FROM RECORD_"+criteria.codeType+" rc "
                            + "JOIN CODE_"+criteria.codeType+" c ON c.id = rc."+criteria.codeType+"_id "
                            +" WHERE rc.record_id = r.id "
                            + AND +TextNormalizerQueryUtil.createStringComparisonJpql(" c.code ", "code", criteria.codeStr) +" ) " ;
                }

        }

        if (!criteria.recordTypes.isEmpty()) {
            queryData.query += " AND r.record_type IN :recordTypes ";
            queryData.params.put("recordTypes", criteria.recordTypes.stream().map(RecordType::name).collect(Collectors.toList()));
        }

        if (StringUtils.isNotBlank(criteria.program)) {
            queryData.query += " AND EXISTS (SELECT p.id FROM RECORD_PROGRAM rp "
                    + " JOIN PROGRAM p ON rp.program_id = p.id "
                    + " WHERE rp.record_id = r.id "
                    + AND+ TextNormalizerQueryUtil.createStringComparisonJpql("p.label_normalized", "program", criteria.program)+" )";
            queryData.params.put("program", TextNormalizerQueryUtil.normalize(criteria.program));
        }
    }

    private Set<Long> getSearchedLists(Set<Long>visibleListIds, RecordSearchCriteria criteria){
        Set <Long> searchedLists=new HashSet<>(visibleListIds);
        Long reglissRefToListId = reglissRefToListIdOptimizer(criteria);
        if (reglissRefToListId != null) {
            criteria.listIds.add(reglissRefToListId);
        }
        if(!criteria.listIds.isEmpty()){
            //if user specified lists to search
            searchedLists.retainAll(criteria.listIds);
        }
        return searchedLists;
    }

    public static Map<String, Object> searchSortMapping() {
        Map<String, Object> map = new HashMap<>();
        map.put("recordType", "UPPER(r.record_type)");
        map.put("nameCodeIban", "UPPER(r.record_name)");
        map.put("listShortName", "UPPER(r.lst_short_name)");
        map.put("reglissReference", "UPPER(r.regliss_reference)");
        map.put("versionNumber", "r.FROM_VERSION_NO");
        map.put(PaginationService.DEFAULT_ORDER_KEY, Sort.by(Sort.Order.desc("r.id")));
        //TODO properly sort inactive
        map.put("status", "r.DELETED_IN_VERSION_ID");
        map.put("externalReference", "r.EXTERNAL_REFERENCE");
        map.put("fromVersionId", "r.FROM_VERSION_ID");
        return map;
    }
}
