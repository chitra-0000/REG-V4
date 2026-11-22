package com.bnpp.regliss.repository.batch;

import com.bnpp.regliss.entity.batch.BatchJobExecution;
import com.bnpp.regliss.repository.pagination.PaginationOptions;

import java.util.List;

public interface BatchJobExecutionRepositoryCustom {

    List<BatchJobExecution> findAllWithPagination(PaginationOptions criteria);
}

