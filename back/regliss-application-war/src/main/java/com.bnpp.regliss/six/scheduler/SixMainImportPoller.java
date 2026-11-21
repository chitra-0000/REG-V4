package com.bnpp.regliss.six.scheduler;

import com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.importer.dj.XsltApi;
import com.bnpp.regliss.importer.dj.util.StaticContextAccessor;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.scheduler.AutomaticImporter;
import com.bnpp.regliss.utils.thread.ThreadScopeContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@ReglissBatchProfile
@Slf4j
public class SixMainImportPoller {

    @Autowired
    private AutomaticImporter importer;

    @Scheduled(cron = "${task.dj_import.cron}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CloseResourcesAfter
    public Optional<Long> importSixMainFiles() {
        try {
            log.debug("Started Six Main import");
            return importer.pollInputFolder(ImportFileType.SIX_MAIN_FILE);
        } finally {
            // Clears DJReferentialHolder
            ThreadScopeContextHolder.clearThread();
            StaticContextAccessor.getBean(XsltApi.class).printAllReferentialsWarningMessages();
            log.debug("Ended Six Main import");
        }
    }
}
