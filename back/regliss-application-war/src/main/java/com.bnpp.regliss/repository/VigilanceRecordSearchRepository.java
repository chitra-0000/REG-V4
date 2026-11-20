package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.ListVersionDTO;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.search.VigilanceRecordSearchCriteria;
import com.bnpp.regliss.utils.PerformanceUtil;
import com.bnpp.regliss.utils.ReflectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/*This class is a repository that contains a lot of String literals mostly used in the construction of native sql.
 * In this case trying to factor into constants all the repeated literals
 * that appear in queries will just complicate them further.
 * For this reason we mark sonar rule: <String literals should not be duplicated -> squid:S1192> as false positive in this
 * class. Also extensive testing was performed on this queries through com.bnpp.regliss.repository.VigilanceRecordSearchBehaviorLocalIT
 * behavior driven tests.
 */
/squid:S1192/
@Service
public class VigilanceRecordSearchRepository {
    private static final Logger log = LoggerFactory.getLogger(VigilanceRecordSearchRepository.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RecordRepository recordRepo;

    private static final String VERSION_TABLE="select  %d as list_id, %d as version_id from dual ";

    private static final String BASE_QUERY_CODE="SELECT rc.record_id as recordId FROM RECORD_%s rc JOIN CODE_%s c ON c.id = rc.%s_id WHERE c.code = :code " +
            "  AND EXISTS ( SELECT 1 FROM record r join vers on r.list_id = vers.list_id and vers.version_id between r.FROM_VERSION AND r.TO_VERSION  WHERE r.id=rc.record_id ) ";
    private static final String BASE_QUERY_RECORD="SELECT r.id as recordId FROM RECORD r " +
            "join vers on r.list_id = vers.list_id and vers.version_id between r.FROM_VERSION AND r.TO_VERSION " +
            "WHERE r.%s = :code ";

    private static class RecordSearchQueryData {
        public String query ;
        public Map<String, Object> params = new HashMap<>();
    }

    /findsecbugs:SQL_INJECTION_JPA/
    /**
     * findsecbugs:SQL_INJECTION_JPA - The input values included in SQL queries need to be passed in safely.
     * Bind variables in prepared statements can be used to easily mitigate the risk of SQL injection
     * The parameters are set using the binding mechanism see setParamsOnQuery(queryData.params,query);
     * The sql query is complex and created via string concatenation leading to a false positive on sonar rule.
     */
    public List<Record> query(VigilanceRecordSearchCriteria criteria) {
        if(StringUtils.isEmpty(criteria.codeValue)||criteria.searchedVersions.isEmpty()){
            return Collections.emptyList();
        }
        RecordSearchQueryData queryData = constructNativeQuery(criteria);

        Query query = entityManager.createNativeQuery(queryData.query);
        log.trace(queryData.query);
        setParamsOnQuery(queryData.params,query);

        log.debug("Launching record search query...");
        List<Object> resultsObjs = PerformanceUtil.runAndMeasure("VigilanceRecordSearchQuery ",query::getResultList);
        log.debug("Query returned {} items", resultsObjs.size());

        Set<Long> recordIds = resultsObjs.stream().map(arr -> ((Number)arr).longValue()).collect(toSet());
        log.debug("Loading details of vigilance external search records...");

        return recordRepo.findAllById(recordIds);
    }

    private static void setParamsOnQuery(Map<String, Object> params, Query query) {
        params.forEach(query::setParameter);
    }

    private RecordSearchQueryData constructNativeQuery(VigilanceRecordSearchCriteria criteria) {
        ReflectionUtils.trimAllStringFields(criteria);
        return queryForMatchingCode(criteria);
    }

    private String constructQueryWithClause(List<ListVersionDTO> searchedVersions) {
        return searchedVersions.stream()
                .map(listVersion -> String.format(VERSION_TABLE,listVersion.getListId(),listVersion.getVersionId()))
                .collect(Collectors.joining(" union all "));
    }

    private RecordSearchQueryData queryForMatchingCode(VigilanceRecordSearchCriteria criteria) {
        RecordSearchQueryData queryData = new RecordSearchQueryData();

        queryData.query="WITH vers as ( "+constructQueryWithClause(criteria.searchedVersions)+" ) ";

        queryData.params.put("code", criteria.codeValue);
        if(criteria.codeType.isPartOfRecord()){
            queryData.query = queryData.query+String.format(BASE_QUERY_RECORD , criteria.codeType );
        }else {
            queryData.query =  queryData.query+String.format(BASE_QUERY_CODE ,criteria.codeType,criteria.codeType,criteria.codeType);
        }

        return queryData;
    }
}
