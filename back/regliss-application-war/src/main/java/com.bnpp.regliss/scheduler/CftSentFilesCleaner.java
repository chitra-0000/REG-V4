package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.entity.FileDelivery;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Subscriber;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.exporter.GeneratedFileNameService;
import com.bnpp.regliss.profile.ReglissPublisherProfile;
import com.bnpp.regliss.repository.FileDeliveryRepository;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.repository.SubscriberRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@ReglissPublisherProfile
@Slf4j
public class CftSentFilesCleaner {


    @Value("${cft.push.folder.base}")
    File pushFolderBase;

    @Value("${cft.sent_file_lifetime.minutes}")
    long cftSentFileLifetime;

    @Autowired
    private FileRepository fileRepo;

    @Autowired
    private FileDeliveryRepository fileDeliveryRepo;

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private SubscriberRepository subscriberRepo;

    @Autowired
    GeneratedFileNameService filenameService;

    @Autowired
    private HeartbeatService heartbeatService;

    @Scheduled(fixedDelayString = "${task.cft_sent_files_cleaner.interval}")
    @Transactional
    public void cleanSentCftFiles() {
        if (heartbeatService.isNodeActivePublisher()) {
            log.debug("Started to clean CFT sent files.");
            List<File> filesToDelete = getFilesToDelete();
            fileRepo.deleteFiles(filesToDelete);
        }
    }

    List<File> getFilesToDelete() {
        List<File> filesToDelete = new ArrayList<>();
        List<File> cftDirectories = fileRepo.getDirectoriesInFolder(pushFolderBase);
        for (File cftDirectory : cftDirectories) {
            Optional<ReglissList> listOpt = listRepo.findByReferenceAndDeletedFalse(cftDirectory.getName());

            if (listOpt.isPresent()) {
                List<File> versionDirectories = fileRepo.getDirectoriesInFolder(cftDirectory);
                for (File versionFolder :versionDirectories) {
                    if (NumberUtils.isParsable(versionFolder.getName())) {
                        Long versionNr = Long.valueOf(versionFolder.getName());
                        filesToDelete.addAll(getCftFilesToDeleteForList(fileRepo.getUnlockedFilesFromInFolderAndSubfolders(versionFolder), listOpt.get(), versionNr));
                    }
                }
                filesToDelete.addAll(fileRepo.getUnlockedFilesInFolder(cftDirectory));
            } else if (FileDeliveryPoller.OFAC_CFT_FOLDER_NAME.equals(cftDirectory.getName())) {
                filesToDelete.addAll(getCftFilesToDeleteForOfac(fileRepo.getUnlockedFilesFromInFolderAndSubfolders(cftDirectory)));
            } else if (FileDeliveryPoller.DELIVERY_REPORT_CFT_FOLDER_NAME.equals(cftDirectory.getName())) {
                filesToDelete.addAll(getCftFilesToDeleteForDeliveryReport(fileRepo.getDirectoriesInFolder(cftDirectory)));
            }
        }
        return filesToDelete;
    }

    private List<File> getCftFilesToDeleteForList(List<File> filesForList, ReglissList reglissList, Long versionNumber) {
        List<File> filesToDelete = new ArrayList<>();
        if (!filesForList.isEmpty()) {
            List<FileDelivery> listDeliveriesForVersion = new ArrayList<>();
            Optional<Version> version = reglissList.getVersions().stream()
                    .filter(Version::isNotDeleted)
                    .filter(v -> v.getVersionNumber().equals(versionNumber))
                    .findFirst();
            if (version.isPresent()) {
                listDeliveriesForVersion = fileDeliveryRepo
                        .getUncompletedVersionDeliveries(version.get().getId());
            }

            for (File file : filesForList) {
                if (fileCanBeDeleted(file, listDeliveriesForVersion)) {
                    filesToDelete.add(file);
                }
            }
        }

        return filesToDelete;
    }

    private List<File> getCftFilesToDeleteForOfac(List<File> ofacFiles) {
        List<File> filesToDelete = new ArrayList<>();
        List<FileDelivery> listDeliveriesForVersion = fileDeliveryRepo.getUncompletedOfacDeliveries();
        for (File file : ofacFiles) {
            if (fileCanBeDeleted(file, listDeliveriesForVersion)) {
                filesToDelete.add(file);
            }
        }
        return filesToDelete;
    }

    private List<File> getCftFilesToDeleteForDeliveryReport(List<File> subscriberDirectories) {
        List<File> filesToDelete = new ArrayList<>();
        for (File subscriberDirectory : subscriberDirectories) {
            List<File> subscriberFiles = fileRepo.getUnlockedFilesInFolder(subscriberDirectory);
            Subscriber subscriber = subscriberRepo.findByCftFluxName(subscriberDirectory.getName());
            List<FileDelivery> listDeliveriesForSubscriber = fileDeliveryRepo.getUncompletedPVDeliveriesForSubscriber(subscriber);
            for (File file : subscriberFiles) {
                if (fileCanBeDeleted(file, listDeliveriesForSubscriber)) {
                    filesToDelete.add(fileRepo.getFileByName(subscriberDirectory, file.getName()));
                }
            }
        }
        return filesToDelete;
    }

    private boolean fileCanBeDeleted(File file, List<FileDelivery> uncompletedDeliveries) {
        long fileCreationTime = fileRepo.getFileCreationTimeToMillis(file.getParentFile(), file.getName());
        long cftFileLifetimeToMillis = cftSentFileLifetime * 60000;
        boolean fileIsOlderThanNeeded = System.currentTimeMillis() - cftFileLifetimeToMillis > fileCreationTime;
        boolean allDeliveriesAreCompleted = uncompletedDeliveries.isEmpty();
        return allDeliveriesAreCompleted || fileIsOlderThanNeeded;
    }

}

