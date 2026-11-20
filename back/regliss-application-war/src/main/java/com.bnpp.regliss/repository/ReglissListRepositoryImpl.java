package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.search.ReglissListSearchCriteria;
import com.bnpp.regliss.search.ReglissListSearchResult;
import com.bnpp.regliss.utils.ReflectionUtils;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import org.apache.commons.lang.StringUtils;

import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class ReglissListRepositoryImpl extends BaseRepositoryImpl<ReglissList> implements ReglissListRepositoryCustom {

    @Override
    public List<ReglissListSearchResult> searchOptimized(ReglissListSearchCriteria criteria, Set<Long> visibleListIds) {
        if (visibleListIds.isEmpty()) {
            return emptyList();
        }
        ReflectionUtils.trimAllStringFields(criteria);

        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT new com.bnpp.regliss.search.ReglissListSearchResult(" +
                "lst.id," +
                "lst.active," +
                "lst.type," +
                "lst.listCategory.id," +
                "lst.scope," +
                "lst.country.id," +
                "lst.name," +
                "lst.confidentiality," +
                "lst.reference," +
                "lst.importConfiguration.allowAutoUpload," +
                "lst.importConfiguration.allowManualUpload," +
                "lst.importConfiguration.importFileType," +
                "lastV.id," +
                "lastV.versionNumber," +
                "lastV.status," +
                "lastV.certificationDate," +
                "lastV.publicationDate," +
                "lastV.startDate," +
                "CASE \n" +
                "    WHEN lst.importConfiguration.allowAutoUpload = true and lst.importConfiguration.allowManualUpload = false THEN true\n" +
                "    WHEN 'AUTO' = (SELECT NVL(i.uploadType, 'null') FROM ImportedFile i WHERE i.version = lastV and i.uploadDate = (SELECT max(uploadDate) " +
                "FROM ImportedFile imp WHERE imp.version = lastV AND NVL(imp.status, 'null') = 'IMPORTED_OK')) THEN true\n" +
                "    else false\n" +
                "end as appVerWasAutoUploaded," +

                "currentV.id," +
                "currentV.versionNumber," +
                "currentV.status," +
                "currentV.certificationDate," +
                "currentV.publicationDate," +
                "currentV.startDate," +
                "CASE \n" +
                "    WHEN lst.importConfiguration.allowAutoUpload = true and lst.importConfiguration.allowManualUpload = false THEN true\n" +
                "    WHEN 'AUTO' = (SELECT NVL(i.uploadType, 'null') FROM ImportedFile i WHERE i.version = currentV and i.uploadDate = (SELECT max(uploadDate) " +
                "FROM ImportedFile imp WHERE imp.version = currentV AND NVL(imp.status, 'null') = 'IMPORTED_OK')) THEN true\n" +
                "    else false\n" +
                "end as curVerWasAutoUploaded" +
                ") FROM ReglissList lst ";

        jpql += " LEFT JOIN ListLastCertifiedVersion lv ON lv.listId = lst.id " +
                " LEFT JOIN Version lastV ON lv.versionId = lastV.id " +

                " LEFT JOIN ListInProgressVersion lvInProgr ON lvInProgr.listId = lst.id " +
                " LEFT JOIN Version currentV ON lvInProgr.versionId = currentV.id ";

        jpql += commonPart(criteria, visibleListIds, params);

        TypedQuery<ReglissListSearchResult> query = entityManager.createQuery(jpql, ReglissListSearchResult.class);  // NOSONAR
        for(Map.Entry<String, Object> pair : params.entrySet()){
            query.setParameter(pair.getKey(), pair.getValue());
        }
        return query.getResultList().stream().distinct().collect(toList());
    }





    private String commonPart(ReglissListSearchCriteria criteria, Set<Long> visibleListIds, Map<String, Object> params) {
        String jpql = " WHERE lst.deleted = false  ";

        if (criteria.authenticatedLowLevel) {
            jpql += " AND lst.confidentiality IN ('PUBLIC', 'RESTRICTED')";
        }

        if (StringUtils.isNotBlank(criteria.name)) {
            jpql += " AND (" + TextNormalizerQueryUtil.createStringComparisonJpql("lst.nameNormalized", "name", criteria.name)
                    + " OR " + TextNormalizerQueryUtil.createStringComparisonJpql("lst.shortNameNormalized", "name", criteria.name) + ")";
            params.put("name", TextNormalizerQueryUtil.normalize(criteria.name));
        }
        if (StringUtils.isNotBlank(criteria.reference)) {
            jpql += " AND " + TextNormalizerQueryUtil.createStringComparisonJpql("lst.reference", "reference", criteria.reference);
            params.put("reference", TextNormalizerQueryUtil.normalize(criteria.reference));
        }
        if (!criteria.type.isEmpty()) {
            jpql += " AND lst.type IN (:typeIdList) ";
            params.put("typeIdList", criteria.type);
        }

        if (criteria.regulatorOrInternalPolicyId != null) {
            jpql += " AND lst.regulatorOrInternalPolicy.id = :regulatorOrInternalPolicyId ";
            params.put("regulatorOrInternalPolicyId", criteria.regulatorOrInternalPolicyId);
        }

        if (!criteria.confidentialityId.isEmpty()) {
            jpql += " AND lst.confidentiality IN (:confidentialityIdList) ";
            params.put("confidentialityIdList", criteria.confidentialityId);
        }

        if (criteria.countryId != null) {
            jpql += " AND lst.country.id = :countryId ";
            params.put("countryId", criteria.countryId);
        }

        if (!criteria.categoryIds.isEmpty()) {
            jpql += " AND lst.listCategory.id IN (:categoryIds) ";
            params.put("categoryIds", criteria.categoryIds);
        }

        if (!criteria.scopeId.isEmpty()) {
            jpql += " AND lst.scope IN (:scopeIdList) ";
            params.put("scopeIdList", criteria.scopeId);
        }

        if (criteria.publicationDateFrom != null) {
            jpql += " AND lastV.startDate >= :activeFromDate";
            params.put("activeFromDate", criteria.publicationDateFrom);

        }
        if (criteria.publicationDateTo != null) {
            jpql += " AND lastV.startDate <= :activeToDate";
            params.put("activeToDate", criteria.publicationDateTo);
        }

        jpql += " AND lst.id IN (:visibleListIds) ORDER BY lst.reference ASC";
        params.put("visibleListIds", visibleListIds);
        return jpql;
    }

}

