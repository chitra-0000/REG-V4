package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.entity.BatchJobType;
import com.bnpp.regliss.entity.batch.BatchJobExecution;
import com.bnpp.regliss.six.exporter.exception.SixExportException;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.repository.batch.BatchExportRepository;
import com.bnpp.regliss.repository.batch.BatchJobExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

@Service
@ReglissBatchProfile
@Slf4j
public class SixCleanupService {

    static final long TMP_FILE_AGE_MINUTES = 60;
    private static final String GENERATION_SUFFIX = "_" + SixExportRunGuard.GENERATION;
    private static final String REGENERATION_SUFFIX = "_" + SixExportRunGuard.REGENERATION;
    private static final List<BatchJobType> IMPORT_JOB_TYPES = Collections.singletonList(BatchJobType.BATCH_IMPORT_SIXRAW);
    private static final List<BatchJobType> EXPORT_JOB_TYPES = Collections.singletonList(BatchJobType.BATCH_EXPORT_SIXRAW);
    private static final String STOPPED_BY_JOB = "a SIX job started during the cleanup";

    @Autowired
    private SixCleanupStore sixCleanupStore;

    @Autowired
    private SixFilteredStore sixFilteredStore;

    @Autowired
    private SixExportRunGuard sixExportRunGuard;

    @Autowired
    private SixXmlGenerationService sixXmlGenerationService;

    @Autowired
    private BatchJobExecutionRepository batchJobExecutionRepository;

    @Autowired
    private BatchExportRepository batchExportRepository;

    @Autowired
    private VersionRepository versionRepository;

    public static final class Result {
        private String exportSkippedBecause;
        private String importSkippedBecause;
        private final List<String> delivered = new ArrayList<>();
        private final List<String> discarded = new ArrayList<>();
        private final List<String> notMoved = new ArrayList<>();
        private boolean exportTablesCleared;
        private final Map<String, Integer> rawRowsDeleted = new LinkedHashMap<>();

        public String getExportSkippedBecause() { return exportSkippedBecause; }
        public String getImportSkippedBecause() { return importSkippedBecause; }
        public List<String> getDelivered() { return new ArrayList<>(delivered); }
        public List<String> getDiscarded() { return new ArrayList<>(discarded); }
        public List<String> getNotMoved() { return new ArrayList<>(notMoved); }
        public boolean isExportTablesCleared() { return exportTablesCleared; }
        public Map<String, Integer> getRawRowsDeleted() { return new LinkedHashMap<>(rawRowsDeleted); }

        @Override
        public String toString() {
            String export = exportSkippedBecause != null ? "export part skipped: " + exportSkippedBecause
                    : "held files delivered " + delivered + ", removed (DJ already has newer) " + discarded + ", not moved "
                    + notMoved + ", filtered tables cleared " + exportTablesCleared;
            String raw = importSkippedBecause != null ? "import part skipped: " + importSkippedBecause
                    : "raw rows deleted " + rawRowsDeleted;
            return export + "; " + raw;
        }
    }

    public Result runCleanup() {
        Result result = new Result();
        log.info("SIX cleanup started");
        runExportPart(result);
        runImportPart(result);
        log.info("SIX cleanup finished: {}", result);
        return result;
    }

    private void runExportPart(Result result) {
        Optional<String> busy = whyExportBusy(true);
        if (busy.isPresent()) {
            result.exportSkippedBecause = busy.get();
            log.info("SIX cleanup: export part skipped today: {}", busy.get());
            return;
        }
        try {
            recoverHeldFiles(result);
            if (!exportIdle()) {
                result.exportSkippedBecause = STOPPED_BY_JOB;
                return;
            }
            if (sixCleanupStore.clearFilteredTables(this::exportIdle) == SixCleanupStore.ClearResult.CLEARED) {
                result.exportTablesCleared = true;
            } else {
                result.exportSkippedBecause = STOPPED_BY_JOB;
            }
        } catch (RuntimeException e) {
            log.error("SIX cleanup: export part stopped (the next run continues): {}", SixExportException.rootCause(e), e);
            result.exportSkippedBecause = "error: " + SixExportException.rootCause(e);
        }
    }

    private void runImportPart(Result result) {
        Optional<String> busy = whyImportBusy(true);
        if (busy.isPresent()) {
            result.importSkippedBecause = busy.get();
            log.info("SIX cleanup: import part skipped today: {}", busy.get());
            return;
        }
        try {
            List<SixFilteredStore.PollerRow> inProgress = rowsInProgress();
            deleteOlderRawVersions(SixCleanupStore.RawTable.INSTRUMENTS, versionRepository.getLatestVersionIdOfInstruments(), inProgress, result);
            deleteOlderRawVersions(SixCleanupStore.RawTable.STRUCTURED, versionRepository.getLatestVersionIdOfStructure(), inProgress, result);
            deleteOlderRawVersions(SixCleanupStore.RawTable.OPTIONS, versionRepository.getLatestVersionIdOfOptions(), inProgress, result);
        } catch (RuntimeException e) {
            log.error("SIX cleanup: import part stopped (the next run continues): {}", SixExportException.rootCause(e), e);
            result.importSkippedBecause = "error: " + SixExportException.rootCause(e);
        }
    }

    private boolean exportIdle() {
        return !whyExportBusy(false).isPresent();
    }

    private boolean importIdle() {
        return !whyImportBusy(false).isPresent();
    }

    Optional<String> whyExportBusy(boolean logStoppedJobs) {
        Optional<String> job = workingJob(EXPORT_JOB_TYPES, logStoppedJobs);
        if (job.isPresent()) {
            return job;
        }
        if (batchExportRepository.countSixExportsNotStarted() > 0) {
            return Optional.of("a SIX export is requested and not yet started");
        }
        return listInProgress();
    }

    Optional<String> whyImportBusy(boolean logStoppedJobs) {
        Optional<String> job = workingJob(IMPORT_JOB_TYPES, logStoppedJobs);
        if (job.isPresent()) {
            return job;
        }
        if (batchExportRepository.countSixConfidenceEntries() > 0) {
            return Optional.of("a SIX delivery waits for its confidence step (SIX_CONFIDENCE_VALUES)");
        }
        return Optional.empty();
    }

    private Optional<String> workingJob(List<BatchJobType> types, boolean logStoppedJobs) {
        for (BatchJobExecution job : batchJobExecutionRepository.findByJobTypeInAndEndDateIsNull(types)) {
            if (sixExportRunGuard.isJobWorking(job.getId())) {
                return Optional.of("SIX job " + job.getId() + " is running");
            }
            if (logStoppedJobs) {
                log.warn("SIX cleanup: job {} is not finished but does not work any more (server {} stopped or job stuck): not waited for",
                        job.getId(), job.getNodeId());
            }
        }
        return Optional.empty();
    }

    private Optional<String> listInProgress() {
        return rowsInProgress().stream().findFirst().map(row -> "SIX export list in progress: " + row);
    }

    private static Long lowestRawVersionInUse(SixCleanupStore.RawTable raw, List<SixFilteredStore.PollerRow> inProgress) {
        return inProgress.stream().filter(row -> raw.getPollerFileType().equals(row.getFileType()))
                .map(SixFilteredStore.PollerRow::getRawVersionId).filter(Objects::nonNull).min(Long::compare).orElse(null);
    }

    private List<SixFilteredStore.PollerRow> rowsInProgress() {
        LocalDateTime koBefore = LocalDateTime.now().minusMinutes(SixExportRunGuard.KO_AFTER_MINUTES);
        Map<Long, Boolean> working = new HashMap<>();
        List<SixFilteredStore.PollerRow> inProgress = new ArrayList<>();
        for (SixFilteredStore.PollerRow row : sixFilteredStore.rowsInStatus(SixFilteredStore.PENDING, SixFilteredStore.READY,
                SixFilteredStore.BUILDING, SixFilteredStore.BUILT)) {
            boolean alive;
            if (SixFilteredStore.BUILT.equals(row.getStatus())) {
                alive = true;
            } else if (SixFilteredStore.BUILDING.equals(row.getStatus())) {
                alive = row.getUpdatedTime() != null && row.getUpdatedTime().isAfter(koBefore);
            } else {
                alive = row.getBatchJobExecutionId() != null
                        && working.computeIfAbsent(row.getBatchJobExecutionId(), sixExportRunGuard::isJobWorking);
            }
            if (alive) {
                inProgress.add(row);
            }
        }
        return inProgress;
    }

    private void recoverHeldFiles(Result result) {
        List<SixXmlGenerationService.HeldFile> held;
        try {
            held = sixXmlGenerationService.heldFiles();
        } catch (IOException e) {
            throw new IllegalStateException("holding folder not readable: " + e.getMessage(), e);
        }
        if (!held.isEmpty()) {
            Map<String, Stamp> lastDelivered = lastDeliveredByListFolder();
            Map<String, List<SixXmlGenerationService.HeldFile>> byList = new LinkedHashMap<>();
            for (SixXmlGenerationService.HeldFile file : held) {
                byList.computeIfAbsent(file.getListFolder(), k -> new ArrayList<>()).add(file);
            }
            byList.forEach((list, files) -> recoverList(list, files, lastDelivered.get(list), result));
        }
        try {
            int tmp = sixXmlGenerationService.removeTempFilesAndEmptyFolders(TMP_FILE_AGE_MINUTES);
            if (tmp > 0) {
                log.info("SIX cleanup: {} unfinished '.tmp' file(s) removed from the holding folder", tmp);
            }
        } catch (IOException e) {
            log.warn("SIX cleanup: holding folder not fully tidied: {}", e.getMessage());
        }
    }

    private void recoverList(String list, List<SixXmlGenerationService.HeldFile> files, Stamp delivered, Result result) {
        files.sort(Comparator.comparing(SixCleanupService::stampOf));
        SixXmlGenerationService.HeldFile newest = files.get(files.size() - 1);
        for (SixXmlGenerationService.HeldFile file : files) {
            boolean deliver = file == newest && (delivered == null || stampOf(file).compareTo(delivered) > 0);
            try {
                if (deliver) {
                    sixXmlGenerationService.deliverHeldFile(file);
                    result.delivered.add(file.toString());
                    log.warn("SIX cleanup: left-over CONVERTER file {} of list {} moved to DJ IN", file, list);
                } else {
                    sixXmlGenerationService.discardHeldFile(file);
                    result.discarded.add(file.toString());
                    log.warn("SIX cleanup: left-over CONVERTER file {} removed: DJ already received {} for list {}", file,
                            file == newest ? delivered : "a newer file", list);
                }
            } catch (IOException | RuntimeException e) {
                result.notMoved.add(file.toString());
                log.error("SIX cleanup: left-over CONVERTER file {} could not be {}: {}", file, deliver ? "moved to DJ IN" : "removed",
                        SixExportException.rootCause(e), e);
            }
        }
    }

    private Map<String, Stamp> lastDeliveredByListFolder() {
        Map<String, Stamp> last = new HashMap<>();
        for (SixFilteredStore.PollerRow row : sixFilteredStore.rowsInStatus(SixFilteredStore.DONE)) {
            if (row.getListRef() == null || row.getRawVersionId() == null) {
                continue;
            }
            Stamp stamp = new Stamp(parseKey(sixExportRunGuard.deliveryOf(row.getRawVersionId())), row.getUpdatedTime());
            last.merge(SixXmlGenerationService.holdingFolderName(row.getListRef()), stamp, (a, b) -> a.compareTo(b) >= 0 ? a : b);
        }
        return last;
    }

    private static Stamp stampOf(SixXmlGenerationService.HeldFile file) {
        return new Stamp(deliveryKeyOfGroup(file.getGroup()), file.getLastModified());
    }

    static Long deliveryKeyOfGroup(String group) {
        String key = group;
        if (key.endsWith(REGENERATION_SUFFIX)) {
            key = key.substring(0, key.length() - REGENERATION_SUFFIX.length());
        } else if (key.endsWith(GENERATION_SUFFIX)) {
            key = key.substring(0, key.length() - GENERATION_SUFFIX.length());
        }
        return parseKey(key);
    }

    private static Long parseKey(String key) {
        if (key == null || key.isEmpty() || key.length() > 18 || !key.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Long.valueOf(key);
    }

    static final class Stamp implements Comparable<Stamp> {
        private static final Comparator<Stamp> ORDER = Comparator
                .comparing((Stamp s) -> s.delivery, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(s -> s.time, Comparator.nullsFirst(Comparator.naturalOrder()));
        private final Long delivery;
        private final LocalDateTime time;

        Stamp(Long delivery, LocalDateTime time) {
            this.delivery = delivery;
            this.time = time;
        }

        @Override
        public int compareTo(Stamp other) {
            return ORDER.compare(this, other);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Stamp && compareTo((Stamp) o) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(delivery, time);
        }

        @Override
        public String toString() {
            return "delivery " + delivery + " at " + time;
        }
    }

    private void deleteOlderRawVersions(SixCleanupStore.RawTable raw, Long latest,
                                        List<SixFilteredStore.PollerRow> inProgress, Result result) {
        if (latest == null) {
            log.info("SIX cleanup: no {} version, nothing to delete", raw.getTable());
            return;
        }
        if (!sixCleanupStore.hasRowsOfVersion(raw, latest)) {
            log.warn("SIX cleanup: latest version {} has no row in {} (failed import not fully rolled back?): older versions kept",
                    latest, raw.getTable());
            return;
        }
        long keepFrom = latest;
        Long inUse = lowestRawVersionInUse(raw, inProgress);
        if (inUse != null && inUse < latest) {
            keepFrom = inUse;
            log.info("SIX cleanup: {}: versions from {} kept, still used by a SIX export", raw.getTable(), inUse);
        }
        int rows = sixCleanupStore.deleteOlderVersions(raw, keepFrom, this::importIdle);
        result.rawRowsDeleted.put(raw.getTable(), rows);
        log.info("SIX cleanup: {} row(s) of versions older than {} deleted from {} (and their SIX_TARGET rows)", rows, keepFrom, raw.getTable());
    }
}

