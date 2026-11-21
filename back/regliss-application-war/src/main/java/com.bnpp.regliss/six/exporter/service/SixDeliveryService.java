package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.batch.service.BatchManagementService;
import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.NodeDetailsSupplier;
import com.bnpp.regliss.entity.BatchJobType;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.batch.BatchExport;
import com.bnpp.regliss.entity.batch.BatchExportType;
import com.bnpp.regliss.entity.batch.BatchJobExecution;
import com.bnpp.regliss.exporter.BatchExportService;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.repository.batch.BatchExportRepository;
import com.bnpp.regliss.scheduler.AutomaticFeedAggregator;
import com.bnpp.regliss.scheduler.AutomaticSixImportFileRepository;
import com.bnpp.regliss.scheduler.UnblockImportService;
import com.bnpp.regliss.service.EmailService;
import com.bnpp.regliss.service.GenerationReason;
import com.bnpp.regliss.six.importer.service.SixConfidenceStore;
import com.bnpp.regliss.vo.AutomaticImportFeedBase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

@Slf4j
@Service
@ReglissBatchProfile
public class SixDeliveryService {

    private static final long KEEP_ALIVE_EVERY_MS = 60_000L;

    @Value("${six.file.prefix}")
    private String sixFileNamePrefix;

    @Autowired
    private AutomaticSixImportFileRepository sixFileRepo;
    @Autowired
    private BatchExportRepository batchExportRepository;
    @Autowired
    private BatchExportService batchExportService;
    @Autowired
    private BatchManagementService batchManagementService;
    @Autowired
    private BatchProgressService batchProgressService;
    @Autowired
    private UnblockImportService unblockImportService;
    @Autowired
    private ReglissListRepository reglissListRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;
    @Autowired
    private SixConfidenceStore store;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private final AtomicLong lastKeepAlive = new AtomicLong();

    public static Long deliveryKey(String fileName) {
        if (fileName == null) {
            return null;
        }
        Matcher m = AutomaticFeedAggregator.SIX_RAW_PATTERN.matcher(FilenameUtils.getBaseName(fileName).toUpperCase());
        if (!m.matches()) {
            return null;
        }
        try {
            return Long.parseLong(m.group(2) + m.group(3));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean afterSuccessfulImport(AutomaticImportFeedBase feed) {
        String fileName = feed.getXmlDataFileName();
        if (sixFileRepo.getInputFileByName(fileName).exists()) {
            log.info("SIX {} imported, waiting for the other files of its delivery", fileName);
            return true;
        }
        log.warn("SIX {} imported, but its delivery was cancelled meanwhile (another file failed) -> rolling it back", fileName);
        rollbackDelivery(deliveryKey(fileName), null, "another file of the same delivery failed");
        return false;
    }

    public void afterFailedImport(AutomaticImportFeedBase feed, Long listId, Long failedJobId, Exception error) {
        String fileName = feed.getXmlDataFileName();
        rollbackDelivery(deliveryKey(fileName), failedJobId, "file " + fileName + " failed: " + error.getMessage());
        if (failedJobId == null && listId != null) {
            batchManagementService.attemptUnlockListWithCurrentNode(listId);
        }
    }

    public void rollbackDelivery(Long key, Long failedJobId, String reason) {
        log.error("SIX delivery {} is rolled back: {}", key, reason);
        if (key != null) {
            moveDeliveryFilesToError(key);
            rollbackWaitingFiles(key, reason);
        }
        if (failedJobId != null) {
            closeJob(failedJobId, User.SYS_USERNAME);
        }
    }

    private void moveDeliveryFilesToError(Long key) {
        for (String fileName : filesInInputFolder(key).values()) {
            try {
                if (sixFileRepo.moveToErrorDirectoryIfPresent(fileName)) {
                    log.info("SIX {} moved to the error folder", fileName);
                }
            } catch (RuntimeException e) {
                log.error("SIX delivery {}: could not move {} to the error folder", key, fileName, e);
            }
        }
    }

    private void rollbackWaitingFiles(Long key, String reason) {
        for (WaitingRow row : waitingRows()) {
            if (!key.equals(row.key)) {
                continue;
            }
            try {
                rollbackWaitingFile(row, key, reason);
            } catch (RuntimeException e) {
                log.error("SIX delivery {}: rollback of {} failed, will be retried", key, row, e);
            }
        }
    }

    private void rollbackWaitingFile(WaitingRow row, Long key, String reason) {
        log.warn("SIX delivery {}: rolling back {} (version {})", key, row.kind, row.versionId);
        store.deleteRow(row.exportId);
        if (row.versionId != null) {
            unblockImportService.tryDeleteSixAutomaticVersionAndRecordsInTx(row.versionId);
        }
        closeJob(row.jobId, row.launcher);
        try {
            BatchJobExecution job = entityManager.find(BatchJobExecution.class, row.jobId);
            if (job != null && job.getReglissListId() != null) {
                ReglissList list = reglissListRepository.getExactlyOne(job.getReglissListId());
                emailService.sendEmailDjImportGeneralError(list, "SIX delivery " + key + " rolled back ("
                        + reason + "). This file was imported successfully but is removed because the delivery is all or nothing; "
                        + "all files of the delivery were moved to the error folder.");
            }
        } catch (Exception e) {
            log.error("Could not send the rollback mail for job {}", row.jobId, e);
        }
    }

    public Optional<Delivery> findWaitingDelivery() {
        List<WaitingRow> rows = waitingRows();
        Optional<Long> finishingKey = finishingDeliveryKey(rows);
        Optional<Long> latestKey = finishingKey.isPresent() ? finishingKey : newestDeliveryKey(rows);
        if (!latestKey.isPresent()) {
            if (!rows.isEmpty()) {
                log.warn("SIX_CONFIDENCE_VALUES rows found whose job has no SIX file name: {}", rows);
            }
            return Optional.empty();
        }
        if (!finishingKey.isPresent()) {
            rows = supersedeLeftovers(rows, latestKey.get());
        }
        Delivery delivery = new Delivery(latestKey.get());
        addWaitingRows(delivery, rows);
        addOpenImportJobs(delivery);
        filesInInputFolder(delivery.key).forEach((kind, fileName) -> delivery.file(kind).fileInInput = fileName);
        return Optional.of(delivery);
    }

    private static Optional<Long> finishingDeliveryKey(List<WaitingRow> rows) {
        return rows.stream()
                .filter(r -> r.key != null && r.kind != null && r.kind.isSource() && r.batchNodeId != null)
                .map(r -> r.key).max(Long::compare);
    }

    private static Optional<Long> newestDeliveryKey(List<WaitingRow> rows) {
        return rows.stream().map(r -> r.key).filter(Objects::nonNull).max(Long::compare);
    }

    private static void addWaitingRows(Delivery delivery, List<WaitingRow> rows) {
        for (WaitingRow row : rows) {
            if (delivery.key.equals(row.key) && row.kind != null) {
                delivery.file(row.kind).row = row;
                delivery.file(row.kind).jobId = row.jobId;
            }
        }
    }

    private void addOpenImportJobs(Delivery delivery) {
        for (BatchJobExecution job : openSixImportJobs()) {
            if (delivery.key.equals(store.deliveryKeyOfJob(job.getId()))) {
                kindOfList(job.getReglissListId()).ifPresent(kind -> {
                    delivery.file(kind).jobOpen = true;
                    if (delivery.file(kind).jobId == null) {
                        delivery.file(kind).jobId = job.getId();
                    }
                });
            }
        }
    }

    private List<WaitingRow> supersedeLeftovers(List<WaitingRow> rows, Long latestKey) {
        Map<SixFileKind, WaitingRow> newestByKind = newestRowByKind(rows, latestKey);
        List<WaitingRow> kept = new ArrayList<>();
        Map<Long, List<WaitingRow>> olderDeliveries = new TreeMap<>();
        for (WaitingRow row : rows) {
            if (isKept(row, latestKey, newestByKind)) {
                kept.add(row);
            } else if (latestKey.equals(row.key)) {
                supersedeRows(Collections.singletonList(row), latestKey);
            } else {
                olderDeliveries.computeIfAbsent(row.key, k -> new ArrayList<>()).add(row);
            }
        }
        olderDeliveries.forEach((olderKey, olderRows) -> supersedeOlderDelivery(olderKey, olderRows, latestKey));
        return kept;
    }

    private static Map<SixFileKind, WaitingRow> newestRowByKind(List<WaitingRow> rows, Long latestKey) {
        Map<SixFileKind, WaitingRow> newestByKind = new EnumMap<>(SixFileKind.class);
        for (WaitingRow row : rows) {
            if (latestKey.equals(row.key) && row.kind != null) {
                newestByKind.merge(row.kind, row, (a, b) -> versionOf(a) >= versionOf(b) ? a : b);
            }
        }
        return newestByKind;
    }

    private static boolean isKept(WaitingRow row, Long latestKey, Map<SixFileKind, WaitingRow> newestByKind) {
        if (row.key == null) {
            return true;
        }
        return latestKey.equals(row.key) && (row.kind == null || newestByKind.get(row.kind) == row);
    }

    private void supersedeOlderDelivery(Long olderKey, List<WaitingRow> olderRows, Long latestKey) {
        try {
            for (WaitingRow row : olderRows) {
                store.deleteRow(row.exportId);
            }
            Set<SixFileKind> importing = new HashSet<>();
            for (BatchJobExecution job : openSixImportJobs()) {
                if (olderKey.equals(store.deliveryKeyOfJob(job.getId())) && olderRows.stream().noneMatch(r -> r.jobId == job.getId())) {
                    kindOfList(job.getReglissListId()).ifPresent(importing::add);
                }
            }
            filesInInputFolder(olderKey).forEach((kind, fileName) -> {
                if (!importing.contains(kind) && sixFileRepo.moveToIgnoreDirectoryIfPresent(fileName)) {
                    log.info("SIX {} of the superseded delivery {} moved to the ignore folder", fileName, olderKey);
                }
            });
        } catch (RuntimeException e) {
            log.error("SIX delivery {}: could not supersede it completely, retried at the next tick", olderKey, e);
            return;
        }
        supersedeRows(olderRows, latestKey);
    }

    private void supersedeRows(List<WaitingRow> superseded, Long latestKey) {
        for (WaitingRow row : superseded) {
            try {
                log.warn("SIX delivery {}: {} (version {}, job {}) superseded by delivery {} - not merged, not exported",
                        row.key, row.kind, row.versionId, row.jobId, latestKey);
                store.deleteRow(row.exportId);
                closeJob(row.jobId, row.launcher);
            } catch (RuntimeException e) {
                log.error("SIX: could not supersede {}, retried at the next tick", row, e);
            }
        }
    }

    private static long versionOf(WaitingRow row) {
        return row.versionId == null ? Long.MIN_VALUE : row.versionId;
    }

    public void finishFile(Delivery delivery, DeliveryFile file) {
        if (file.fileInInput != null) {
            sixFileRepo.deleteInputFile(file.fileInInput);
        }
        if (file.jobOpen) {
            batchManagementService.updateBatchExecutionCompletedProgress(file.jobId);
        }
        if (file.kind.isSource()) {
            closeJob(file.jobId, launcherOf(file));
            createFilteredExportAndDeleteRow(file);
        } else {
            createFilteredExportAndDeleteRow(file);
            closeJob(file.jobId, launcherOf(file));
        }
        log.info("SIX delivery {}: {} finished (100%)", delivery.key, file.kind);
    }

    private void createFilteredExportAndDeleteRow(DeliveryFile file) {
        if (file.row == null) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.execute(status -> {
            batchExportService.persistSixFilteredFileGenerationStubInTx(BatchExportType.SIX_FILTERED_FILE_GENERATION,
                    GenerationReason.GENERATION, file.row.version);
            store.deleteRow(file.row.exportId);
            return null;
        });
    }

    public void keepWaitingFilesAlive(long callerJobId) {
        long now = System.currentTimeMillis();
        long last = lastKeepAlive.get();
        if (now - last < KEEP_ALIVE_EVERY_MS || !lastKeepAlive.compareAndSet(last, now)) {
            return;
        }
        try {
            for (WaitingRow row : waitingRows()) {
                if (row.jobId != callerJobId) {
                    batchProgressService.incrementExportBatchForSix(row.jobId, 0);
                }
            }
        } catch (Exception e) {
            log.warn("SIX keep-alive of the waiting files failed (ignored): {}", e.getMessage());
        }
    }

    public String currentNodeId() {
        return nodeDetailsSupplier.getNodeId();
    }

    private void closeJob(Long jobId, String launcherUserName) {
        if (jobId == null) {
            return;
        }
        try {
            unblockImportService.finishSixImportExecutionByRequesterUser(jobId, launcherUserName);
        } catch (Exception e) {
            log.error("Could not close SIX job {} / unlock its list", jobId, e);
        }
    }

    private static String launcherOf(DeliveryFile file) {
        return file.row != null && file.row.launcher != null ? file.row.launcher : User.SYS_USERNAME;
    }

    private List<WaitingRow> waitingRows() {
        List<WaitingRow> result = new ArrayList<>();
        for (BatchExport export : batchExportRepository.getByBatchType()) {
            List<Long> ids = export.getExportParams().getGeneratedFileIds();
            if (ids == null || ids.isEmpty()) {
                log.warn("SIX_CONFIDENCE_VALUES row {} has no job id", export.getId());
                continue;
            }
            WaitingRow row = new WaitingRow();
            row.exportId = export.getId();
            row.jobId = ids.get(0);
            row.batchNodeId = store.currentNodeOfRow(export.getId());
            row.launcher = export.getExportParams().getLauncherUsername();
            row.version = export.getExportParams().getVersionOpt().orElse(null);
            row.versionId = row.version == null ? null : row.version.getId();
            row.kind = row.version == null ? null : SixFileKind.of(row.version.getImportFileType()).orElse(null);
            row.key = store.deliveryKeyOfJob(row.jobId);
            result.add(row);
        }
        return result;
    }

    private List<BatchJobExecution> openSixImportJobs() {
        return entityManager.createQuery(
                        "SELECT e FROM BatchJobExecution e WHERE e.jobType = :type AND e.endDate IS NULL", BatchJobExecution.class)
                .setParameter("type", BatchJobType.BATCH_IMPORT_SIXRAW)
                .getResultList();
    }

    private Optional<SixFileKind> kindOfList(Long listId) {
        if (listId == null) {
            return Optional.empty();
        }
        return SixFileKind.of(reglissListRepository.getExactlyOne(listId).getImportFileType());
    }

    private Map<SixFileKind, String> filesInInputFolder(Long key) {
        Map<SixFileKind, String> files = new EnumMap<>(SixFileKind.class);
        File[] inInput = sixFileRepo.getInputDirectory().listFiles(File::isFile);
        if (inInput == null) {
            throw new IllegalStateException("Could not list the SIX IN folder " + sixFileRepo.getInputDirectory());
        }
        for (File file : inInput) {
            String fileName = file.getName();
            if (fileName.startsWith(sixFileNamePrefix) && key.equals(deliveryKey(fileName))) {
                SixFileKind.ofFileName(fileName).ifPresent(kind -> files.putIfAbsent(kind, fileName));
            }
        }
        return files;
    }

    public static final class WaitingRow {
        long exportId;
        long jobId;
        Long key;
        SixFileKind kind;
        Version version;
        Long versionId;
        String batchNodeId;
        String launcher;

        public Long getVersionId() { return versionId; }
        public long getExportId() { return exportId; }
        public String getBatchNodeId() { return batchNodeId; }

        @Override
        public String toString() {
            return "{export " + exportId + ", job " + jobId + ", " + kind + ", key " + key + "}";
        }
    }

    public enum FileState { PENDING, RUNNING, WAITING, ABSENT }

    public static final class DeliveryFile {
        final SixFileKind kind;
        WaitingRow row;
        Long jobId;
        boolean jobOpen;
        String fileInInput;

        DeliveryFile(SixFileKind kind) { this.kind = kind; }

        public SixFileKind getKind() { return kind; }
        public WaitingRow getRow() { return row; }
        public Long getJobId() { return jobId; }

        public FileState state() {
            if (row != null) return FileState.WAITING;
            if (jobOpen) return FileState.RUNNING;
            if (fileInInput != null) return FileState.PENDING;
            return FileState.ABSENT;
        }

        public boolean confidenceDone() {
            return !kind.receivesConfidence() || (row != null && row.batchNodeId != null);
        }

        @Override
        public String toString() { return kind + "=" + state(); }
    }

    public static final class Delivery {
        final Long key;
        private final Map<SixFileKind, DeliveryFile> files = new EnumMap<>(SixFileKind.class);

        Delivery(Long key) { this.key = key; }

        public Long getKey() { return key; }

        DeliveryFile file(SixFileKind kind) {
            return files.computeIfAbsent(kind, DeliveryFile::new);
        }

        public List<DeliveryFile> members() {
            List<DeliveryFile> result = new ArrayList<>();
            for (DeliveryFile f : files.values()) {
                if (f.state() != FileState.ABSENT) result.add(f);
            }
            return Collections.unmodifiableList(result);
        }

        public Optional<DeliveryFile> instrument() {
            return Optional.ofNullable(files.get(SixFileKind.source())).filter(f -> f.state() != FileState.ABSENT);
        }

        public boolean isFinishing() {
            return instrument().map(f -> f.row != null && f.row.batchNodeId != null).orElse(false);
        }

        public boolean hasPendingOrRunning() {
            return members().stream().anyMatch(f -> f.state() == FileState.PENDING || f.state() == FileState.RUNNING);
        }

        public boolean isCancelled() {
            return members().stream().anyMatch(f -> f.state() == FileState.WAITING && f.fileInInput == null);
        }

        @Override
        public String toString() { return "SIX delivery " + key + " " + members(); }
    }
}

