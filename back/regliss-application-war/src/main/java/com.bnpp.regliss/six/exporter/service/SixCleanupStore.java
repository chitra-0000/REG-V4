package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.six.exporter.exception.SixExportException;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

@Component
@ReglissBatchProfile
@Slf4j
public class SixCleanupStore {

    public enum RawTable {
        INSTRUMENTS("SIX_INSTRUMENTS", SixFileKind.INSTRUMENT),
        STRUCTURED("SIX_STRUCTURED", SixFileKind.STRUCTURED),
        OPTIONS("SIX_OPTION", SixFileKind.OPTIONS);

        private final String table;
        private final SixFileKind kind;

        RawTable(String table, SixFileKind kind) {
            this.table = table;
            this.kind = kind;
        }

        public String getTable() {
            return table;
        }

        public String getPollerFileType() {
            return kind.getPollerFileType();
        }
    }

    static final String POLLER_TABLE = "SIX_FILTERED_POLLER";
    private static final String CHILD_TABLE = "FILTERED_SIX_TARGET";
    static final List<String> FILTERED_TABLES = Collections.unmodifiableList(Arrays.asList(
            CHILD_TABLE, "FILTERED_SIX_INSTRUMENTS", "FILTERED_SIX_STRUCTURED", "FILTERED_SIX_OPTION"));


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${six.cleanup.delete.chunk.size:2000}")
    private int chunkSize;

    private TransactionTemplate newTx;

    @PostConstruct
    void init() {
        newTx = new TransactionTemplate(transactionManager);
        newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public enum ClearResult { CLEARED, BUSY }

    public ClearResult clearFilteredTables(BooleanSupplier stillIdle) {
        ClearResult result = newTx.execute(status -> {
            jdbcTemplate.execute("LOCK TABLE " + POLLER_TABLE + " IN EXCLUSIVE MODE NOWAIT");
            if (!stillIdle.getAsBoolean()) {
                return ClearResult.BUSY;
            }
            for (String table : FILTERED_TABLES) {
                emptyTable(table);
            }
            return ClearResult.CLEARED;
        });
        return result == null ? ClearResult.BUSY : result;
    }

    private void emptyTable(String table) {
        String truncate = "TRUNCATE TABLE " + table + (CHILD_TABLE.equals(table) ? "" : " CASCADE");
        try {
            newTx.execute(status -> {
                jdbcTemplate.execute(truncate);
                return null;
            });
            log.info("SIX cleanup: {} truncated", table);
        } catch (DataAccessException e) {
            if (SixExportException.isConnectionError(e)) {
                throw e;
            }
            log.warn("SIX cleanup: TRUNCATE of {} refused ({}), rows deleted instead", table, SixExportException.rootCause(e));
            int rows = deleteInChunks("DELETE FROM " + table + " WHERE ID IS NOT NULL AND ROWNUM <= ?");
            log.info("SIX cleanup: {} row(s) removed from {}", rows, table);
        }
    }

    public boolean hasRowsOfVersion(RawTable raw, long versionId) {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + raw.getTable() + " WHERE VERSION_ID = ? AND ROWNUM <= ?",
                Integer.class, versionId, 1);
        return n != null && n > 0;
    }

    public int deleteOlderVersions(RawTable raw, long latestVersionId, BooleanSupplier stillIdle) {
        String sql = "DELETE FROM " + raw.getTable() + " WHERE VERSION_ID < ? AND ROWNUM <= ?";
        int chunk = chunk();
        int total = 0;
        int deleted;
        do {
            if (!stillIdle.getAsBoolean()) {
                log.info("SIX cleanup: a SIX job started, delete of {} stopped after {} row(s)", raw.getTable(), total);
                return total;
            }
            Integer n = newTx.execute(status -> jdbcTemplate.update(sql, latestVersionId, chunk));
            deleted = n == null ? 0 : n;
            total += deleted;
        } while (deleted >= chunk);
        return total;
    }

    private int deleteInChunks(String sql) {
        int chunk = chunk();
        int total = 0;
        int deleted;
        do {
            Integer n = newTx.execute(status -> jdbcTemplate.update(sql, chunk));
            deleted = n == null ? 0 : n;
            total += deleted;
        } while (deleted >= chunk);
        return total;
    }

    private int chunk() {
        return Math.max(1, chunkSize);
    }
}

