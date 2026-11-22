package com.bnpp.regliss.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bnpp.regliss.entity.FileDelivery;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.entity.Version;

@Service
public class FileDeliveryAuditService {

    @Autowired
    private VersionService versionService;

    @Autowired
    private ConcatenatedListAuditService concatenatedListAuditService;

    @Autowired
    private SubscriberAuditService subscriberAuditService;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private FileDeliveryService fileDeliveryService;

    public void auditDelivery(FileDelivery fileDelivery) {
        configureRequestContext(fileDelivery.getDeliveryUser());
        if (fileDelivery.isDeliveryReportFile()) {
            subscriberAuditService.auditDeliveryReportDelivery(fileDelivery);
        } else {
            Optional<Version> deliveryVersion = fileDelivery.getGeneratedFile().getVersionOpt();
            if (deliveryVersion.isPresent()) {
                auditVersionFileDelivery(fileDelivery, deliveryVersion.get());
            } else {
                concatenatedListAuditService.deliverOneGeneratedFile(fileDelivery);
            }
        }
    }

    private void auditVersionFileDelivery(FileDelivery fileDelivery, Version deliveryVersion) {
        Stream<FileDelivery> latestDeliveriesOfTheVersionExceptCurrent = fileDeliveryService
                .getLatestDeliveriesForVersion(deliveryVersion)
                .stream()
                .filter(l -> !l.getId().equals(fileDelivery.getId()));
        if (latestDeliveriesOfTheVersionExceptCurrent
                .noneMatch(delivery -> delivery.isScheduled() || delivery.isInProgress())) {
            versionService.deliveriesFinishedForVersion(deliveryVersion);
        }
    }

    private void configureRequestContext(User user) {
        requestContext.setRequestTime(LocalDateTime.now());
        requestContext.setCurrentUser(user);
    }

}
