package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.FileDelivery.Status;
import com.bnpp.regliss.entity.Subscriber;
import com.bnpp.regliss.search.SubscriberSearchCriteria;
import com.bnpp.regliss.utils.ReflectionUtils;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import org.apache.commons.lang.StringUtils;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubscriberRepositoryImpl extends BaseRepositoryImpl<Subscriber> implements SubscriberRepositoryCustom {

    private static final String  AND  = " AND ";

    @Override
    public List<Subscriber> search(SubscriberSearchCriteria criteria) {
        ReflectionUtils.trimAllStringFields(criteria);
        String jpql = "FROM Subscriber s WHERE 1=1 ";
        Map<String, Object> params = new HashMap<>();

        if (StringUtils.isNotBlank(criteria.name)){
            jpql += AND + TextNormalizerQueryUtil.createStringComparisonJpql("s.nameNormalized", "name", criteria.name) + " ";
            params.put("name", TextNormalizerQueryUtil.normalize(criteria.name));
        }

        if(StringUtils.isNotBlank(criteria.flow)){
            jpql += AND + TextNormalizerQueryUtil.createStringComparisonJpql("s.cftFluxNameNormalized", "fluxName", criteria.flow) + " ";
            params.put("fluxName", TextNormalizerQueryUtil.normalize(criteria.flow));
        }

        if(StringUtils.isNotBlank(criteria.pole)){
            jpql += AND + TextNormalizerQueryUtil.createStringComparisonJpql("s.entityOrSectorNormalized", "pole", criteria.pole) + " ";
            params.put("pole", TextNormalizerQueryUtil.normalize(criteria.pole));
        }

        jpql += " ORDER BY  s.active DESC, s.name ASC";

        TypedQuery<Subscriber> query = entityManager.createQuery(jpql, Subscriber.class);  // NOSONAR
        for (Map.Entry<String, Object> parameterEntry : params.entrySet()) {
            query.setParameter(parameterEntry.getKey(), parameterEntry.getValue());
        }

        return query.getResultList();
    }

    @Override
    public Map<Long, Status> getAggregateStatuses(List<Long> subscriberIds) {
        Query query = entityManager.createQuery("SELECT d.subscription.subscriber.id, "
                + "MAX(CASE "
                + "WHEN d.status='IN_PROGRESS' THEN 100 "
                + "WHEN d.status='KO' THEN 50 "
                + "WHEN d.status='OK' THEN 0 "
                + "ELSE -100 "
                + "END ) "
                + "FROM FileDelivery d "
                + "WHERE d.subscription.subscriber.id IN (:subscriberIds) "
                + "AND d.status != 'SCHEDULED' "
                + "AND d.scheduledRunTime = "
                + "(SELECT MAX(d2.scheduledRunTime) FROM FileDelivery d2 "
                + "WHERE d2.subscription = d.subscription) "
                + "GROUP BY d.subscription.subscriber.id")
                .setParameter("subscriberIds", subscriberIds);

        Map<Long, Status> map = new HashMap<>();
        List<Object[]> results = query.getResultList();
        for (Object[] row : results) {
            Long subscriberId = (Long) row[0];
            int statusCode = (Integer) row[1];
            switch (statusCode) {
            case 100: map.put(subscriberId, Status.IN_PROGRESS); break;
            case 50: map.put(subscriberId, Status.KO); break;
            case 0: map.put(subscriberId, Status.OK); break;
            default : throw new IllegalArgumentException();
            }
        }
        return  map;
    }
}

