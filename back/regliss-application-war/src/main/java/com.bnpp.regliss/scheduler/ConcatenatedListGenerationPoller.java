package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.exporter.BatchExportService;
import com.bnpp.regliss.lock.DistributedLock;
import com.bnpp.regliss.lock.DistributedLockHandler;
import com.bnpp.regliss.lock.DistributedLocks;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.service.GenerationReason;
import com.bnpp.regliss.service.ConcatenatedListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@ReglissBatchProfile
public class ConcatenatedListGenerationPoller {

    @Autowired
    private ConcatenatedListService concatenatedListService;

    @Autowired
    private BatchExportService batchExportService;
    @Autowired
    private DistributedLocks distributedLocks;

    @Scheduled(cron = "${task.concatenated_list_generator.cron}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generateConcatenatedListFiles() {
        DistributedLock distributedLock = new DistributedLock("generateConcatenatedListFiles1");
        DistributedLockHandler.Builder distributedLockHandlerBuilder = new DistributedLockHandler.Builder()
                .lock(distributedLock)
                .onSuccess(lock -> {
                  List<Long> filesToGenerateIds = concatenatedListService.getConcatenatedListsFilesToGenerateInTx();
                    if (!filesToGenerateIds.isEmpty()) {
                        log.info("Scheduled Concatenated List generation for files: {}", filesToGenerateIds);
                        batchExportService.persistConcatenatedListExportGenerationInTx(filesToGenerateIds, GenerationReason.GENERATION);
                    }
                })
                .onFailure(l ->

                        log.info("Failed acquring distributed lock for concatenated list")
                );

        distributedLocks.executeWithDistributedLock(distributedLockHandlerBuilder.build());

    }

}

