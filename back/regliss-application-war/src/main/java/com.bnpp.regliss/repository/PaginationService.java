package com.bnpp.regliss.repository;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.repository.pagination.PaginationOptions;
import com.bnpp.regliss.service.ReferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PaginationService {

    public static final String DEFAULT_ORDER_KEY = "DEFAULT_ORDER_KEY";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ReferenceService referenceService;

    public Query createPaginatedQuery(String jpql, Map<String, Object> params, PageRequest pageRequest) {
        jpql = addLeftJoinsForOrderByExpressions(jpql, pageRequest);
        Query query = entityManager.createQuery(jpql); // NOSONAR: used internally only

        setParamsOnQuery(params, query);

        if (pageRequest != null) {
            query.setFirstResult((int) pageRequest.getOffset());
            query.setMaxResults(pageRequest.getPageSize());
        }

        return query;
    }

    public Query createNativePaginatedQuery(String sql,Map<String, Object> params,  PageRequest pageRequest) {
        if (pageRequest != null) {
            sql = addLeftJoinsForOrderByExpressions(sql, pageRequest);   //// NOSONAR: used internally only
        }
        Query query = entityManager.createNativeQuery(sql);
        setParamsOnQuery(params, query);

        if (pageRequest != null) {
            query.setFirstResult((int) pageRequest.getOffset());
            query.setMaxResults(pageRequest.getPageSize());
        }

        return query;
    }
    public Query createNativePaginatedQuery(String sql,Map<String, Object> params,Class clazz , PageRequest pageRequest) {
        sql = addLeftJoinsForOrderByExpressions(sql, pageRequest);
        Query query = entityManager.createNativeQuery(sql,clazz); // NOSONAR: used internally only

        setParamsOnQuery(params, query);

        if (pageRequest != null) {
            query.setFirstResult((int) pageRequest.getOffset());
            query.setMaxResults(pageRequest.getPageSize());
        }
        return query;
    }

    public Query createPaginatedQuery(String jpql, PageRequest pageRequest) {
        return createPaginatedQuery(jpql, Collections.emptyMap(), pageRequest);
    }

    public static void setParamsOnQuery(Map<String, Object> params, Query query) {
        for(Map.Entry<String, Object> pair : params.entrySet()){
            query.setParameter(pair.getKey(), pair.getValue());
        }
    }

    private String addLeftJoinsForOrderByExpressions(String query, PageRequest pageRequest) {
        if (pageRequest == null || pageRequest.getSort() == null || !pageRequest.getSort().iterator().hasNext()) {
            return query;
        }
        Sort sort = pageRequest.getSort();

        Map<String, String> transformedOrderByPaths = new HashMap<>();
        StringBuilder resultedQuery=new StringBuilder();
        resultedQuery.append(leftJoinOrderBy(query, sort, transformedOrderByPaths));
        resultedQuery.append( " ORDER BY " );
        Iterator<Order> it = sort.iterator();
        while (it.hasNext()) {
            Order order = it.next();
            resultedQuery.append( transformedOrderByPaths.get(order.getProperty()));
            if (order.getDirection() != null) {
                resultedQuery.append( " " ).append(order.getDirection());
            }
            if (it.hasNext()) {
                resultedQuery.append( ", " );
            }
        }
        return resultedQuery.toString();
    }

    private String leftJoinOrderBy(String query, Sort querySort, Map<String, String> transformedOrderByPaths) {
        StringBuilder additionalOuterJoins = new StringBuilder();
        int aliasIndex = 0;
        Map<String, String> nullablePathAliases = new LinkedHashMap<>();
        for (Order queryOrder : querySort) {
            String originalExpression = queryOrder.getProperty();
            String nullableMakedExpression = markAsNullableAllTwoDotPaths(originalExpression);

            StringBuilder expression = new StringBuilder(nullableMakedExpression);

            while (expression.indexOf("?.") >= 0) {
                int nullablePathEndIndex = expression.indexOf("?.");
                int nullablePathStartIndex = nullablePathEndIndex;
                while (nullablePathStartIndex > 0
                        && (Character.isJavaIdentifierPart(expression.charAt(nullablePathStartIndex - 1))
                                || expression.charAt(nullablePathStartIndex - 1) == '.')) {
                    nullablePathStartIndex--;
                }
                String nullablePath = expression.substring(nullablePathStartIndex, nullablePathEndIndex);

                log.trace("Found nullable path: {}", nullablePath);

                if (!nullablePathAliases.containsKey(nullablePath)) {
                    nullablePathAliases.put(nullablePath, "order_aux_" + (aliasIndex++));
                }
                String alias = nullablePathAliases.get(nullablePath);
                expression.replace(nullablePathStartIndex, nullablePathEndIndex + "?".length(), alias);
            }

            transformedOrderByPaths.put(originalExpression, expression.toString());
        }
        for(Map.Entry<String, String> pair : nullablePathAliases.entrySet()){
            additionalOuterJoins.append(" LEFT OUTER JOIN ").append(pair.getKey()).append(" ").append(pair.getValue());
        }

        int whereIndex = query.toUpperCase().indexOf("WHERE");
        if (whereIndex < 0) {
            whereIndex = query.length();
        }
        query = query.substring(0, whereIndex) + " " + additionalOuterJoins.toString() + " " + query.substring(whereIndex);
        return query;
    }

    private String markAsNullableAllTwoDotPaths(String expression) {
        expression = expression.replaceAll("\\.([a-zA-Z_0-9$]+)\\.", ".$1?.");
        expression = expression.replaceAll("\\.([a-zA-Z_0-9$]+)\\.", ".$1?.");
        // replace TWICE in order to catch the dots caught in previous matches
        return expression;
    }

    public long countTotal(String jpql, Map<String, Object> params) {
        jpql = Pattern.compile("LEFT JOIN FETCH \\S+", Pattern.CASE_INSENSITIVE).matcher(jpql).replaceAll("");
        TypedQuery<Long> q = entityManager.createQuery(jpql, Long.class);  // NOSONAR: used internally only
        for(Map.Entry<String, Object> pair : params.entrySet()){
            q.setParameter(pair.getKey(), pair.getValue());
        }
        return q.getResultList().stream().findAny().orElse(0L);
    }

    public long countTotalNative(String sql, Map<String, Object> params) {
        Query query = entityManager.createNativeQuery(sql);  // NOSONAR: used internally only

        setParamsOnQuery(params, query);

        return ((Number) query.getSingleResult()).longValue();
    }

    public static class SortByEnum<T extends Enum<?>> {
        public final String property;
        public final Class<T> enumClass;
        public SortByEnum(String property, Class<T> enumClass) {
            this.property = property;
            this.enumClass = enumClass;
        }
    }

    public PageRequest toPageRequestWithAtMostOneDefaultOrder(PaginationOptions paginationOptions, Map<String, Object> sortMapping) {
        Sort sort = createSort(paginationOptions, sortMapping);
        return createPageRequest(paginationOptions, sort);
    }

    /*Used when there is more than one default sort*/
    public PageRequest toPageRequestWithMultipleDefaultOrders(PaginationOptions paginationOptions, Map<String, Object> userSortMapping, Sort defaultSorts) {
        Sort sort = createSort(paginationOptions, userSortMapping, defaultSorts);
        return createPageRequest(paginationOptions, sort);
    }

    private PageRequest createPageRequest(PaginationOptions paginationOptions, Sort sort) {
        PageRequest result =  PageRequest.of(paginationOptions.pageNumber - 1, paginationOptions.pageSize);
        if(sort!=null) {
            result =  PageRequest.of(paginationOptions.pageNumber - 1, paginationOptions.pageSize,sort);
        }
        log.debug("PageRequest: {}", result);
        return result;
    }


    private Sort createSort(PaginationOptions paginationOptions, Map<String, Object> sortMapping) {
        if (emptyPaginationOptions(paginationOptions)) {
            return createDefaultSort(paginationOptions, sortMapping);
        } else {
            Sort sort = createUserSort(paginationOptions, sortMapping);

            if (sortMapping.containsKey(DEFAULT_ORDER_KEY)) {
                sort = sort.and((Sort)sortMapping.get(DEFAULT_ORDER_KEY));
            }
            return sort;
        }
    }

    private Sort createSort(PaginationOptions paginationOptions, Map<String, Object> sortMapping, Sort defaultSort) {
        if (defaultSort == null ) {
            throw new IllegalArgumentException("Default sort should not be null");
        }
        if (emptyPaginationOptions(paginationOptions)) {
            return defaultSort;
        } else {
            Sort sort = createUserSort(paginationOptions, sortMapping);
            return sort.and(defaultSort);
        }
    }

    private boolean emptyPaginationOptions(PaginationOptions paginationOptions) {
        return paginationOptions == null || paginationOptions.sortCol == null || paginationOptions.sortOrder == null;
    }

    private Sort createUserSort(PaginationOptions paginationOptions, Map<String, Object> sortMapping) {
        Object orderByObject = sortMapping.get(paginationOptions.sortCol);
        if (orderByObject == null) {
            throw new IllegalArgumentException("No mapping for sort column key: " + paginationOptions.sortCol);
        }
        Direction direction = paginationOptions.sortOrder;
        return createUserSort(orderByObject, direction);
    }

    private Sort createDefaultSort(PaginationOptions paginationOptions, Map<String, Object> sortMapping) {
        if (sortMapping.containsKey(DEFAULT_ORDER_KEY)) {
            return (Sort)sortMapping.get(DEFAULT_ORDER_KEY);
        } else {
            log.debug("Sort column mapping not defined for key '{}' and no default sort column is set: no sorting happens", paginationOptions.sortCol);
            return null;
        }
    }

    private Sort createUserSort(Object orderByObject, Direction direction) {
        if (orderByObject instanceof String) {
            String localizeLabelColumn = ((String)orderByObject);
            return Sort.by(new Order(direction, localizeLabelColumn));
        } else if (orderByObject instanceof SortByEnum) {
            return Sort.by(new Order(direction, getOrderByEnumJpql((SortByEnum<?>) orderByObject)));
        } else if (orderByObject instanceof Sort) {
            return (Sort) orderByObject;
        } else {
            throw new ReglissException("Object of unsupported type in sort mapping: " + orderByObject);
        }
    }

    public <T extends Enum<?>> String getOrderByEnumJpql(SortByEnum<T> sortBy) {
        StringBuilder caseJpql = new StringBuilder();
        caseJpql.append("(CASE ");
        List<T> sortedEnumValues = new ArrayList<>(Arrays.asList(sortBy.enumClass.getEnumConstants()));
        sortedEnumValues.sort(referenceService.localizedLabelComparator(sortBy.enumClass));
        int index = 1;
        for (Enum<?> enumValue : sortedEnumValues) {
            caseJpql.append("WHEN (").append(sortBy.property).append(" = '").append(enumValue.name()).append("') THEN ").append(index).append(" ");
            index ++;
        }
        caseJpql.append(" ELSE -1 END)");
        return caseJpql.toString() ;
    }
}
