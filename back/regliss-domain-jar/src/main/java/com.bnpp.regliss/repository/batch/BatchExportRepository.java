package com.bnpp.regliss.repository.batch;

import com.bnpp.regliss.entity.batch.BatchExport;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BatchExportRepository extends EntityRepository<BatchExport, Long>{

    @Query(nativeQuery = true, value = "SELECT e.*, CASE" +
            "    WHEN r.import_file_type = 'DOW_JONES_CORE' THEN 1 " +
            "    WHEN r.import_file_type = 'NOT_SUPPORTED' or r.import_file_type = 'MANUAL_PROCESSING' THEN 3 " +
            "    ELSE 2 " +
            "END  " +
            "AS format " +
            "FROM  ctr_batch_export e " +
            "LEFT OUTER JOIN version v ON v.id = e.version_id AND e.batch_node_id is NULL " +
            "LEFT OUTER JOIN regliss_list r ON r.id = v.list_id " +
            "ORDER BY format, e.request_time asc " +
            "FETCH FIRST 1 ROWS ONLY")
    BatchExport getOrderedBatchExportRecords();

    @Query(nativeQuery = true, value = "select b.* from ctr_batch_export b where b.batch_type = 'SIX_CONFIDENCE_VALUES'")
    List<BatchExport> getByBatchType();

    @Query(nativeQuery = true, value = "select count(*) from ctr_batch_export b where b.batch_type = 'SIX_CONFIDENCE_VALUES'")
    long countSixConfidenceEntries();

    @Query(nativeQuery = true, value = "select count(*) from ctr_batch_export b where b.batch_type = 'SIX_FILTERED_FILE_GENERATION' and b.batch_node_id is null")
    long countSixExportsNotStarted();

    @Query(nativeQuery = true, value = "select count(*) from ctr_batch_export b"
            + " where b.batch_type = 'SIX_FILTERED_FILE_REGENERATION'"
            + " and ',' || b.generated_file_ids || ',' like '%,' || :listRef || ',%'")
    long countSixRegenerationRequests(@Param("listRef") String listRef);
}

