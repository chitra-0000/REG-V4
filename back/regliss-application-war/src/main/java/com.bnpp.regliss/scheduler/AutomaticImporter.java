package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.batch.service.BatchManagementService;
import com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.importer.dj.XsltApi;
import com.bnpp.regliss.importer.dj.util.StaticContextAccessor;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.service.EmailService;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.utils.thread.ThreadScopeContextHolder;
import com.bnpp.regliss.vo.AutomaticImportFeedBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@ReglissBatchProfile
@Service
public class AutomaticImporter {

    @Autowired
    protected EmailService emailService;

    @Autowired
    protected ReglissRequestContext requestContext;

    @Autowired
    protected ReferenceService referenceService;

    @Autowired
    private BatchManagementService batchManagementService;

    @Autowired
    private ReglissListRepository reglissListRepository;

    @Autowired
    private AutomaticFeedAggregator feedAggregator;

    @Autowired
    private AutomaticFeedImporter feedImporter;

    @CloseResourcesAfter
    public Optional<Long> pollInputFolder(ImportFileType importFormat) {
        try {
            return importAutoFiles(importFormat);
        } finally {
            // Clears DJReferentialHolder
            ThreadScopeContextHolder.clearThread();
            //reports the INSERT commands to execute on DB to make the references link correctly.
            StaticContextAccessor.getBean(XsltApi.class).printAllReferentialsWarningMessages();
        }
    }

    public Optional<Long> importAutoFiles(ImportFileType importFileType) {
        //some methods used by UI will require a 'current user' on the current thread
        requestContext.setCurrentUser(referenceService.getSysUser());
        log.debug("Looking for new auto files");

        Map<Long, AutomaticImportFeedBase> importableFeeds = feedAggregator.getImportableFeedsByListIdInTx(importFileType);
        if (importableFeeds.isEmpty()) {
            return Optional.empty();
        }
        if (batchManagementService.currentNodeIsAllowedToStartImportAuto()) {
            Optional<Long> oneListIdOpt = importableFeeds.keySet().stream()
                    .filter(listId -> batchManagementService.attemptLockList(reglissListRepository.getExactlyOne(listId)))
                    .findFirst();

            if (oneListIdOpt.isPresent()) {
                Long listId = oneListIdOpt.get();
                log.info("list locked for import auto {} ", listId);
                if (importFileType.isSixRawFormat()) {
                    feedImporter.importSixFeedWithoutTx(listId, importableFeeds.get(listId), importFileType);
                } else {
                    feedImporter.importFeedWithoutTx(listId, importableFeeds.get(listId), importFileType);
                }
                return oneListIdOpt;
            }
        }
        return Optional.empty();
    }

}

