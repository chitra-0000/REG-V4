package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.importer.dj.XsltApi;
import com.bnpp.regliss.importer.dj.util.StaticContextAccessor;
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
public class CustomImportPoller {

    @Autowired
    private AutomaticImporter autoImporter;

    @Autowired
    private SemiAutomaticImporter semiautoImporter;

    @Scheduled(cron = "${task.dj_import.cron}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CloseResourcesAfter
    public void importCustomFiles() {
        try {
            log.debug("Started Custom SemiAutomatic import");
            semiautoImporter.pollInputFolder(ImportFileType.CUSTOM);
            log.debug("Started Custom Automatic import");
            autoImporter.pollInputFolder(ImportFileType.CUSTOM);
        } finally {
            // Clears DJReferentialHolder
            ThreadScopeContextHolder.clearThread();
            StaticContextAccessor.getBean(XsltApi.class).printAllReferentialsWarningMessages();
            log.debug("Ended Custom Automatic import import");
        }
    }
}

