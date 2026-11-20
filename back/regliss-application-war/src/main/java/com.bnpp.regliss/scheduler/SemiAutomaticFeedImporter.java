package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.batch.service.BatchManagementService;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.ImportedFile;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.importer.UnicodeBOMInputStream;
import com.bnpp.regliss.importer.dj.ImportService;
import com.bnpp.regliss.importer.dj.XsltApi;
import com.bnpp.regliss.importer.dj.util.StaticContextAccessor;
import com.bnpp.regliss.importer.manual.ImportedFileService;
import com.bnpp.regliss.importer.manual.ManualImportFileRepository;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.vo.AutomaticImportFeedBase;
import com.bnpp.regliss.vo.CustomAutomaticFeed;
import com.bnpp.regliss.vo.ImportLineErrorReport;
import com.bnpp.regliss.vo.ListIdAndVersionId;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.function.Supplier;

import static com.bnpp.regliss.ReglissException.ErrorCode.ERROR_CONSTRAINT_VIOLATION;
import static com.bnpp.regliss.ReglissException.ErrorCode.ERROR_CONSTRAINT_THRESHOLD_VIOLATION;
import static java.util.Collections.singletonList;


@Service
@ReglissBatchProfile
@Slf4j
public class SemiAutomaticFeedImporter extends FeedImporter{
    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private BatchManagementService batchManagementService;

    @Autowired
    private ImportedFileService importedFileService;

    @Autowired
    private UnblockImportService unblockImportService;

    @Autowired
    private AutomaticFeedService feedService;

    @Autowired
    private ManualImportFileRepository fileRepo;

    @Autowired
    private ImportService importService;

    @Autowired
    private FeedValidator feedValidator;

    private static final String EXTENSION = "zip";

    public void importFeedWithoutTx(ImportedFile importedFile, AutomaticImportFeedBase feed) {
        Long listId = importedFile.getVersion().getList().getId();
        ImportFileType importFormat = importedFile.getVersion().getList().getImportFileType();
        Long versionId = importedFile.getVersion().getId();
        Version version = importedFile.getVersion();
        ReglissList list = listRepo.findByIdFetchVersions(listId);
        Long batchExecutionId = null;

        try {
            log.info("Processing feed {}", feed);
            batchExecutionId = batchManagementService.createNewSemiAutoBatchExecutionInTx(importedFile, version);
            File tempFile = fileRepo.getFileInUploadFolderByName(importedFile.getFileName());

            if (tempFile.exists() && tempFile.isFile()) {
                feedValidator.checkUTF8InvalidFeedFiles(feed,  new ListIdAndVersionId(listId, versionId));
                if (feed.isCustomAutomatic()) {
                    insertXmlValuesIntoCAFeed((CustomAutomaticFeed) feed);
                }
                feedValidator.verifyFeed(list, feed, batchExecutionId);
                importData(feed, list, batchExecutionId,importedFile);
                feedService.removeFromListPreviousErrors(listId, feed);
                fileRepo.deleteManualImportedFileFromDisk(importedFile.getFileName());
                batchManagementService.updateBatchExecutionCompletedProgress(batchExecutionId);
            } else {
                log.error("Import file not found {}", importedFile.getFileName());
                importedFileService.setSemiAutoImportedKOInTx(importedFile.getId(), singletonList(new ImportLineErrorReport.ImportLineErrorReportBuilder("File Not Found")
                                                                                                        .withToastMessage("File Not Found")
                                                                                                        .withType(ImportLineErrorReport.ErrorType.BLOCKING)
                                                                                                       .build()));
                feedService.saveErrorAndSendEmails(listId, new ReglissException(), feed);
            }
        } catch (ReglissException e) {
            log.error("{}: Could not import feed {} for list {}", importFormat, feed, list, e);
            feedService.saveErrorAndSendEmails(listId, e, feed);
            unblockImportService.tryRevertSemiAutoVersionInTx(versionId);
            translateExceptionToErrors(e, feed);
            importedFileService.setSemiAutoImportedKOInTx(importedFile.getId(), feed.getErrors());
            fileRepo.deleteManualImportedFileFromDisk(importedFile.getFileName());
        } catch (Exception e) {
            log.error("{}: Could not import feed {} for list {}", importFormat, feed, list, e);
            feedService.saveErrorAndSendEmails(listId, e, feed);
            unblockImportService.tryRevertSemiAutoVersionInTx(versionId);
            addToErrors(e, feed);
            importedFileService.setSemiAutoImportedKOInTx(importedFile.getId(), feed.getErrors());
            fileRepo.deleteManualImportedFileFromDisk(importedFile.getFileName());
        } finally {
            StaticContextAccessor.getBean(XsltApi.class).printAllReferentialsWarningMessages();
            unblockImportService.finishImportExecutionByRequesterUser(batchExecutionId);
            unblockImportService.tryUnlockSemiAutoVersionInTx(versionId);
        }
    }


    private void importData(AutomaticImportFeedBase feed, ReglissList list, long batchExecutionId,ImportedFile importedFile) {
        Supplier<UnicodeBOMInputStream> xmlStreamProvider = openFromZipIfNecessary(feed, false);
        importService.doManualImportFeed(feed, list, xmlStreamProvider, batchExecutionId,importedFile.getId());
    }

    private void translateExceptionToErrors(ReglissException e, AutomaticImportFeedBase feed) {
        switch (e.getErrorCode()) {
            case DJ_IMPORT_XSD_VALIDATION_FAILED:
                feed.addToErrors(new ImportLineErrorReport.ImportLineErrorReportBuilder("XML structure control")
                        .withToastMessage("Wrong XML structure")
                        .withErrorDetails(e.getMessage())
                        .withType(ImportLineErrorReport.ErrorType.BLOCKING)
                        .build());
                break;
            case DJ_IMPORT_VALIDATION_ERROR:
                feed.addToErrors(new ImportLineErrorReport.ImportLineErrorReportBuilder("Unicode compatibility")
                        .withToastMessage("File is not UTF-8 compatible")
                        .withType(ImportLineErrorReport.ErrorType.BLOCKING)
                        .build());
                break;
            case IMPORT_RECORD_MISSING_EXTERNAL_REFERENCE:
                feed.addToErrors(new ImportLineErrorReport.ImportLineErrorReportBuilder("External reference is mandatory")
                        .withToastMessage("External reference is mandatory")
                        .withType(ImportLineErrorReport.ErrorType.BLOCKING)
                        .build());
                break;
            case CA_EMPTY_FILE :
                feed.addToErrors(new ImportLineErrorReport.ImportLineErrorReportBuilder("File empty control")
                        .withToastMessage("File Empty")
                        .withType(ImportLineErrorReport.ErrorType.BLOCKING)
                        .build());
                break;
            default:
                addToErrors(e, feed);
                break;

        }
    }

    private void addToErrors(Exception e, AutomaticImportFeedBase feed ){
        if( !e.getMessage().contains(ERROR_CONSTRAINT_VIOLATION.name()) && !e.getMessage().contains(ERROR_CONSTRAINT_THRESHOLD_VIOLATION.name()))
            feed.addToErrors(new ImportLineErrorReport.ImportLineErrorReportBuilder("Other error")
                    .withToastMessage("General error")
                    .withErrorDetails(e.getMessage())
                    .withType(ImportLineErrorReport.ErrorType.BLOCKING)
                    .build());
    }
}

