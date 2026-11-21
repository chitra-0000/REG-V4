package com.bnpp.regliss.six.importer.service;

import com.bnpp.regliss.six.exporter.service.SixDeliveryService;
import com.bnpp.regliss.six.exporter.service.SixFileKind;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Repository
public class SixConfidenceStore {

    private static final String CLAIM_UNMERGED_ROW =
            "SELECT ID FROM CTR_BATCH_EXPORT WHERE ID = ? AND BATCH_NODE_ID IS NULL FOR UPDATE SKIP LOCKED";
    private static final String MARK_ROW = "UPDATE CTR_BATCH_EXPORT SET BATCH_NODE_ID = ? WHERE ID = ?";
    private static final String TAKE_FREE_ROW =
            "UPDATE CTR_BATCH_EXPORT SET BATCH_NODE_ID = ? WHERE ID = ? AND BATCH_NODE_ID IS NULL";
    private static final String TAKE_OVER_ROW =
            "UPDATE CTR_BATCH_EXPORT SET BATCH_NODE_ID = ? WHERE ID = ? AND BATCH_NODE_ID = ?";
    private static final String DELETE_ROW = "DELETE FROM CTR_BATCH_EXPORT WHERE ID = ?";
    private static final String NODE_OF_ROW = "SELECT BATCH_NODE_ID FROM CTR_BATCH_EXPORT WHERE ID = ?";
    private static final String JOB_PARAMS = "SELECT JOB_PARAMS FROM CTR_BATCH_JOB_EXECUTION WHERE ID = ?";

    private static final Pattern DATE_TIME_IN_TEXT = Pattern.compile("_(\\d{8,})_(\\d{6,})");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public int mergeConfidenceOnce(long exportRowId, SixFileKind target, long targetVersionId,
                                   long instrumentVersionId, String nodeId) {
        Integer result = newTransaction().execute(status -> {
            List<Long> claimed = jdbcTemplate.queryForList(CLAIM_UNMERGED_ROW, Long.class, exportRowId);
            if (claimed.isEmpty()) {
                return -1;
            }
            int updated = jdbcTemplate.update(mergeSql(target), instrumentVersionId, targetVersionId);
            jdbcTemplate.update(MARK_ROW, nodeId, exportRowId);
            return updated;
        });
        return result == null ? -1 : result;
    }

    static String mergeSql(SixFileKind target) {
        if (!target.receivesConfidence()) {
            throw new IllegalArgumentException(target + " does not receive a confidence level");
        }
        return "MERGE /*+ LEADING(i) USE_HASH(t) */ INTO " + target.getTable() + " t"
                + " USING (SELECT DISTINCT CH_VALOR, CONFIDENCE_LEVEL FROM " + SixFileKind.source().getTable()
                + "        WHERE VERSION_ID = ?) i"
                + " ON (i.CH_VALOR = t." + target.getInstrumentLinkColumn() + " AND t.VERSION_ID = ?)"
                + " WHEN MATCHED THEN UPDATE SET t.CONFIDENCE_LEVEL = i.CONFIDENCE_LEVEL";
    }

    public boolean takeRow(long exportRowId, String nodeId) {
        return jdbcTemplate.update(TAKE_FREE_ROW, nodeId, exportRowId) == 1;
    }

    public boolean takeOverRow(long exportRowId, String previousNodeId, String nodeId) {
        return jdbcTemplate.update(TAKE_OVER_ROW, nodeId, exportRowId, previousNodeId) == 1;
    }

    public String currentNodeOfRow(long exportRowId) {
        List<String> nodes = jdbcTemplate.queryForList(NODE_OF_ROW, String.class, exportRowId);
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    public void deleteRow(long exportRowId) {
        jdbcTemplate.update(DELETE_ROW, exportRowId);
    }

    public Long deliveryKeyOfJob(long batchJobExecutionId) {
        List<String> params = jdbcTemplate.queryForList(JOB_PARAMS, String.class, batchJobExecutionId);
        if (params.isEmpty() || params.get(0) == null) {
            return null;
        }
        String text = params.get(0);
        try {
            JsonNode fileNames = objectMapper.readTree(text).get("filenames");
            if (fileNames != null) {
                for (JsonNode fileName : fileNames) {
                    Long key = SixDeliveryService.deliveryKey(fileName.asText());
                    if (key != null) {
                        return key;
                    }
                }
            }
        } catch (Exception notJson) {
            log.debug("JOB_PARAMS of job {} is not plain JSON, falling back to a text search", batchJobExecutionId, notJson);
        }
        Matcher m = DATE_TIME_IN_TEXT.matcher(text);
        return m.find() ? Long.parseLong(m.group(1) + m.group(2)) : null;
    }

    private TransactionTemplate newTransaction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx;
    }
}
