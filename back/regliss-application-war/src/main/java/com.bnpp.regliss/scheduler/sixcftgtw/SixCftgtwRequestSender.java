
package com.bnpp.regliss.scheduler.sixcftgtw;

import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.batch.service.NodeDetailsSupplier;
import com.bnpp.regliss.entity.CftgtwRequest;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.profile.ReglissPublisherProfile;
import com.bnpp.regliss.repository.CftgtwRequestRepository;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.scheduler.CftSender;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Component
@ReglissPublisherProfile
@Slf4j
public class SixCftgtwRequestSender {

    private static final Logger LOGGER = LoggerFactory.getLogger("cftgtw");

    private static final String CFTGTW_FILENAME = "cftgtw.txt";

    @Value("${cft.gtw.hostname.SIX}")
    private String cftgtwHostnameSix;

    @Value("${cft.gtw.idf.six}")
    private String cftgtwIdfSix;

    @Value("${cft.gtw.part.six}")
    private String cftgtwPartSix;

    @Value("${cft.gtw.path_to_file.six}")
    private String cftSixgtwPath;

    @Value("${cft.gtw.gmt.SIX}")
    String cftgtwGmtSix;

    @Value("${cft.gtw.hostpath.SIX}")
    private String cftGtwSixHostPath;

    @Value("${cft.gtw.hostpath.SIXArchive}")
    private String cftGtwSixArchiveHostPath;

    @Value("${cft.gtw.idf.SIX.LISTFILE}")
    private String cftgtwIdfSixListFile;
    @Value("${cft.gtw.idf.SIX.PATTERNRETREIVE}")
    private String cftgtwIdfSixPattern;

    @Value("${cft.gtw.SIX.FilenameCorrection}")
    private String cftgtwFileNameCorrecton;

    @Autowired
    private CftgtwRequestRepository cftgtwRequestRepo;

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private SixCftgtwFeedGenerator feedGenerator;

    @Autowired
    private CftSender cftSender;

    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;

    @Autowired
    private HeartbeatService heartbeatService;

    @Scheduled(fixedDelayString = "${task.sixcftgtw_request_sender.interval}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendScheduledCftgtwRequests() {
        if (heartbeatService.isNodeActivePublisher()) {
            sendCftGtwRequests();
        }
    }

    private void sendCftGtwRequests() {
        List<CftgtwRequest> requestsToSend = cftgtwRequestRepo.findByActualSixRunTimeIsNull();

        for (CftgtwRequest requestToSend : requestsToSend) {
            String cftLogTimeIds = null;
            LocalDateTime cftGatewayTime = LocalDateTime.now(ZoneId.of(cftgtwGmtSix));
            if (cftGatewayTime.isAfter(requestToSend.getRequestTime().minusSeconds(1))) {
                LOGGER.info("Sending new cftgtw requests for format: {} and time: {}", requestToSend.getImportFileType(),
                        requestToSend.getRequestTime());
                requestToSend.setActualRunTime(LocalDateTime.now());
                try {
                    if (requestToSend.getImportFileType().equals(ImportFileType.SIX_LIST_ARCHIVE)) {
                        cftLogTimeIds = (sendCftGtwFile(ImportFileType.SIX_LIST_ARCHIVE));
                    } else {
                        cftLogTimeIds = (sendCftGtwFile(ImportFileType.SIX_LIST));
                    }
                } catch (RuntimeException e) {
                    //log error but continue for other lists/formats
                    log.error("Error trying to send CFTGW request for six raw file");
                    log.error("Error trying to send CFTGW request", e);
                }
            }
            requestToSend.setRemainingCftLogTimeIds(Collections.singletonList(cftLogTimeIds));
        }
    }

    private String sendCftGtwFile(ImportFileType fileType) {
        String logTimeId = null;
        File cftgtwFile = new File(cftSixgtwPath, CFTGTW_FILENAME);
        String fileName = getFileNameSix();
        try {
            if (fileType.equals(ImportFileType.SIX_LIST_ARCHIVE)) {
                logTimeId = cftSender.sendFile(cftgtwPartSix, cftgtwIdfSixPattern, cftgtwFile.getAbsolutePath(),
                        cftgtwHostnameSix + cftGtwSixArchiveHostPath + fileName);
                LOGGER.info(
                        "Cftgtw request sent for Six raw file, with logTimeId: {}. Requested file with name: {}",
                        logTimeId, fileName);
            } else {
                logTimeId = cftSender.sendFile(cftgtwPartSix, cftgtwIdfSixPattern, cftgtwFile.getAbsolutePath(),
                        cftgtwHostnameSix + cftGtwSixHostPath + fileName);
                LOGGER.info(
                        "Cftgtw request sent for Six raw file, with logTimeId: {}. Requested file with name: {}",
                        logTimeId, fileName);
            }
        } catch (RuntimeException e) {
            LOGGER.error(
                    "Cftgtw request was not sent successfully for for Six raw file. Requested file with name: {} . Error message: {}",
                    fileName, e.getMessage());
        }


        return logTimeId;
    }

    private String getFileNameSix() {
        String prefix = "GSM_";
        String numberPattern = "*_";
        String contractID = "203_";
        String fileType = "FULL_";
        String fileExtension = ".xml";
        return prefix + numberPattern + contractID + cftgtwFileNameCorrecton + "_*_" + fileType + "*" + fileExtension;
    }
}



