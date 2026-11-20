package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.FileDelivery;

import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.search.FileDeliverySearchCriteria;
import com.bnpp.regliss.utils.ReflectionUtils;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;


public class FileDeliveryRepositoryImpl extends BaseRepositoryImpl<FileDelivery> implements FileDeliveryRepositoryCustom {
    private static final String LISTNAME = "listName";
    private static final String FLUXNAME = "fluxName";
    private static final String SUBSCRIBERNAME = "subscriberName";
    private static final String COUNTQUERY = "SELECT COUNT(*) ";
    private static final String JPQLAND = " AND ";

    @Autowired
    private PaginationService paginationService;

    /unchecked/
    @Override
    public List<FileDelivery> getFileDeliveries() {
        Map<String, Object> params = new HashMap<>();
        String jpql = "FROM FileDelivery fd"
                + " WHERE fd.status != 'SCHEDULED' "
                + " AND fd.actualRunTime >= :searchStartDate ";
        params.put("searchStartDate", LocalDateTime.now().minusDays(30));

        TypedQuery<FileDelivery> query = entityManager.createQuery(jpql, FileDelivery.class);
        for(Map.Entry<String, Object> parameterEntry : params.entrySet()){
            query.setParameter(parameterEntry.getKey(), parameterEntry.getValue());
        }

        return query.getResultList();
    }

    /unchecked/
    @Override
    public PaginatedResults<FileDelivery> search(FileDeliverySearchCriteria criteria) {
        ReflectionUtils.trimAllStringFields(criteria);
        Map<String, Object> params = new HashMap<>();
        String normalizedFluxName = TextNormalizerQueryUtil.normalize(criteria.getFluxName());
        String normalizedSubscriberName = TextNormalizerQueryUtil.normalize(criteria.getSubscriberName());
        String normalizedListName = TextNormalizerQueryUtil.normalize(criteria.getListName());
        String baseJpql = "FROM FileDelivery fd " +
                " LEFT JOIN ConcatenatedList cl ON cl.label = fd.subscription.concatenatedList.label " +
                " LEFT JOIN ReglissList l ON l.shortNameNormalized = fd.subscription.list.shortNameNormalized " +
                " WHERE fd.status != 'SCHEDULED' " +
                " AND fd.actualRunTime >= :searchStartDate ";

        params.put("searchStartDate", LocalDateTime.now().minusDays(30));

        if (StringUtils.isNotBlank(criteria.getListName())) {
            baseJpql += " AND (" +
                    TextNormalizerQueryUtil.createStringComparisonJpql("l.shortNameNormalized", LISTNAME, criteria.getListName()) +
                    " OR " +
                    TextNormalizerQueryUtil.createStringComparisonJpql("LOWER(cl.label)", LISTNAME, criteria.getListName()) +
                    ")";
            params.put(LISTNAME, normalizedListName);
        }

        if (criteria.getVersionNumber() > 0) {
            baseJpql += " AND fd.generatedFile.version.versionNumber = :versionNumber";
            params.put("versionNumber", criteria.getVersionNumber());
        }

        baseJpql = criteriaListType(criteria, baseJpql);

        if (!criteria.getStatus().isEmpty()) {
            baseJpql += " AND fd.status IN (:status)";
            params.put("status", criteria.getStatus());
        }

        if (criteria.getActualRunTime() != null) {
            if (criteria.getActualRunTime()[1] != null) {
                baseJpql += " AND function('trunc', fd.actualRunTime) >= TO_DATE(:from, 'YYYY-MM-DD') " +
                        " AND function('trunc', fd.actualRunTime) <= TO_DATE(:to, 'YYYY-MM-DD')";
                params.put("from", criteria.getActualRunTime()[0]);
                params.put("to", criteria.getActualRunTime()[1]);
            } else {
                baseJpql += " AND function('trunc', fd.actualRunTime) = TO_DATE(:actualRunTime, 'YYYY-MM-DD')";
                params.put("actualRunTime", criteria.getActualRunTime()[0]);
            }
        }

        String jpql1 = "SELECT fd " + baseJpql;
        if (StringUtils.isNotBlank(criteria.getFluxName())) {
            jpql1 += JPQLAND + TextNormalizerQueryUtil.createStringComparisonJpql(
                    "fd.subscription.subscriber.cftFluxNameNormalized", FLUXNAME, criteria.getFluxName());
            params.put(FLUXNAME, normalizedFluxName);
        }
        if (StringUtils.isNotBlank(criteria.getSubscriberName())) {
            jpql1 += JPQLAND + TextNormalizerQueryUtil.createStringComparisonJpql(
                    "fd.subscription.subscriber.nameNormalized", SUBSCRIBERNAME, criteria.getSubscriberName());
            params.put(SUBSCRIBERNAME, normalizedSubscriberName);
        }

        String jpql2 = "SELECT fd " + baseJpql;
        if (StringUtils.isNotBlank(criteria.getFluxName())) {
            jpql2 += JPQLAND + TextNormalizerQueryUtil.createStringComparisonJpql(
                    "fd.generatedFile.subscriber.cftFluxNameNormalized", FLUXNAME, criteria.getFluxName());
        }
        if (StringUtils.isNotBlank(criteria.getSubscriberName())) {
            jpql2 += JPQLAND + TextNormalizerQueryUtil.createStringComparisonJpql(
                    "fd.generatedFile.subscriber.nameNormalized", SUBSCRIBERNAME, criteria.getSubscriberName());
        }
        PageRequest pageRequest = paginationService.toPageRequestWithAtMostOneDefaultOrder(criteria.getPaginationOptions(), searchSortMapping());

        List<FileDelivery> finalResults = new ArrayList<>();
        long totalCount = 0;
        if(StringUtils.isNotBlank(criteria.getSubscriberName()) || StringUtils.isNotBlank(criteria.getFluxName()))
        {
            List<FileDelivery> list1 = paginationService.createPaginatedQuery(jpql1, params, pageRequest).getResultList();
            List<FileDelivery> list2 = paginationService.createPaginatedQuery(jpql2, params, pageRequest).getResultList();
            List<FileDelivery> concatenatedList = new ArrayList<>();
            concatenatedList.addAll(list1);
            concatenatedList.addAll(list2);
            long totalCount1 = paginationService.countTotal(COUNTQUERY + jpql1.substring(10,jpql1.length()), params);
            long totalCount2 = paginationService.countTotal(COUNTQUERY + jpql2.substring(10,jpql2.length()), params);
            finalResults = concatenatedList;
            totalCount = totalCount1 + totalCount2;
        }
        else {
            finalResults = paginationService.createPaginatedQuery(jpql1, params, pageRequest).getResultList();
            totalCount = paginationService.countTotal(COUNTQUERY + jpql1.substring(10,jpql1.length()), params);
        }
        return new PaginatedResults<>(finalResults, totalCount);
    }

    private String criteriaListType(FileDeliverySearchCriteria criteria, String baseJpql) {
        if ("Concatenated list".equals(criteria.getListType())) {
            baseJpql += " AND fd.generatedFile.version IS NULL";
        } else if ("List".equals(criteria.getListType())) {
            baseJpql += " AND fd.generatedFile.version IS NOT NULL";
        }
        return baseJpql;
    }

    public static Map<String, Object> searchSortMapping() {
        Map<String, Object> map = new HashMap<>();
        map.put("versionNumber", "fd.generatedFile.version.versionNumber");
        map.put(FLUXNAME, "UPPER(fd.subscription.subscriber.cftFluxName || fd.generatedFile.subscriber.cftFluxName)");
        map.put(LISTNAME, "UPPER(fd.subscription.list.shortName || fd.subscription.concatenatedList.label)");
        map.put("fileName", "UPPER(fd.generatedFile.fileName || fd.generatedFile.generatedFileFormat.fileExtension)");
        map.put("actualRunTime", "fd.actualRunTime");
        map.put(SUBSCRIBERNAME, "UPPER(fd.subscription.subscriber.name || fd.generatedFile.subscriber.name)");
        map.put("status", "UPPER(fd.status)");
        map.put(PaginationService.DEFAULT_ORDER_KEY, Sort.by(new Order(Direction.DESC, "fd.id")));
        return map;
    }

    public List<FileDelivery> findScheduledDeliveriesOfOldVersions() {
        Query query = entityManager.createNativeQuery(FileDeliveryRepository.SCHEDULED_DELIVERIES_OLD_VERSIONS_QUERY, FileDelivery.class);  // NOSONAR: used internally only
        return query.getResultList();
    }

}
