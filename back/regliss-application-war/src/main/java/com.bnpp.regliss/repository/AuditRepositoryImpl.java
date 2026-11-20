package com.bnpp.regliss.repository;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.ChildType;
import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.ObjectType;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.repository.pagination.PaginationOptions;
import com.bnpp.regliss.utils.PerformanceUtil;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

import static com.bnpp.regliss.annotation.AuditedFieldCode.*;
import static com.bnpp.regliss.repository.PaginationService.setParamsOnQuery;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class AuditRepositoryImpl implements AuditRepositoryCustom {

    @Autowired
    private PaginationService paginationService;

    @Autowired
    private EntityManager entityManager;

    private static final String SQL=
            " where a.part_version_id = :version " +
            "   and a.object_type = :objectType " +
            "   and (a.audited_field IN (:recordAuditedFields) " +
            " or " +
            "      a.audited_action in (:recordChildrenActions) or " +
            "     (a.audited_action in (:identityDocumentActions)  and " +
            "       NOT EXISTS (select 1 " +
            "                  from identity_document idd " +
            "                 inner join ref_identity_document_type iddt on idd.identity_document_type_id = iddt.id " +
            "                 where idd.id = a.child_id " +
            "                   and iddt.business_concept =:passportBusinessConcept)) " +
            "                   OR " +
            "       (a.audited_action in (:countryDetailsActions) and " +
            "        NOT EXISTS  (select 1 " +
            "                  from country_details cdet " +
            "                 inner join ref_country_type ctype on cdet.country_type_id = ctype.id " +
            "                 where cdet.id = a.child_id " +
            "                   and ctype.business_concept =:citizenshipBusinessConcept)) or " +
            "       (a.audited_action in (:personBiographyActions) and " +
            "       NOT EXISTS (select 1 " +
            "                  from person_biography pbio " +
            "                 inner join ref_date_type dtype on pbio.date_type_id = dtype.id " +
            "                 where pbio.id = a.child_id " +
            "                   and dtype.business_concept =:dateOfBirthBusinessConcept)))";

    @Override
    public List<Audit> queryAudits4MinorUpdates(Long versionId, PaginationOptions paginationOptions) {
        Map<String, Object> userSortMapping = ImmutableMap.of("recordReglissReference", "reference",
                "primaryName", "RECORD_NAME");
        PageRequest pageRequest = paginationService.toPageRequestWithAtMostOneDefaultOrder(paginationOptions,
                userSortMapping);

        String selectJpql = "SELECT a.* FROM audits a" + SQL;

        Query query = paginationService.createNativePaginatedQuery(selectJpql, getParametersForMinorChangesQuery(versionId),Audit.class, pageRequest);

        log.debug("Launching audit search query...");
        List<Audit> audits = PerformanceUtil.runAndMeasure("AuditSearchQuery ",  query::getResultList);
        log.debug("Query returned {} items", audits.size());

        return audits;
    }

    @Override
    public List<Long> getObjectIdsWithMinorUpdates(Long versionId) {
        String sql = "SELECT DISTINCT a.object_id FROM audits a" + SQL;
        Query query = entityManager.createNativeQuery(sql);
        setParamsOnQuery(getParametersForMinorChangesQuery(versionId), query);
        return query.getResultList();
    }

    @Override
    public Long countTotalAuditsForObjectIds(Long versionId, List<Long> objectIds) {
        if (objectIds == null || objectIds.isEmpty()) {
            return 0L;
        }

        String sql = "SELECT COUNT(*) FROM audits a WHERE a.object_id IN (:objectIds) AND " + SQL.substring(6);
        try {
            Query query = entityManager.createNativeQuery(sql);
            Map<String, Object> params = getParametersForMinorChangesQuery(versionId);

            log.info("Executing SQL: {}", sql);
            log.info("With parameters: {}", params);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }
            query.setParameter("objectIds", objectIds);
            BigDecimal result = (BigDecimal) query.getSingleResult();
            return result.longValue();
        } catch (Exception e) {
            log.error("Error in countTotalAuditsForObjectIds: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Audit> getPaginatedAuditsForObjectIds(Long versionId, List<Long> objectIds, PaginationOptions pagination) {
        if (objectIds == null || objectIds.isEmpty()) {
            return Collections.emptyList();
        }

        String sql = "SELECT a.* FROM audits a WHERE a.object_id IN (:objectIds) AND " + SQL.substring(6);

        try {
            Map<String, Object> params = getParametersForMinorChangesQuery(versionId);

            PageRequest pageRequest = paginationService.toPageRequestWithAtMostOneDefaultOrder(
                    pagination,
                    ImmutableMap.of("recordReglissReference", "reference", "primaryName", "RECORD_NAME")
            );

            Query query = entityManager.createNativeQuery(sql, Audit.class);

            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }
            query.setParameter("objectIds", objectIds);

            // Apply pagination
            if (pageRequest != null) {
                query.setFirstResult((int) pageRequest.getOffset());
                query.setMaxResults(pageRequest.getPageSize());
            }

            return query.getResultList();
        } catch (Exception e) {
            log.error("Error in getPaginatedAuditsForObjectIds: {}", e.getMessage(), e);
            throw e;
        }
    }


    @Override
    public Long countAudits4MinorUpdates(Long versionId) {
        return paginationService.countTotalNative("SELECT count(distinct(object_id)) FROM audits a"+ SQL, getParametersForMinorChangesQuery(versionId));
    }

    public Long countTotalAuditsForVersion(Long versionId) {
        String sql = "SELECT COUNT(*) FROM audits a " + SQL;
        Query q = entityManager.createNativeQuery(sql);
        setParamsOnQuery(getParametersForMinorChangesQuery(versionId), q);
        return ((Number) q.getSingleResult()).longValue();
    }


    private Map<String, Object> getParametersForMinorChangesQuery(Long versionId) {
        Map<String, Object> params = new HashMap<>();
        params.put("version", versionId);
        params.put("objectType", ObjectType.RECORD.toString());

        params.put("recordAuditedFields", Stream.of(RECORD_TYPE, RECORD_ADDITIONAL_INFO,
                RECORD_START_DATE, RECORD_INSTRUCTION, RECORD_KEYWORDS, PERSON_TITLE, PERSON_IS_DECEASED, VESSEL_FLAG,
                VESSEL_OWNER, VESSEL_TYPE, VESSEL_TONNAGE, VESSEL_GROSS_TONNAGE, VESSEL_CALL_SIGN, VESSEL_IMO, AIRPORT_SEARCH_CODE,
                CITY_SEARCH_CODE, COUNTRY_SEARCH_CODE, SEAPORT_SEARCH_CODE, OTHER_SEARCH_CODE, RECORD_CATEGORY,
                COUNTRY_ISO_NUM, COUNTRY_ISO_ALPHA, RECORD_DESCRIPTION, RECORD_DOW_JONES_NOTES, RECORD_ADDRESS_ZIP_CODE, RECORD_ADDRESS_ADDRESS).map(Object::toString).collect(toSet()));

        params.put("recordChildrenActions", getAuditedActionsForChildTypes(EnumSet.of(ChildType.FUNCTION, ChildType.SANCTION_TYPE,
                ChildType.GENDER, ChildType.RECORD_CATEGORY, ChildType.DESCRIPTIONS3,ChildType.OCCUPATION_CATEGORY,
                ChildType.IBAN, ChildType.SANCTION_REFERENCE, ChildType.SOURCE, ChildType.RELATION, ChildType.PROGRAM,
                ChildType.DATE_DETAILS)).stream().map(Object::toString).collect(toSet()));

        params.put("identityDocumentActions", getAuditedActionsForChildTypes(EnumSet.of(ChildType.IDENTITY)).stream().map(Object::toString).collect(toSet()));
        params.put("passportBusinessConcept", ReferentialBusinessConcept.IDT_PASSPORT.name());

        params.put("countryDetailsActions", getAuditedActionsForChildTypes(EnumSet.of(ChildType.COUNTRY_DETAILS)).stream().map(Object::toString).collect(toSet()));
        params.put("citizenshipBusinessConcept", ReferentialBusinessConcept.COUNTRY_TYPE_CITIZENSHIP.name());

        params.put("personBiographyActions", getAuditedActionsForChildTypes(EnumSet.of(ChildType.PERSON_BIOGRAPHY)).stream().map(Object::toString).collect(toSet()));
        params.put("dateOfBirthBusinessConcept", ReferentialBusinessConcept.DATE_TYPE_DATE_OF_BIRTH.name());
        return params;
    }

    private Set<AuditedAction> getAuditedActionsForChildTypes(Set<ChildType> childTypes) {
        return childTypes.stream()
                .map(AuditedAction::getAllForChildType)
                .flatMap(List::stream).collect(toSet());
    }

}
