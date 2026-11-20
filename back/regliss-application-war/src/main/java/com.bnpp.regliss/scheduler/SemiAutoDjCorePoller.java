package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.utils.thread.ThreadScopeContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@ReglissBatchProfile
@Slf4j
public class SemiAutoDjCorePoller {

    @Autowired
    private SemiAutomaticImporter semiautoImporter;

    @Scheduled(cron = "${task.dj_import.cron}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CloseResourcesAfter
    public void importSemiAutoDjListFiles() {
        try {
            log.debug("Started list MLM SemiAutomatic import");
            semiautoImporter.pollInputFolder(ImportFileType.DOW_JONES_CORE);
        } finally {
            ThreadScopeContextHolder.clearThread();
            log.debug("Ended list MLM SemiAutomatic import");
        }
    }
}

