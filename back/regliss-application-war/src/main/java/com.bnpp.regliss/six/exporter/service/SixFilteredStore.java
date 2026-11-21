package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.profile.ReglissBatchProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@ReglissBatchProfile
@Slf4j
public class SixFilteredStore {

    private static final Pattern SQL_NAME = Pattern.compile("^[A-Z][A-Z0-9_]{0,127}$");
    private static final int DELETE_CHUNK = 5000;
    private static final int IN_LIST_LIMIT = 1000;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate newTx;

    @PostConstruct
    void init() {
        newTx = new TransactionTemplate(transactionManager);
        newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int deleteVersionOfList(SixFileKind kind, long versionId, String listRef) {
        String sql = "DELETE FROM " + filteredTable(kind) + " WHERE VERSION_ID = ? AND SIX_LIST_REF = ? AND ROWNUM <= ?";
        return deleteInChunks(sql, versionId, listRef);
    }

    private int deleteInChunks(String sql, long versionId, String listRef) {
        int total = 0;
        int deleted;
        do {
            Integer n = newTx.execute(status -> jdbcTemplate.update(sql, versionId, listRef, DELETE_CHUNK));
            deleted = n == null ? 0 : n;
            total += deleted;
        } while (deleted == DELETE_CHUNK);
        return total;
    }

    public int removeFilteredRowsOfDroppedRow(PollerRow row) {
        Optional<SixFileKind> kind = kindOfPollerType(row.getFileType());
        if (!kind.isPresent() || row.getRawVersionId() == null || row.getListRef() == null) {
            return -1;
        }
        Integer inUse = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SIX_FILTERED_POLLER WHERE SIX_LIST_REFERENCE = ?"
                + " AND RAW_VERSION_ID = ? AND FILE_TYPE = ? AND STATUS IN " + IN_USE_STATUSES, Integer.class,
                row.getListRef(), row.getRawVersionId(), row.getFileType());
        if (inUse != null && inUse > 0) {
            return -1;
        }
        return deleteVersionOfList(kind.get(), row.getRawVersionId(), row.getListRef());
    }

    private static Optional<SixFileKind> kindOfPollerType(String pollerFileType) {
        return Arrays.stream(SixFileKind.values()).filter(k -> k.getPollerFileType().equals(pollerFileType)).findFirst();
    }

    private static String filteredTable(SixFileKind kind) {
        String table = kind.getFilteredTable();
        if (table == null || !SQL_NAME.matcher(table).matches()) {
            throw new IllegalStateException("Unsupported filtered table for " + kind + ": " + table);
        }
        return table;
    }

    public static final String PENDING = "PENDING";
    public static final String READY = "READY";
    public static final String BUILDING = "BUILDING";
    public static final String BUILT = "BUILT";
    public static final String DONE = "DONE";
    public static final String DROPPED = "DROPPED";

    private static final String COLUMNS = "SELECT ID, FILE_TYPE, RAW_LIST_ID, RAW_VERSION_ID, BATCH_JOB_EXECUTION_ID,"
            + " SIX_LIST_REFERENCE, STATUS, UPDATED_TIME, GENERATION_REASON FROM SIX_FILTERED_POLLER";
    private static final String REASON = "COALESCE(GENERATION_REASON, '" + SixExportRunGuard.GENERATION + "')";
    private static final String READY_ROWS = COLUMNS + " WHERE SIX_LIST_REFERENCE = ? AND STATUS = '" + READY + "'";
    private static final String LIST_STATUSES = inList(PENDING, READY, BUILDING, BUILT, DONE, DROPPED);
    private static final String IN_USE_STATUSES = inList(PENDING, READY, BUILDING, BUILT);

    private static String inList(String... statuses) {
        return "('" + String.join("', '", statuses) + "')";
    }

    public void announce(long pollerRowId) {
        changeStatus(pollerRowId, PENDING, READY);
    }

    public void dropPending(long pollerRowId) {
        changeStatus(pollerRowId, PENDING, DROPPED);
    }

    private void changeStatus(long pollerRowId, String from, String to) {
        int n = jdbcTemplate.update("UPDATE SIX_FILTERED_POLLER SET STATUS = ?, UPDATED_TIME = ? WHERE ID = ? AND STATUS = ?",
                to, now(), pollerRowId, from);
        if (n != 1) {
            throw new IllegalStateException("SIX_FILTERED_POLLER row " + pollerRowId + " is not " + from + " any more");
        }
    }

    public int dropPendingRowsOfJob(long batchJobExecutionId) {
        return jdbcTemplate.update("UPDATE SIX_FILTERED_POLLER SET STATUS = ?, UPDATED_TIME = ? WHERE BATCH_JOB_EXECUTION_ID = ? AND STATUS = ?",
                DROPPED, now(), batchJobExecutionId, PENDING);
    }

    public List<PollerRow> readyRows(String listRef) {
        return jdbcTemplate.query(READY_ROWS, SixFilteredStore::pollerRow, listRef);
    }

    public List<PollerRow> rowsInStatus(String... statuses) {
        return namedJdbcTemplate.query(COLUMNS + " WHERE STATUS IN (:statuses)",
                new MapSqlParameterSource("statuses", Arrays.asList(statuses)), SixFilteredStore::pollerRow);
    }

    public List<Long> jobsStillFiltering(String listRef, String reason) {
        return jdbcTemplate.queryForList("SELECT DISTINCT BATCH_JOB_EXECUTION_ID FROM SIX_FILTERED_POLLER WHERE SIX_LIST_REFERENCE = ?"
                + " AND STATUS = ? AND BATCH_JOB_EXECUTION_ID IS NOT NULL AND " + REASON + " = ?", Long.class, listRef, PENDING, reason);
    }

    public List<PollerRow> openRowsOfList(String listRef, long rawVersionId) {
        return jdbcTemplate.query(COLUMNS + " WHERE SIX_LIST_REFERENCE = ? AND RAW_VERSION_ID = ? AND STATUS IN "
                + inList(PENDING, READY, BUILDING), SixFilteredStore::pollerRow, listRef, rawVersionId);
    }

    public void dropReady(List<Long> ids) {
        updateIds(READY, DROPPED, ids);
    }

    public void markBuilt(List<Long> ids) {
        updateIds(BUILDING, BUILT, ids);
    }

    public void markBuildFailed(List<Long> ids) {
        updateIds(BUILDING, DROPPED, ids);
    }

    public void markReleased(List<Long> ids) {
        updateIds(BUILT, DONE, ids);
    }

    public void markReleaseFailed(List<Long> ids) {
        updateIds(BUILT, DROPPED, ids);
    }

    public void dropUnfinished(List<Long> ids) {
        for (String from : Arrays.asList(PENDING, READY, BUILDING)) {
            updateIds(from, DROPPED, ids);
        }
    }

    public int deleteOlderVersionRows(String fileType, long rawVersionId, LongPredicate jobWorking) {
        LocalDateTime koBefore = LocalDateTime.now().minusMinutes(SixExportRunGuard.KO_AFTER_MINUTES);
        Map<Long, Boolean> working = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        for (PollerRow row : jdbcTemplate.query(COLUMNS + " WHERE FILE_TYPE = ? AND RAW_VERSION_ID < ?", SixFilteredStore::pollerRow,
                fileType, rawVersionId)) {
            if (!isStillUsed(row, koBefore, working, jobWorking)) {
                ids.add(row.getId());
            }
        }
        int removed = 0;
        for (int start = 0; start < ids.size(); start += IN_LIST_LIMIT) {
            MapSqlParameterSource params = new MapSqlParameterSource("ids", ids.subList(start, Math.min(start + IN_LIST_LIMIT, ids.size())))
                    .addValue("built", BUILT);
            removed += namedJdbcTemplate.update("DELETE FROM SIX_FILTERED_POLLER WHERE ID IN (:ids) AND STATUS <> :built", params);
        }
        return removed;
    }

    private static boolean isStillUsed(PollerRow row, LocalDateTime koBefore, Map<Long, Boolean> working, LongPredicate jobWorking) {
        String status = row.getStatus();
        if (BUILT.equals(status)) {
            return true;
        }
        if (BUILDING.equals(status) && row.getUpdatedTime() != null && row.getUpdatedTime().isAfter(koBefore)) {
            return true;
        }
        boolean unfinished = PENDING.equals(status) || READY.equals(status) || BUILDING.equals(status);
        return unfinished && row.getBatchJobExecutionId() != null
                && working.computeIfAbsent(row.getBatchJobExecutionId(), jobWorking::test);
    }

    public void touchBuilding(List<Long> ids) {
        updateIds(BUILDING, BUILDING, ids);
    }

    private void updateIds(String from, String to, List<Long> ids) {
        for (int start = 0; start < ids.size(); start += IN_LIST_LIMIT) {
            MapSqlParameterSource params = new MapSqlParameterSource("ids", ids.subList(start, Math.min(start + IN_LIST_LIMIT, ids.size())))
                    .addValue("fromStatus", from).addValue("toStatus", to).addValue("updatedTime", now());
            namedJdbcTemplate.update("UPDATE SIX_FILTERED_POLLER SET STATUS = :toStatus, UPDATED_TIME = :updatedTime"
                    + " WHERE ID IN (:ids) AND STATUS = :fromStatus", params);
        }
    }

    public PollerClaim claimPollerRows(String listRef, Set<String> requiredTypes) {
        PollerClaim claim = newTx.execute(status -> {
            List<PollerRow> rows = jdbcTemplate.query(READY_ROWS + " FOR UPDATE SKIP LOCKED", SixFilteredStore::pollerRow, listRef);
            Map<String, List<PollerRow>> byReason = rows.stream()
                    .collect(Collectors.groupingBy(PollerRow::getGenerationReason, TreeMap::new, Collectors.toList()));
            for (List<PollerRow> reasonRows : byReason.values()) {
                Set<String> types = reasonRows.stream().map(PollerRow::getFileType).filter(Objects::nonNull).collect(Collectors.toSet());
                if (types.containsAll(requiredTypes)) {
                    updateIds(READY, BUILDING, ids(reasonRows));
                    return new PollerClaim(true, reasonRows);
                }
            }
            return new PollerClaim(false, rows);
        });
        return claim == null ? new PollerClaim(false, Collections.emptyList()) : claim;
    }

    public <T> T releaseInTx(List<Long> builtIds, Function<List<PollerRow>, T> release) {
        if (builtIds.isEmpty() || builtIds.size() > IN_LIST_LIMIT) {
            throw new IllegalArgumentException("A delivery has 1 to " + IN_LIST_LIMIT + " list rows, not " + builtIds.size());
        }
        return newTx.execute(status -> {
            List<PollerRow> locked = namedJdbcTemplate.query(COLUMNS + " WHERE ID IN (:ids) AND STATUS = '" + BUILT
                    + "' FOR UPDATE SKIP LOCKED", new MapSqlParameterSource("ids", builtIds), SixFilteredStore::pollerRow);
            if (locked.size() != builtIds.size()) {
                log.debug("SIX release: {} of {} BUILT rows locked, released by the other server", locked.size(), builtIds.size());
                return null;
            }
            return release.apply(locked);
        });
    }

    public JobLists jobLists(long batchJobExecutionId) {
        List<String> statuses = jdbcTemplate.queryForList("SELECT STATUS FROM SIX_FILTERED_POLLER WHERE BATCH_JOB_EXECUTION_ID = ?"
                + " AND STATUS IN " + LIST_STATUSES, String.class, batchJobExecutionId);
        return new JobLists(statuses);
    }

    private static Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    private static PollerRow pollerRow(ResultSet rs, int rowNum) throws SQLException {
        return new PollerRow(rs);
    }

    public static final class JobLists {
        private final int total;
        private final int done;
        private final int built;
        private final int open;
        private final int dropped;

        JobLists(List<String> statuses) {
            this.total = statuses.size();
            this.done = (int) statuses.stream().filter(DONE::equals).count();
            this.built = (int) statuses.stream().filter(BUILT::equals).count();
            this.dropped = (int) statuses.stream().filter(DROPPED::equals).count();
            this.open = total - done - built - dropped;
        }

        public int getTotal() { return total; }
        public int getDone() { return done; }
        public int getBuilt() { return built; }
        public int getOpen() { return open; }
        public int getDropped() { return dropped; }

        public boolean allDelivered() {
            return total > 0 && done == total;
        }
    }

    public static List<Long> ids(Collection<PollerRow> rows) {
        return rows.stream().map(PollerRow::getId).collect(Collectors.toList());
    }

    public static final class PollerRow {
        private final long id;
        private final String fileType;
        private final Long rawListId;
        private final Long rawVersionId;
        private final Long batchJobExecutionId;
        private final String listRef;
        private final String status;
        private final LocalDateTime updatedTime;
        private final String generationReason;

        PollerRow(ResultSet rs) throws SQLException {
            Timestamp updated = rs.getTimestamp("UPDATED_TIME");
            String reason = rs.getString("GENERATION_REASON");
            this.id = rs.getLong("ID");
            this.fileType = rs.getString("FILE_TYPE");
            this.rawListId = nullableLong(rs, "RAW_LIST_ID");
            this.rawVersionId = nullableLong(rs, "RAW_VERSION_ID");
            this.batchJobExecutionId = nullableLong(rs, "BATCH_JOB_EXECUTION_ID");
            this.listRef = rs.getString("SIX_LIST_REFERENCE");
            this.status = rs.getString("STATUS");
            this.updatedTime = updated == null ? null : updated.toLocalDateTime();
            this.generationReason = reason == null ? SixExportRunGuard.GENERATION : reason;
        }

        public long getId() { return id; }
        public String getFileType() { return fileType; }
        public Long getRawListId() { return rawListId; }
        public Long getRawVersionId() { return rawVersionId; }
        public Long getBatchJobExecutionId() { return batchJobExecutionId; }
        public String getListRef() { return listRef; }
        public String getStatus() { return status; }
        public LocalDateTime getUpdatedTime() { return updatedTime; }
        public String getGenerationReason() { return generationReason; }

        private static Long nullableLong(ResultSet rs, String column) throws SQLException {
            long value = rs.getLong(column);
            return rs.wasNull() ? null : value;
        }

        @Override
        public String toString() {
            return "list " + listRef + " " + fileType + " " + status + " " + generationReason + " (row " + id + ", raw version " + rawVersionId
                    + ", job " + batchJobExecutionId + ")";
        }
    }

    public static final class PollerClaim {
        private final boolean claimed;
        private final List<PollerRow> rows;

        PollerClaim(boolean claimed, List<PollerRow> rows) {
            this.claimed = claimed;
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        }

        public boolean isClaimed() { return claimed; }
        public List<PollerRow> getRows() { return rows; }

        public Set<Long> jobIds() {
            return jobIdsOf(rows);
        }

        @Override
        public String toString() {
            return rows.stream().map(r -> r.getFileType() + "(v" + r.getRawVersionId() + ")").collect(Collectors.joining(", "));
        }
    }

    public static Set<Long> jobIdsOf(Collection<PollerRow> rows) {
        return rows.stream().map(PollerRow::getBatchJobExecutionId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
