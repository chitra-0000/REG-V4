package com.bnpp.regliss.exporter;

import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.ServerEventsChannelType;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.service.EmailService;
import com.bnpp.regliss.service.ServerEventsGateway;
import com.bnpp.regliss.vo.WebsocketNotificationVO;
import com.bnpp.regliss.vo.WebsocketNotificationVO.CompletionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@ReglissBatchProfile
@Slf4j
public class FileGenerationNotificationService {

    @Autowired
    private ServerEventsGateway serverEvents;

    @Autowired
    private EmailService emailService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendEmailsAfterPublicationInTx(Set<GeneratedFile> filesGeneratedKo, long versionId) {
        Set<GeneratedFile> deliveryReportsKo = filesGeneratedKo.stream()
                .filter(file -> file.getGeneratedFileFormat().getFileFormat() == FileExportFormat.DELIVERY_REPORT)
                .collect(toSet());
        filesGeneratedKo.removeAll(deliveryReportsKo);

        for (GeneratedFile failedFile : deliveryReportsKo) {
            emailService.sendEmailDeliveryReportKo(failedFile, versionId);
        }

        if (filesGeneratedKo.isEmpty()) {
            emailService.sendEmailPublicationOk(versionId);
            emailService.sendEmailPublicationOkUpdateNotification(versionId);
        } else {
            emailService.sendEmailPublicationKo(versionId,
                    filesGeneratedKo.stream().sorted(comparing(GeneratedFile::getFullName)).collect(toList()));
        }
    }

    public void notifyCompletionToBrowsers(ServerEventsChannelType eventsChannelType) {
        WebsocketNotificationVO notif = new WebsocketNotificationVO(CompletionMessage.GENERATION);
        serverEvents.broadcastEvent(eventsChannelType);
        log.trace("Message sent through websocket: " + notif.getMessage());
    }

}

