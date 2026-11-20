package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.batch.service.ImportConfigurationSupplier;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.ImportedFile;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.importer.manual.ManualImportFileRepository;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.ImportedFileRepository;
import com.bnpp.regliss.service.EmailService;
import com.bnpp.regliss.utils.FeedIdHelper;
import com.bnpp.regliss.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static java.util.Comparator.comparing;

@Slf4j
@Service
@ReglissBatchProfile
public class FeedAgregator {

    @Autowired
    private AutomaticImportFileRepository automaticFileRepo;

    @Autowired
    private ManualImportFileRepository manualFileRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private FeedValidator feedValidator;

    @Autowired
    private ImportedFileRepository importedFileRepository;

    @Autowired
    private ImportConfigurationSupplier importConfigurationSupplier;

    @Autowired
    private AutomaticSixImportFileRepository automaticSixImportFileRepository;

    public AutomaticImportFeedBase createFeed(AutomaticImportFeedIdBase feedId, String filename, String originalFilename, ImportFileType importFileType) {
        if (importFileType.isDjFormat()) {
            return new DJFeed((DJFeedId) feedId, filename, originalFilename);
        } else if (importFileType.isSixRawFormat()){
            return new SixAutomaticFeed((SixAutomaticFeedId) feedId, filename, originalFilename);
        } else {
            return new CustomAutomaticFeed((CustomAutomaticFeedId) feedId, filename, originalFilename);
        }
    }

    public void sendEmailIfFeedOlderThanPreviousVersion(ReglissList list, AutomaticImportFeedBase feedToProcess, ImportFileType importFileType) {
        if (feedToProcess.isCustomAutomatic()) {
            log.info("{}: Ordering marker <= than the ordering marker of the latest version of the list", importFileType);
            emailService.sendEmailCaWrongOrderingMarker(list);
        } else {
            log.info("{}: Feed with files {} for list {} is older than the latest version of the list", importFileType, feedToProcess.getAllFileNames(), list.getName());
            emailService.sendEmailDjImportOldDateError(list);
        }
        deleteOrMoveFileToError(feedToProcess);
    }

    public boolean feedIsReadyForImport(ReglissList list, AutomaticImportFeedBase feedA) {
        log.trace("Analyzing feed {} for list {} ", feedA, list);
        if (!validateIncrementalImport(list, feedA)) return false;
        if (!list.getPreviousErrors().isEmpty()) {
            if (feedCanOverwritePreviousFailedFile(feedA, list)) {
                removeFromPreviousErrors(list, feedA);
            } else {
                log.info("Waiting for previous failed file: {} for list: {}. No new file will be imported for this list until this failed file or a full file sooner than it will be received.",
                        getOldestErrorForList(list), list.getShortName());
                return false;
            }
        }
        return true;
    }

    private boolean validateIncrementalImport(ReglissList list, AutomaticImportFeedBase feedA) {
        /*ASAP 304,323, REGLISS should not import incremental CORE/SANCTIONS files*/

        if (!feedA.getFeedId().isFull() && (!importConfigurationSupplier.isIncrementalAllowedForDJFormat(list.getImportFileType()) ||  !list.getImportConfiguration().allowIncrementalImport())) {
            String feedFiles = String.join(", ", feedA.getAllFileNames());
            log.warn("Incremental file import for Core/Sanctions list not supported, feed files: {}, list id: {}. Feed will be moved to error",
                    feedFiles, list.getId()
            );
            deleteOrMoveFileToError(feedA);
            return false;
        }
        return true;
    }

    public AutomaticImportFeedIdBase getPreviousFeedId(Version prevVersion){
        return FeedIdHelper.feedIdFromFilename(getPreviousFileName(prevVersion), prevVersion.getImportFileType());
    }

    public boolean dateInsideFileIsOlderThan(AutomaticImportFeedIdBase previousFeedId, AutomaticImportFeedBase feed, ReglissList list) {
        return (list.getImportConfiguration().getImportFileType().isDjFormat() &&
                previousFeedId.getDateStamp().compareTo(feedValidator.dataStructureControl(feed)) > 0 );
    }

    public boolean dateInsideFileIsOlderThan(LocalDate previousDate, AutomaticImportFeedBase feed, ReglissList list) {
        return (list.getImportConfiguration().getImportFileType().isDjFormat() &&
                previousDate.compareTo(FeedIdHelper.parseStartDate(feedValidator.dataStructureControl(feed))) > 0 );
    }

    private String getPreviousFileName(Version prevVersion) {
        if(prevVersion.wasAutoUploaded()) {
            return prevVersion.getFileName();
        } else {
            return importedFileRepository.getImportedFileOKForPreviousVersions(prevVersion.getId(), prevVersion.getList().getId()).stream()
                    .max(comparing(ImportedFile::getId))
                    .map(ImportedFile::getFileName)
                    .orElseThrow(() -> new ReglissException("No ImportedFile record with status = IMPORTED_OK for any previous version"));
        }
    }

    private void removeFromPreviousErrors(ReglissList list, AutomaticImportFeedBase currentFeed) {
        if (currentFeed.isFullImport()) {
            removeCurrentFeedsFromPreviousErrors(currentFeed, list);
        } else {
            AutomaticImportFeedIdBase oldestError = getOldestErrorForList(list);
            list.removeFromPreviousErrors(oldestError);
        }
    }

    private boolean feedCanOverwritePreviousFailedFile(AutomaticImportFeedBase currentFeed, ReglissList list) {
        AutomaticImportFeedIdBase oldestError = getOldestErrorForList(list);
        oldestError.setPrefix(oldestError.getPrefix().toUpperCase());
        return currentFeed.isFullImport() ||
                currentFeed.getFeedId().equals(oldestError);
    }

    private AutomaticImportFeedIdBase getOldestErrorForList(ReglissList list) {
        return list.getPreviousErrors().first();
    }

    private void removeCurrentFeedsFromPreviousErrors(AutomaticImportFeedBase feed, ReglissList list) {
        list.getPreviousErrors()
                .stream()
                .filter(feedId -> feedId.compareTo(feed.getFeedId()) < 0)
                .forEach(list::removeFromPreviousErrors);
    }

    private void deleteOrMoveFileToError(AutomaticImportFeedBase feedToProcess) {
        if(feedToProcess.isAuto() && !feedToProcess.isSixAutomatic()) {
            automaticFileRepo.moveToErrorDirectory(feedToProcess);
        } else if (feedToProcess.isAuto() && feedToProcess.isSixAutomatic()) {
            automaticSixImportFileRepository.moveToErrorDirectory(feedToProcess);
        }
    }

}

