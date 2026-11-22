package com.bnpp.regliss.service;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.FileDelivery.Status;
import com.bnpp.regliss.entity.referential.FileExportMode;
import com.bnpp.regliss.entity.referential.WatchlistBroadcastFrequency;
import com.bnpp.regliss.repository.FileDeliveryRepository;
import com.bnpp.regliss.repository.SubscriptionRepository;
import com.bnpp.regliss.utils.FeedIdHelper;
import com.bnpp.regliss.vo.AutomaticImportFeedIdBase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;

@Slf4j
@Service
public class FileDeliveryService {

    @Autowired
    private SubscriptionRepository subscriptionRepo;

    @Autowired
    private FileDeliveryRepository repo;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private EmailService emailService;

    @Value("${file_delivery.initial_retries}")
    private int initialRetries;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleDeliveriesInTx(Collection<GeneratedFile> filesGeneratedOk, LocalDateTime publicationDate,
            GenerationReason publicationScope) {
        log.debug("Scheduling deliveries for {} files", filesGeneratedOk.size());
        for (GeneratedFile generatedFile : filesGeneratedOk) {
            LocalDateTime requestTime;
            User sysUserRef = referenceService.getSysUser();

            if (generatedFile.isDeliveryReport()) {
                requestTime = getScheduledRunTime(generatedFile.getSubscriberOpt().orElseThrow(() -> new IllegalArgumentException("Subscriber cannot be null")), publicationDate, publicationScope);
                createDeliveryWithInitialRetries(generatedFile, null, requestTime, sysUserRef);
            } else {
                List<Subscription> subscriptionsByFormat = getSubscriptionsByFormat(
                        getSubscriptionsToTrigger(generatedFile), generatedFile);
                for (Subscription subscription : subscriptionsByFormat) {
                    requestTime = getScheduledRunTime(subscription.getSubscriber(), publicationDate, publicationScope);
                    createDeliveryWithInitialRetries(generatedFile, subscription, requestTime, sysUserRef);
                }
            }
        }
    }


    public boolean deliveryShouldBeScheduled(GeneratedFile generatedFile, Subscription subscription) {
        Optional<Version> versionOpt = generatedFile.getVersionOpt();
        if(!versionOpt.isPresent()){ // This is OFAC scenario
            return true;
        } else if (generatedFile.isNotDJWatchlistFormat()) {
            return true;
        } else {
            return shouldWLDeliveryBeScheduled(generatedFile, subscription.getSubscriber());
        }
    }

    public boolean shouldWLDeliveryBeScheduled(GeneratedFile generatedFile, Subscriber subscriber) {
        WatchlistBroadcastFrequency wlFullBroadcastFrequency = subscriber.getWlFullBroadcastFrequency();
        WatchlistBroadcastFrequency wlDeltaBroadcastFrequency = subscriber.getWlDeltaBroadcastFrequency();
        boolean isFullImport = generatedFile.getVersionOpt().map(this::getAutomaticImportFeedId)
                .map(AutomaticImportFeedIdBase::isFull)
                .orElse(false);
        boolean isDeltaImport = !isFullImport;
        boolean isFullExportFile = generatedFile.getFileExportMode() == FileExportMode.FULL;
        boolean isDeltaExportFile = generatedFile.getFileExportMode() == FileExportMode.ADD_MOD;
        boolean shouldWLDeliveryBeScheduled = false;
        if (wlFullBroadcastFrequency == WatchlistBroadcastFrequency.ALL_UPDATES && wlDeltaBroadcastFrequency == WatchlistBroadcastFrequency.ALL_UPDATES) {
            shouldWLDeliveryBeScheduled = true;
        }

        if (wlFullBroadcastFrequency == WatchlistBroadcastFrequency.ONCE_PER_MONTH && wlDeltaBroadcastFrequency == WatchlistBroadcastFrequency.ALL_UPDATES
                && ((isFullExportFile && isFullImport) || (isDeltaImport && isDeltaExportFile))) {
            shouldWLDeliveryBeScheduled = true;
        }

        if (wlFullBroadcastFrequency == null && wlDeltaBroadcastFrequency == WatchlistBroadcastFrequency.ALL_UPDATES && isDeltaExportFile) {
            shouldWLDeliveryBeScheduled = true;
        }

        if (wlFullBroadcastFrequency == WatchlistBroadcastFrequency.ONCE_PER_MONTH && wlDeltaBroadcastFrequency == null && isFullExportFile && isFullImport) {
            shouldWLDeliveryBeScheduled = true;
        }

        if (wlFullBroadcastFrequency == WatchlistBroadcastFrequency.ALL_UPDATES && wlDeltaBroadcastFrequency == null && isFullExportFile) {
            shouldWLDeliveryBeScheduled = true;
        }

        return shouldWLDeliveryBeScheduled;
    }

    private AutomaticImportFeedIdBase getAutomaticImportFeedId(Version version) {
        ImportFileType importFormat = version.getImportFileType();
        return FeedIdHelper.feedIdFromFilename(version.getFileName(), importFormat);
    }

    public void scheduleRedeliveries(List<GeneratedFile> filesToRedeliver, List<Long> subscriberList) {
        for (GeneratedFile generatedFile : filesToRedeliver) {
            List<Subscription> subscriptionsByFormatAndSubscribers = getSubscriptionsByFormat(getSubscriptionsToTrigger(generatedFile), generatedFile).stream()
                    .filter(s -> subscriberList.contains(s.getSubscriber().getId()))
                    .collect(toList());
            for (Subscription subscription : subscriptionsByFormatAndSubscribers) {
                createDeliveryWithInitialRetries(generatedFile, subscription, requestContext.getRequestTime(), requestContext.getCurrentUserRef());
            }
        }
    }

    public void createDeliveryWithInitialRetries(GeneratedFile generatedFile, Subscription subscription,LocalDateTime requestTime, User userRef) {
        if (deliveryShouldBeScheduled(generatedFile, subscription)) {
            repo.save(new FileDelivery(generatedFile, subscription, requestTime, userRef, initialRetries));
        }
    }

    public List<FileDelivery> getLatestDeliveriesForVersion(Version version) {
        return getLatestDeliveries(repo.getVersionDeliveries(version.getId()));
    }

    private List<FileDelivery> getLatestDeliveries(List<FileDelivery> deliveries) {
        Map<Subscription, Optional<FileDelivery>> latestDeliveriesBySubscription = deliveries.stream()
                .filter(s-> s.getSubscription() != null)
                .collect(groupingBy(FileDelivery::getSubscription, maxBy(comparing(FileDelivery::getScheduledRunTime))));
        return latestDeliveriesBySubscription.values().stream()
                                .map(Optional::get)
                                .collect(toList());
    }

    private List<Subscription> getSubscriptionsByFormat(Set<Subscription> subscriptions, GeneratedFile generatedFile) {
        return subscriptions.stream()
                .filter(s -> s.getGeneratedFileFormat().equals(generatedFile.getGeneratedFileFormat()))
                .collect(toList());
    }

    private Set<Subscription> getSubscriptionsToTrigger(GeneratedFile generatedFile) {
        ReglissList list = generatedFile.getVersionOpt().map(Version::getList).orElse(null);
        if (list != null) {
            return subscriptionRepo.findListSubscriptions(list);
        } else {
            ConcatenatedList concatenatedList = generatedFile.getConcatenatedListOpt().orElse(null);
            return subscriptionRepo.findConcatenatedListSubscriptions(concatenatedList);
        }
    }

    private LocalDateTime getScheduledRunTime(Subscriber subscriber, LocalDateTime publicationDate, GenerationReason publicationScope) {
        if (StringUtils.isEmpty(subscriber.getDiffusionTime()) || publicationScope == GenerationReason.REGENERATION) {
            return publicationDate;
        }

        return computeNextScheduledRunTime(subscriber, publicationDate);
    }

    private LocalDateTime computeNextScheduledRunTime(Subscriber subscriber, LocalDateTime publicationDate) {
        LocalDate subscriptionDate = publicationDate.toLocalDate();
        LocalTime subscriptionTime = LocalTime.parse(subscriber.getDiffusionTime());
        LocalDateTime publicationDateWithSubscriptionTime = LocalDateTime.of(subscriptionDate, subscriptionTime);

        if (publicationDate.isBefore(publicationDateWithSubscriptionTime)) {
            return publicationDateWithSubscriptionTime;
        } else {
            return publicationDateWithSubscriptionTime.plusDays(1);
        }
    }

    public void decreaseRetriesLeftOrSetKoStatus(FileDelivery delivery, String errorMessage, Integer returnCode) {
        int retriesLeft = delivery.getRetriesLeft();
        if (retriesLeft > 0) {
            delivery.setRetriesLeft(--retriesLeft);
            delivery.setStatus(Status.SCHEDULED);
        } else {
            delivery.setStatus(Status.KO);
            delivery.setErrorMessage(errorMessage);
            delivery.setReturnCode(returnCode);

            emailService.sendEmailFileDeliveryKo(delivery);
        }

        repo.save(delivery);
    }

    public void obsoleteDelivery(FileDelivery delivery, String errorMessage) {
        delivery.setStatus(Status.KO);
        delivery.setErrorMessage(errorMessage);
        emailService.sendEmailFileDeliveryKo(delivery);

        repo.save(delivery);
    }


    public void setOkStatus(FileDelivery delivery, int returnCode) {
        delivery.setStatus(Status.OK);
        delivery.setReturnCode(returnCode);

        repo.save(delivery);
    }


    public boolean hasOfac(Long subscriberId) {
        return subscriptionRepo.findOfacSubscriptions().stream().anyMatch(s->s.getId().equals(subscriberId));
    }


    public Set<Subscription> getOfacSubscriptions(Long subscriberId) {
        return subscriptionRepo.findOfacSubscriptions().stream().filter(s->s.getSubscriber().getId().equals(subscriberId)).collect(Collectors.toSet());
    }


    public List<FileDelivery> getDeliveriesOfSubscriberAndFile(GeneratedFile generatedFile, String cftFluxName) {
        List<FileDelivery> deliveriesForSubscriberAndFile = new ArrayList<>();
        if (generatedFile.isDeliveryReport()) {
            deliveriesForSubscriberAndFile.addAll(repo.attemptedReportDeliveriesForSubscriberAndFile(cftFluxName,
                    generatedFile.getFileName(), generatedFile.getGeneratedFileFormat().getFileExtension()));
        } else {
            deliveriesForSubscriberAndFile.addAll(repo.attemptedDeliveriesForSubscriberAndFile(cftFluxName,
                    generatedFile.getFileName(), generatedFile.getGeneratedFileFormat().getFileExtension()));
        }
        return deliveriesForSubscriberAndFile;
    }
}
