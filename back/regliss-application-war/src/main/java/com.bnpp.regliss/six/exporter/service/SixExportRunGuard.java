package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.entity.ImportedFile;
import com.bnpp.regliss.entity.SixFilteredPoller;
import com.bnpp.regliss.entity.batch.BatchJobExecution;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.ImportedFileRepository;
import com.bnpp.regliss.repository.batch.BatchJobExecutionRepository;
import com.bnpp.regliss.repository.six.SixFilteredPollerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@ReglissBatchProfile
@Slf4j
public class SixExportRunGuard {

    public static final String FAILED = "FAILED";
    public static final String FAILED_ALL = "FAILED_ALL";
    public static final String GENERATION = "GENERATION";
    public static final String REGENERATION = "REGENERATION";
    public static final long KO_AFTER_MINUTES = 30;

    @Autowired
    private SixFilteredPollerRepository sixFilteredPollerRepository;

    @Autowired
    private BatchJobExecutionRepository batchJobExecutionRepository;

    @Autowired
    private ImportedFileRepository importedFileRepository;

    @Autowired
    private HeartbeatService heartbeatService;

    private static final long FAILURE_LOOKBACK_HOURS = 24;

    private final Map<Long, String> keyByVersion = Collections.synchronizedMap(new DeliveryKeyCache());

    public String deliveryOf(long rawVersionId) {
        String cached = keyByVersion.get(rawVersionId);
        if (cached != null) {
            return cached;
        }
        ImportedFile file = importedFileRepository.findByVersionId(rawVersionId);
        Long key = file == null ? null : SixDeliveryService.deliveryKey(file.getOriginalFileName());
        if (key == null) {
            return "version:" + rawVersionId;
        }
        String delivery = String.valueOf(key);
        keyByVersion.put(rawVersionId, delivery);
        return delivery;
    }

    public void listFailed(String fileType, long rawListId, long rawVersionId, String listRef, Long batchJobExecutionId) {
        save(fileType, FAILED, rawListId, rawVersionId, listRef, batchJobExecutionId);
    }

    public void deliveryFailed(String fileType, long rawListId, long rawVersionId, String listRef, Long batchJobExecutionId) {
        save(fileType, FAILED_ALL, rawListId, rawVersionId, listRef, batchJobExecutionId);
    }

    private void save(String fileType, String status, long rawListId, long rawVersionId, String listRef, Long batchJobExecutionId) {
        sixFilteredPollerRepository.save(new SixFilteredPoller(fileType, status, LocalDateTime.now(), rawListId, rawVersionId,
                listRef, batchJobExecutionId));
        log.error("SIX delivery {}: {} recorded ({} file, list {}, raw version {}, job {})", deliveryOf(rawVersionId), status,
                fileType, listRef, rawVersionId, batchJobExecutionId);
    }

    public Optional<SixFilteredPoller> deliveryStop(long rawVersionId, Long batchJobExecutionId) {
        return find(FAILED_ALL, rawVersionId, batchJobExecutionId, null);
    }

    public Optional<SixFilteredPoller> listStop(long rawVersionId, Long batchJobExecutionId, String listRef) {
        Optional<SixFilteredPoller> list = find(FAILED, rawVersionId, batchJobExecutionId, listRef);
        return list.isPresent() ? list : deliveryStop(rawVersionId, batchJobExecutionId);
    }

    private Optional<SixFilteredPoller> find(String status, long rawVersionId, Long batchJobExecutionId, String listRef) {
        LocalDateTime jobStart = jobStart(batchJobExecutionId);
        List<SixFilteredPoller> failures = jobStart == null
                ? sixFilteredPollerRepository.findByStatus(status)
                : sixFilteredPollerRepository.findByStatusAndInsertionTimeGreaterThanEqual(status,
                jobStart.minusHours(FAILURE_LOOKBACK_HOURS));
        if (failures.isEmpty()) {
            return Optional.empty();
        }
        String delivery = deliveryOf(rawVersionId);
        LocalDateTime regenerationStart = regenerationStart(batchJobExecutionId);
        return failures.stream()
                .filter(f -> f.getRawVersionId() != null && delivery.equals(deliveryOf(f.getRawVersionId())))
                .filter(f -> listRef == null || listRef.equals(f.getSixListReference()))
                .filter(f -> regenerationStart == null || (f.getInsertionTime() != null && !f.getInsertionTime().isBefore(regenerationStart)))
                .findFirst();
    }

    public String describe(SixFilteredPoller failure) {
        return (FAILED_ALL.equals(failure.getStatus()) ? "the export of delivery " : "list " + failure.getSixListReference() + " of delivery ")
                + deliveryOf(failure.getRawVersionId()) + " failed at " + failure.getInsertionTime() + " (raw version "
                + failure.getRawVersionId() + ", job " + failure.getBatchJobExecutionId() + ")";
    }

    public String generationReasonOf(Long batchJobExecutionId) {
        if (batchJobExecutionId == null) {
            return GENERATION;
        }
        return batchJobExecutionRepository.findById(batchJobExecutionId)
                .filter(job -> job.getJobParams() != null && job.getJobParams().contains(REGENERATION))
                .map(job -> REGENERATION)
                .orElse(GENERATION);
    }

    public boolean isJobWorking(Long batchJobExecutionId) {
        if (batchJobExecutionId == null) {
            return false;
        }
        LocalDateTime since = LocalDateTime.now().minusMinutes(KO_AFTER_MINUTES);
        return batchJobExecutionRepository.findById(batchJobExecutionId)
                .filter(job -> !job.isFinished() && job.getLastUpdateDate() != null && job.getLastUpdateDate().isAfter(since))
                .filter(job -> heartbeatService.isNodeAlive(job.getNodeId()))
                .isPresent();
    }

    private LocalDateTime jobStart(Long batchJobExecutionId) {
        if (batchJobExecutionId == null) {
            return null;
        }
        return batchJobExecutionRepository.findById(batchJobExecutionId).map(BatchJobExecution::getStartDate).orElse(null);
    }

    private LocalDateTime regenerationStart(Long batchJobExecutionId) {
        if (batchJobExecutionId == null) {
            return null;
        }
        return batchJobExecutionRepository.findById(batchJobExecutionId)
                .filter(job -> job.getJobParams() != null && job.getJobParams().contains(REGENERATION))
                .map(BatchJobExecution::getStartDate)
                .orElse(null);
    }

    private static final class DeliveryKeyCache extends LinkedHashMap<Long, String> {
        private static final long serialVersionUID = 1L;
        static final int MAX_VERSIONS = 500;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
            return size() > MAX_VERSIONS;
        }
    }
}

