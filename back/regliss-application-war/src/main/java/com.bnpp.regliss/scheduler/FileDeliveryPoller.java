package com.bnpp.regliss.scheduler;

import static java.lang.Long.parseLong;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.batch.service.NodeDetailsSupplier;
import com.bnpp.regliss.entity.FileDelivery;
import com.bnpp.regliss.entity.FileDelivery.Status;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.Subscriber;
import com.bnpp.regliss.entity.Subscription;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.profile.ReglissPublisherProfile;
import com.bnpp.regliss.repository.FileDeliveryRepository;
import com.bnpp.regliss.service.EmailService;
import com.bnpp.regliss.service.FileDeliveryAuditService;
import com.bnpp.regliss.service.FileDeliveryService;
import com.bnpp.regliss.utils.BlobService;

import lombok.extern.slf4j.Slf4j;

@Component
@ReglissPublisherProfile
@Slf4j
public class FileDeliveryPoller {

    public static final String OFAC_CFT_FOLDER_NAME = "OFAC";
    public static final String DELIVERY_REPORT_CFT_FOLDER_NAME = "PV";

    @Autowired
    private FileDeliveryRepository fileDeliveryRepo;

    @Autowired
    private CftSender cftSender;

    @Autowired
    private FileDeliveryAuditService fileDeliveryAuditService;

    @Autowired
    private FileDeliveryService fileDeliveryService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BlobService blobService;

    @Autowired
    private PgpEncrypterSigner pgpEncrypterSigner;

    @Autowired
    private GpgEncrypterSigner gpgEncrypterSigner;

    @Value("${min.available.free.spaceMB}")
    private String minAvailableSpace;

    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;

    @Autowired
    private HeartbeatService heartbeatService;

    @Scheduled(fixedDelayString = "${task.file_delivery.interval}")
    @Transactional
    public void sendScheduledDeliveries() {
        if (heartbeatService.isNodeActivePublisher()) {
            LocalDateTime currentTime = LocalDateTime.now();
            Optional<FileDelivery> fdOpt = fileDeliveryRepo.getOldestNotProcessed(currentTime);
            if (fdOpt.isPresent()) {
                FileDelivery fileDelivery = fdOpt.get();
                if (isCurrentDeliveryNotObsoleted(fileDelivery)) {
                    sendDelivery(currentTime, fileDelivery);
                }
            }
        }
    }

    private boolean isCurrentDeliveryNotObsoleted(FileDelivery fileDelivery) {
        List<FileDelivery> obsoleteDeliveries = new ArrayList<>();
        if (fileDelivery.getSubscriber().getDiffusionTime() != null) {
            obsoleteDeliveries.addAll(fileDeliveryRepo.findScheduledDeliveriesOfOldVersions());
            obsoleteDeliveries.forEach(fd -> fileDeliveryService.obsoleteDelivery(fd, "SCHEDULED delivery for newer version exists!"));
        }

        return !obsoleteDeliveries.contains(fileDelivery);
    }


    private void sendDelivery(LocalDateTime currentTime, FileDelivery fd) {
        GeneratedFile generatedFile = fd.getGeneratedFile();
        if (generatedFile.isDJWatchlistFormat()) {
            synchedDoSendDelivery(currentTime, fd);
        } else {
            doSendDelivery(currentTime, fd);
        }

        generatedFile.getVersionOpt()
                .filter(Version::wasAutoUploaded)
                .ifPresent(this::sendEmailDeliveryOkForAutomaticLists);
    }

    private void sendEmailDeliveryOkForAutomaticLists(Version version) {
        Map<Subscription, List<FileDelivery>> subscriptionToDeliveryMap = fileDeliveryRepo
                .getVersionDeliveries(version.getId()).stream()
                .collect(Collectors.groupingBy(FileDelivery::getSubscription));
        if (subscriptionToDeliveryMap.keySet().stream()
                .allMatch(subscription -> isDeliveryOkForSubscription(subscriptionToDeliveryMap, subscription))) {
            emailService.sendEmailAutomaticVersionDeliveryOk(version);
        }
    }

    private boolean isDeliveryOkForSubscription(Map<Subscription, List<FileDelivery>> map, Subscription subscription) {
        return map.get(subscription).stream().anyMatch(FileDelivery::isOk);
    }

    private synchronized void synchedDoSendDelivery(LocalDateTime currentTime, FileDelivery fileDelivery) {
        doSendDelivery(currentTime, fileDelivery);
    }

    private void doSendDelivery(LocalDateTime currentTime, FileDelivery fileDelivery) {
        Subscriber subscriber = fileDelivery.getSubscriber();
        fileDelivery.setActualRunTime(currentTime);
        String physicalFileName = fileDelivery.getGeneratedFile().getFullName() + (subscriber.isEncryption() ? ".zip" : "");

        try {
            File destinationFolder = cftSender.getDestinationFolder(getSpecificFolderName(fileDelivery));
            File file = new File(destinationFolder,  org.apache.tika.io.FilenameUtils.normalize(FilenameUtils.getName(physicalFileName))); // NOSONAR: paths are configured not user input
            copyBlobToFile(fileDelivery.getGeneratedFile().getFileContent(), file, subscriber.isEncryption());

            log.info("Sending file '" + file.getName() + "' to subscriber " + subscriber.getName());
            tryToSetReadAndWriteAccess(file);
            String fileName= file.getAbsolutePath();

            file = (subscriber.isEncryption()) ? gpgEncrypterSigner.encryptFileAndSign(file.getAbsolutePath(), subscriber.getReceiverKeyId()) : file;

            String cftLogTimeId = cftSender.sendFile(subscriber.getCftPartner(), subscriber.getCftFluxName(), file.getAbsolutePath(), file.getName());
            fileDelivery.setStatus(Status.IN_PROGRESS);
            fileDelivery.setCftLogTimeId(cftLogTimeId);
            if(subscriber.isEncryption()) {
                gpgEncrypterSigner.removingOriginalFileAndEncrytptedFile(fileName);
            }
            log.info("File sent OK");
        } catch (RuntimeException e) {
            log.error("File sent KO - General Error");
            log.error(e.getMessage(), e);
            fileDeliveryService.decreaseRetriesLeftOrSetKoStatus(fileDelivery,
                    "General Error (non-CFT). Please addAndTestIfNew the logs.", -1);
            fileDeliveryAuditService.auditDelivery(fileDelivery);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void tryToSetReadAndWriteAccess(File file) {
        boolean grantReadAccessTry = file.setReadable(true, false);
        boolean grantWriteAccessTry = file.setWritable(true, false);
        if (!grantReadAccessTry || !grantWriteAccessTry) {
            log.warn("Failed to give read or write permission for cft file delivery for file name {}: grant read try was {}, grant write try was {}",
                    file.getName(), grantReadAccessTry, grantWriteAccessTry);
        }
    }

    private String getSpecificFolderName(FileDelivery fileDelivery) {
        GeneratedFile generatedFile = fileDelivery.getGeneratedFile();
        String fluxName = fileDelivery.getSubscriber().getCftFluxName();
        if(generatedFile.getGeneratedFileFormat().getFileFormat() == FileExportFormat.DELIVERY_REPORT){
            return DELIVERY_REPORT_CFT_FOLDER_NAME + File.separator + fluxName;
        }
        if (!generatedFile.getVersionOpt().isPresent()) {
            return OFAC_CFT_FOLDER_NAME + File.separator + fluxName;
        }
        Version version = generatedFile.getVersionOpt().orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        return version.getList().getReference() + File.separator + version.getVersionNumber() + File.separator + fluxName;
    }

    private void copyBlobToFile(Blob fileContentBlob, File file, boolean encryptionRequired) {
        try {
            long fileContentLength = fileContentBlob.length();
            long freeSpace = file.getParentFile().getFreeSpace();
            if(freeSpace < fileContentLength + parseLong(minAvailableSpace)*1024*1024){
                throw new IOException("Too few disk space!!!");
            }
            if (file.exists()) {
                log.warn("File already exists. Overwriting: " + file.getAbsolutePath());
                tryDeleteFile(file);
            }
            log.debug("Copying blob to disk...");

            try (InputStream zippedBlobInputStream = fileContentBlob.getBinaryStream();
                    OutputStream fileOutputStream = new FileOutputStream(file)) {
                if (encryptionRequired) {
                    IOUtils.copy(zippedBlobInputStream, fileOutputStream);
                } else {
                    blobService.unzipBlob(fileContentBlob, fileOutputStream);
                }
            }

            log.debug("File content copied to file " + file.getAbsolutePath());
        } catch (IOException | SQLException e) {
            throw new ReglissException(e);
        }
    }

    private void tryDeleteFile(File file) {
        try {
            Files.delete(file.toPath());
        } catch (IOException e) {
            log.warn("Could not delete file {}", file.getName(), e);
        }
    }
}

