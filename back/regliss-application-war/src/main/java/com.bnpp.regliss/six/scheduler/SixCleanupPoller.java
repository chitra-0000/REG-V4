package com.bnpp.regliss.six.scheduler;

import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.batch.service.NodeDetailsSupplier;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.six.exporter.service.SixCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@ReglissBatchProfile
@Slf4j
public class SixCleanupPoller {

    @Autowired
    private SixCleanupService sixCleanupService;

    @Autowired
    private HeartbeatService heartbeatService;

    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;

    private volatile LocalDate lastRunDay;

    @Scheduled(cron = "${task.six.cleanup.cron}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void cleanSix() {
        cleanSix(LocalDate.now());
    }

    void cleanSix(LocalDate today) {
        if (today.equals(lastRunDay)) {
            log.debug("SIX cleanup: already run today on this server");
            return;
        }
        String node = nodeDetailsSupplier.getNodeId();
        Optional<String> runner;
        try {
            runner = cleanupNode(heartbeatService.getAllActiveBatchNodeIds());
        } catch (RuntimeException e) {
            log.warn("SIX cleanup: batch servers could not be read ({}), tried again at the next tick", e.getMessage());
            return;
        }
        if (!runner.isPresent()) {
            log.warn("SIX cleanup: no alive batch server found (heartbeat), tried again at the next tick (last one 02:45)");
            return;
        }
        if (!runner.get().equals(node)) {
            log.info("SIX cleanup: runs on batch server {}, not on {}", runner.get(), node);
            return;
        }
        lastRunDay = today;
        try {
            sixCleanupService.runCleanup();
        } catch (RuntimeException e) {
            log.error("SIX cleanup not run: {}", e.getMessage(), e);
        }
    }

    static Optional<String> cleanupNode(List<String> aliveBatchNodeIds) {
        return aliveBatchNodeIds == null ? Optional.empty()
                : aliveBatchNodeIds.stream().filter(id -> id != null && !id.isEmpty()).min(String::compareTo);
    }
}

