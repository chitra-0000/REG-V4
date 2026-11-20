package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.ReglissEnvironment;
import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.service.EmailService;
import com.bnpp.regliss.service.VersionLockService;
import com.bnpp.regliss.vo.AutomaticImportFeedBase;
import com.bnpp.regliss.vo.DJFeed;
import com.bnpp.regliss.vo.DJFileImportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@ReglissBatchProfile
public class AutomaticFeedService {

    @Autowired
    private VersionLockService versionLockService;

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private EmailService emailService;

    public boolean feedCanOverwritePreviousVersionOfTheList(ReglissList list, AutomaticImportFeedBase feed) {
        Optional<Version> lastVersion = list.getLastVersion();
        if (!lastVersion.isPresent()) {
            return false;
        }
        Version previousVersion = lastVersion.get();
        boolean isNotLocked = !versionLockService.isLockedByAnotherUser(previousVersion.getId());
        boolean previousVersionIsIntegratedAndNotLocked = previousVersion.isIntegrated() && isNotLocked;

        return previousVersionIsIntegratedAndNotLocked && feed.isFullImport();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeFromListPreviousErrors(long listId, AutomaticImportFeedBase feed) {
        ReglissList list = listRepo.getExactlyOne(listId);
        list.removeFromPreviousErrors(feed.getFeedId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveErrorAndSendEmails(Long listId, Exception e, AutomaticImportFeedBase feed) {
        ReglissList list = listRepo.getExactlyOne(listId);

        sendEmailForException(e, list, feed.getXmlDataFileName());
        list.addToPreviousErrors(feed.getXmlDataFileName());
    }

    private void sendEmailForException(Exception e, ReglissList list, String xmlDataFileName) {
        if (e instanceof ReglissException) {
            ReglissException re = (ReglissException) e;
            switch (re.getErrorCode()) {
                case DJ_IMPORT_CHECKSUM_INCORRECT:
                    emailService.sendEmailDjImportChecksumError(list);
                    break;
                case DJ_IMPORT_XSD_VALIDATION_FAILED:
                case DJ_IMPORT_VALIDATION_ERROR:
                    emailService.sendEmailDjImportFileStructureError(list);
                    break;
                case DJ_IMPORT_ZIP_CORRUPT:
                    emailService.sendEmailDjImportZipError(list, re.getParameters()[0]);
                    break;
                case DJ_IMPORT_FUTURE_DATE:
                    emailService.sendEmailDjImportFutureDateError(list);
                    break;
                case DJ_IMPORT_LOG_COUNT_INCORRECT:
                    emailService.sendEmailDjImportLogLivraisonCountError(list, re.getParameters()[0], re.getParameters()[1]);
                    break;
                case CA_APPLICATION_VERSION_TOO_OLD:
                    emailService.sendEmailDjImportOldDateError(list);
                    break;
                default:
                    emailService.sendEmailDjImportGeneralError(list, xmlDataFileName + ": " + e.getMessage());
                    break;
            }
        } else {
            emailService.sendEmailDjImportGeneralError(list, xmlDataFileName + ": " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkNotIncrementalAsFirstVersionInTx(long listId, DJFeed feed) {
        ReglissList list = listRepo.getExactlyOne(listId);
        if (ReglissEnvironment.isLocalDev()) {
            return; // So that we can import an incremental as first version
        }
        if (feed.getFeedId().getImportMode() == DJFileImportMode.INCREMENTAL &&
                list.getVersions().isEmpty()) {
            String errorMessage = "Cannot import incremental file in the first version of a list";
            throw new ReglissException(errorMessage);
        }
    }
}

