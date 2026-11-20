package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.ImportedFile;
import com.bnpp.regliss.importer.dj.XsltApi;
import com.bnpp.regliss.importer.dj.util.StaticContextAccessor;
import com.bnpp.regliss.importer.crb.CannabisListImportService;
import com.bnpp.regliss.importer.manual.ImportedFileService;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.UserRepository;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.utils.thread.ThreadScopeContextHolder;
import com.bnpp.regliss.vo.AutomaticImportFeedBase;
import com.bnpp.regliss.vo.ImportLineErrorReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@ReglissBatchProfile
@Service
public class SemiAutomaticImporter {

    @Autowired
    private ManualImportService manualImportService;

    @Autowired
    private CannabisListImportService cannabisListImportService;

    @Autowired
    private ImportedFileService importedFileService;

    @Autowired
    protected ReglissRequestContext requestContext;

    @Autowired
    protected ReferenceService referenceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SemiAutomaticFeedAggregator feedAggregator;

    @Autowired
    private SemiAutomaticFeedImporter feedImporter;

    @Autowired
    private UnblockImportService unblockImportService;


    @CloseResourcesAfter
    public void pollInputFolder(ImportFileType importFileType) {
        try {
            if (importFileType != null) {
                if (importFileType.equals(ImportFileType.CUSTOM)) {
                    log.debug("Started Custom SemiAutomatic import");
                    manualImportService.getNextUploadedFileForManualProcessingImportInTx(importFileType)
                            .ifPresent(this:: processUploadedFile);

                } else if (importFileType.equals(ImportFileType.CRB_LIST)) {
                    log.debug("Started CRB list SemiAutomatic import : pollInputFolder(...) ");
                    manualImportService.getNextUploadedFileForManualProcessingImportInTx(importFileType)
                    .ifPresent(importedFile -> cannabisListImportService.processUploadedFile(importedFile));
                }else if (importFileType.equals(ImportFileType.DOW_JONES_CORE)) {
                    log.debug("Started Custom SemiAutomatic import");
                    manualImportService.getNextUploadedFileForManualProcessingImportInTx(importFileType)
                            .ifPresent(this:: processUploadedFile);
                }
            }
        } finally {
            // Clears DJReferentialHolder
            ThreadScopeContextHolder.clearThread();
            //reports the INSERT commands to execute on DB to make the references link correctly.
            StaticContextAccessor.getBean(XsltApi.class).printAllReferentialsWarningMessages();
        }
    }

    private void processUploadedFile(ImportedFile importedFile) {
        log.debug("found semiauto custom uploaded file {}", importedFile.getId());
        importedFileService.setProcessingInTX(importedFile.getId());
        importSemiAutoFiles(importedFile);
    }

    private void importSemiAutoFiles(ImportedFile importedFile) {
        requestContext.setCurrentUser(referenceService.getSysUser());
        List<ImportLineErrorReport> errors = new ArrayList<>();
        Optional<AutomaticImportFeedBase> importableFeed = feedAggregator.getImportableFeedTx(importedFile, errors);
        if(importableFeed.isPresent())
            feedImporter.importFeedWithoutTx(importedFile, importableFeed.get());
        else{
            unblockImportService.unblockSemiAutoImportWhenNoFeedInTx(importedFile.getId(), errors);
        }
    }

}

