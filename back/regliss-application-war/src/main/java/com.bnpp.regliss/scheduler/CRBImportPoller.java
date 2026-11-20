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
public class CRBImportPoller {

    @Autowired
    private SemiAutomaticImporter semiautoImporter;

    @Scheduled(cron = "${task.dj_import.cron}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CloseResourcesAfter
    public void importCRBListFiles() {
        try {
            log.debug("Started CRB list SemiAutomatic import");
            semiautoImporter.pollInputFolder(ImportFileType.CRB_LIST);
        } finally {
            ThreadScopeContextHolder.clearThread();
            log.debug("Ended CRB list SemiAutomatic import");
        }
    }
}

