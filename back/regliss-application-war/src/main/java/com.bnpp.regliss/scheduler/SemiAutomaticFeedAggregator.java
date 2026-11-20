package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.service.EmailService;
import com.bnpp.regliss.utils.FeedIdHelper;
import com.bnpp.regliss.vo.AutomaticImportFeedBase;
import com.bnpp.regliss.vo.AutomaticImportFeedIdBase;
import com.bnpp.regliss.vo.ImportLineErrorReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;

@Service
@ReglissBatchProfile
@Slf4j
public class SemiAutomaticFeedAggregator {

    @Autowired
    private EmailService emailService;

    @Autowired
    private FeedAgregator feedAggregator;

    @Autowired
    private ReglissListRepository listRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AutomaticImportFeedBase> getImportableFeedTx(ImportedFile importedFile, List<ImportLineErrorReport> errors) {
        try {
            ImportFileType importFormat = importedFile.getList().getImportFileType();
            Optional<AutomaticImportFeedBase> feedOpt = getFeed(importedFile.getFileName(), importedFile.getOriginalFileName(), importFormat, errors);
            if (feedOpt.isPresent()) {
                feedOpt.get().fillIsAuto(false);
                ReglissList list = listRepo.getExactlyOne(importedFile.getList().getId());
                return getFeedToProcess(list, asList(feedOpt.get()))
                        .filter(f -> versionIsReadyForImport(list));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.info("File can not be imported '{}'", e.getMessage());
            errors.add(new ImportLineErrorReport.ImportLineErrorReportBuilder("File can not be imported")
                                     .withToastMessage("File can not be imported")
                                     .withType(ImportLineErrorReport.ErrorType.BLOCKING)
                                     .build());
            return Optional.empty();
        }
    }

    private boolean versionIsReadyForImport(ReglissList list) {
        boolean versionIsReadyForImport = list.getLastVersion().map(v -> v.getStatus() == VersionStatus.DRAFT)
                .orElse(false);
        if (!versionIsReadyForImport) {
            log.trace("Version must be in DRAFT status");
            emailService.sendEmailDjImportFileOnHoldError(list);
            return false;
        }
        return true;
    }

    private Optional<AutomaticImportFeedBase> getFeedToProcess(ReglissList list, List<AutomaticImportFeedBase> listFeeds) {
        return listFeeds.stream()
                .min(comparing(AutomaticImportFeedBase::getFeedId))
                .filter(f -> feedAggregator.feedIsReadyForImport(list, f));
    }

    private Optional<AutomaticImportFeedBase> getFeed(String fileName, String originalFileName, ImportFileType importFormat, List<ImportLineErrorReport> errors) {
        try {
            AutomaticImportFeedIdBase feedId = FeedIdHelper.feedIdFromFilename(fileName, importFormat);
            feedId.setPrefix(feedId.getPrefix().toUpperCase());
            return Optional.of(feedAggregator.createFeed(feedId, fileName, originalFileName, importFormat));
        } catch (IllegalArgumentException e) {
            log.info("{}: File extension not allowed '{}'", importFormat, fileName);
            errors.add(new ImportLineErrorReport.ImportLineErrorReportBuilder("Wrong File extension : .xml or .zip expected")
                                .withToastMessage("File extension not allowed")
                                .withType(ImportLineErrorReport.ErrorType.BLOCKING)
                                .build());
            emailService.sendEmailNomenclatureError(fileName);
        }
        return Optional.empty();
    }

}



