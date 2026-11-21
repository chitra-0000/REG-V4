package com.bnpp.regliss.six.scheduler;

import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.six.exporter.service.SixDeliveryService;
import com.bnpp.regliss.six.importer.service.SixConfidenceStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@ReglissBatchProfile
@Slf4j
public class SixBatchConfidencePoller {

    @Autowired
    private SixDeliveryService deliveryService;

    @Autowired
    private SixConfidenceStore store;

    @Autowired
    private BatchProgressService batchProgressService;

    @Autowired
    private HeartbeatService heartbeatService;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ReferenceService referenceService;

    private static final int MERGE_DONE_PERCENT = 5;

    @Scheduled(cron = "${task.batch.export.generation}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CloseResourcesAfter
    public void generateExportFiles() {
        requestContext.setCurrentUser(referenceService.getSysUser());
        deliveryService.findWaitingDelivery().ifPresent(this::process);
    }

    private void process(SixDeliveryService.Delivery delivery) {
        log.debug("{}", delivery);
        if (delivery.isFinishing()) {
            finishDelivery(delivery);
            return;
        }
        if (isReadyForConfidence(delivery) && mergeAll(delivery)) {
            finishWhenAllMerged(delivery.getKey());
        }
    }

    private boolean isReadyForConfidence(SixDeliveryService.Delivery delivery) {
        if (delivery.isCancelled()) {
            deliveryService.rollbackDelivery(delivery.getKey(), null, "a file of the delivery was moved out of the IN folder");
            return false;
        }
        if (delivery.hasPendingOrRunning()) {
            return false;
        }
        if (!delivery.instrument().isPresent()) {
            deliveryService.rollbackDelivery(delivery.getKey(), null, "no instrument file in the delivery");
            return false;
        }
        return true;
    }

    private boolean mergeAll(SixDeliveryService.Delivery delivery) {
        Long instrumentVersionId = delivery.instrument()
                .map(SixDeliveryService.DeliveryFile::getRow)
                .map(SixDeliveryService.WaitingRow::getVersionId)
                .orElse(null);
        if (instrumentVersionId == null) {
            deliveryService.rollbackDelivery(delivery.getKey(), null, "the instrument file has no version");
            return false;
        }
        for (SixDeliveryService.DeliveryFile file : delivery.members()) {
            if (!file.confidenceDone() && !mergeOne(delivery, file, instrumentVersionId)) {
                return false;
            }
        }
        return true;
    }

    private boolean mergeOne(SixDeliveryService.Delivery delivery, SixDeliveryService.DeliveryFile file, long instrumentVersionId) {
        try {
            deliveryService.keepWaitingFilesAlive(-1L);
            long start = System.currentTimeMillis();
            int updated = store.mergeConfidenceOnce(file.getRow().getExportId(), file.getKind(),
                    file.getRow().getVersionId(), instrumentVersionId, deliveryService.currentNodeId());
            if (updated >= 0) {
                log.info("SIX delivery {}: confidence of {} updated from the instrument file: {} rows in {}s",
                        delivery.getKey(), file.getKind(), updated, (System.currentTimeMillis() - start) / 1000);
                batchProgressService.incrementExportBatchForSix(file.getJobId(), MERGE_DONE_PERCENT);
            } else {
                log.debug("SIX delivery {}: {} merged by another server", delivery.getKey(), file.getKind());
            }
            return true;
        } catch (Exception e) {
            log.error("SIX delivery {}: confidence update of {} failed", delivery.getKey(), file.getKind(), e);
            deliveryService.rollbackDelivery(delivery.getKey(), null,
                    "confidence update of " + file.getKind() + " failed: " + e.getMessage());
            return false;
        }
    }

    private void finishWhenAllMerged(Long key) {
        SixDeliveryService.Delivery delivery = deliveryService.findWaitingDelivery()
                .filter(d -> d.getKey().equals(key))
                .orElse(null);
        if (delivery == null
                || !delivery.members().stream().allMatch(SixDeliveryService.DeliveryFile::confidenceDone)) {
            return;
        }
        Long instrumentRowId = delivery.instrument()
                .map(SixDeliveryService.DeliveryFile::getRow)
                .map(SixDeliveryService.WaitingRow::getExportId)
                .orElse(null);
        if (instrumentRowId != null && store.takeRow(instrumentRowId, deliveryService.currentNodeId())) {
            finishDelivery(deliveryService.findWaitingDelivery().orElse(delivery));
        }
    }

    private void finishDelivery(SixDeliveryService.Delivery delivery) {
        SixDeliveryService.DeliveryFile instrument = delivery.instrument().orElse(null);
        if (instrument == null || instrument.getRow() == null || !holdsFinishMark(delivery, instrument.getRow())) {
            return;
        }
        try {
            for (SixDeliveryService.DeliveryFile file : delivery.members()) {
                if (!file.getKind().isSource()) {
                    deliveryService.finishFile(delivery, file);
                }
            }
            deliveryService.finishFile(delivery, instrument);
            log.info("SIX delivery {} finished: all files at 100%, filtered exports created", delivery.getKey());
        } catch (Exception e) {
            log.error("SIX delivery {}: finishing failed, will be resumed at the next tick", delivery.getKey(), e);
        }
    }

    private boolean holdsFinishMark(SixDeliveryService.Delivery delivery, SixDeliveryService.WaitingRow instrumentRow) {
        String me = deliveryService.currentNodeId();
        String holder = instrumentRow.getBatchNodeId();
        if (holder == null || holder.equals(me)) {
            return true;
        }
        if (heartbeatService.isNodeAlive(holder) || !store.takeOverRow(instrumentRow.getExportId(), holder, me)) {
            return false;
        }
        log.warn("SIX delivery {}: server {} stopped while finishing, taken over by {}", delivery.getKey(), holder, me);
        return true;
    }
}
