package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.batch.BatchJobExecution;
import com.bnpp.regliss.repository.batch.BatchJobExecutionRepositoryCustom;
import com.bnpp.regliss.repository.pagination.PaginationOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class BatchJobExecutionRepositoryImpl extends BaseRepositoryImpl<BatchJobExecution> implements BatchJobExecutionRepositoryCustom {

    @Autowired
    private PaginationService paginationService;

    /unchecked/
    @Override
    public List<BatchJobExecution> findAllWithPagination(PaginationOptions paginationOptions) {
        String jpql = "SELECT e FROM BatchJobExecution e";

        Map<String, Object> searchSortMapping = new HashMap<>();
        searchSortMapping.put(PaginationService.DEFAULT_ORDER_KEY, Sort.by(new Sort.Order(Sort.Direction.DESC, "e.id")));
        searchSortMapping.put("nodeId", "NODE_ID");
        searchSortMapping.put("jobType", "JOB_TYPE");
        searchSortMapping.put("hostName", "HOST_NAME");
        searchSortMapping.put("jobParams", "JOB_PARAMS");
        searchSortMapping.put("startDate", "START_DATE");
        searchSortMapping.put("percent", "PERCENT");
        searchSortMapping.put("endDate", "END_DATE");
        searchSortMapping.put("lastUpdateDate", "LAST_UPDATE_DATE");

        PageRequest pageRequest = paginationService.toPageRequestWithAtMostOneDefaultOrder(paginationOptions, searchSortMapping);

        return paginationService.createPaginatedQuery(jpql, pageRequest).getResultList();
    }
}
