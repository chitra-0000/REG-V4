package com.bnpp.regliss.scheduler;


import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.ImportedFile;
import com.bnpp.regliss.importer.manual.ImportedFileService;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ReglissBatchProfile
@Slf4j
public class ManualImportScheduler {

    @Autowired
    private ImportedFileService importedFileService;

    @Autowired
    @Qualifier("manualImportExecutor")
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private ManualImportService manualImportService;

    /**
     * Use this method if active profile is reglissBatch
     */
    @Scheduled(cron = "${task.manual.import.cron}")
    public void scheduledFileImport() {
        Optional<ImportedFile> nextReadyInTx;
        while (thereAreFreeThreads() && (nextReadyInTx = manualImportService.getNextUploadedFileForManualProcessingImportInTx(ImportFileType.MANUAL_PROCESSING)).isPresent()) {
            processUploadedFile(nextReadyInTx.get());
        }
    }

    private void processUploadedFile(ImportedFile uploadedFile) {
        log.debug("found manual uploaded file {}", uploadedFile.getId());
        importedFileService.setProcessingInTX(uploadedFile.getId());
        manualImportService.processUploadedFile(uploadedFile);
    }

    private boolean thereAreFreeThreads() { return(executor.getMaxPoolSize()-executor.getActiveCount()>0); }

}

