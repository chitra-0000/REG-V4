package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.profile.ReglissPublisherProfile;
import com.bnpp.regliss.repository.CftgtwRequestRepository;
import com.bnpp.regliss.service.FileDeliveryService;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@ReglissPublisherProfile
public class CftStatusPoller {

    @Autowired
    private CftLogService cftLogService;

    @Autowired
    private CftgtwRequestRepository cftgtwRequestRepo;

    @Autowired
    private HeartbeatService heartbeatService;

    @Autowired
    private FileDeliveryService fileDeliveryService;

    @Scheduled(fixedDelayString = "${task.cft_status.interval}")
    public void collectNewStatuses() {
        if (heartbeatService.isNodeActivePublisher()) {
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(LocalDate.now().atTime(23, 55)) || now.isBefore(LocalDate.now().atTime(0, 5))) {
                return;
            }

            collectStatusesForFileDeliveries();
        }
    }

    @Scheduled(fixedDelayString = "${task.cft_status.interval}")
    public void collectNewStatusesForGTW() {
        if (heartbeatService.isNodeActivePublisher()) {
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(LocalDate.now().atTime(23, 55)) || now.isBefore(LocalDate.now().atTime(0, 5))) {
                return;
            }

            collectStatusesForCftGTWDeliveries();
        }
    }

    public void collectStatusesForCftGTWDeliveries() {
        // Batch size 3 because there are 3 distinct types of CFTGTWRequest, which result in dozens of lists to be checked. Time consuming.
        Iterable<List<Long>> gtwChunks = Iterables.partition(cftLogService.getAllIdsByRemainingCftLogTimeIdsIsNotNull(), 3);
        for (List<Long> gtwChunk : gtwChunks) {
            cftLogService.collectStatusesForCftGTWDeliveries(gtwChunk);
        }
    }

    public void collectStatusesForFileDeliveries() {
        List<CftLogEntry> cftLogEntries = cftLogService.readAndParseCFTLogFile();
        Iterable<List<Long>> deliveryChunks = Iterables.partition(cftLogService.getAllIdsInProgress(), 100);
        for (List<Long> deliveryChunk : deliveryChunks) {
            cftLogService.collectStatusesForFileDeliveries(deliveryChunk, cftLogEntries);
        }
    }

}

