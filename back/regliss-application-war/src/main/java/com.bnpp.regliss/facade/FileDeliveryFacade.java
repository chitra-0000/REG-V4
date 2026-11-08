package com.bnpp.regliss.facade;

import static java.util.stream.Collectors.*;

import java.util.*;

import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.search.FileDeliverySearchCriteria;
import org.springframework.beans.factory.annotation.*;

import com.bnpp.regliss.*;
import com.bnpp.regliss.ReglissException.*;
import com.bnpp.regliss.config.facade.*;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.exporter.*;
import com.bnpp.regliss.facade.dto.administration.*;
import com.bnpp.regliss.facade.mapper.*;
import com.bnpp.regliss.profile.*;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.service.*;
import com.bnpp.regliss.service.permission.*;
import com.bnpp.regliss.vo.*;

import static com.bnpp.regliss.utils.TextNormalizerQueryUtil.normalizePartialMatchCriteria;

@Facade
@ReglissWebProfile
public class FileDeliveryFacade {

    @Autowired
    private FileDeliveryRepository repo;

    @Autowired
    private SubscriberRepository subscriberRepo;

    @Autowired
    private FileDeliveryMapper mapper;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private FileDeliveryService service;

    @Autowired
    private GeneratedFileService generatedFileService;

    @Autowired
    private SubscriberAuditService auditService;

    @Autowired
    private CurrentFeaturePermissions featurePermissions;

    @Autowired
    private GeneratedFileRepository generatedFileRepository;

    @Autowired
    private ConcatenatedListRepository concatenatedListRepository;

    public List<FileDeliverySearchResultDto> getFileDeliveries() {
        auditService.logDiffusionLoad();

        List<FileDelivery> fileDeliveries = repo.getFileDeliveries();
        return fileDeliveries.stream()
                .map(mapper::toFileDeliverySearchResultDto)
                .collect(toList());
    }

    public PaginatedResults<FileDeliverySearchResultDto> search(FileDeliverySearchCriteria criteria) {
        auditService.logDiffusionSearch(criteria);
        normalizePartialMatchCriteriaForBroadcastFollowUpSearch(criteria);
        PaginatedResults<FileDelivery> paginatedEntities = repo.search(criteria);
        return paginatedEntities.map(mapper::toFileDeliverySearchResultDto);
    }

    public void redeliver(Long idFileDelivery) {
        FileDelivery oldDelivery = repo.getExactlyOne(idFileDelivery);
        if (oldDelivery.getGeneratedFile().isGeneratedForOfac()) {
            if (!featurePermissions.has(FeaturePermission.ofacRebroadcast)) {
                throw new ReglissException(ErrorCode.NOT_AUTHORIZED);
            }
        } else {
            if (!featurePermissions.has(FeaturePermission.versionRebroadcast)) {
                throw new ReglissException(ErrorCode.NOT_AUTHORIZED);
            }
        }
        service.createDeliveryWithInitialRetries(oldDelivery.getGeneratedFile(), oldDelivery.getSubscription(),
                requestContext.getRequestTime(), requestContext.getCurrentUserRef());
    }

    public void redeliverFilesForOfac(List<Long> subscriberList){
        List<GeneratedFile> filesToBeRedelivered = generatedFileService.getLatestGeneratedFilesForOfac().stream()
                .filter(GeneratedFile::isCorrectlyGenerated)
                .collect(toList());
        service.scheduleRedeliveries(filesToBeRedelivered, subscriberList);
    }

    public void redeliverFilesForConcatenatedList(Long id, List<Long> subscriberList){
        ConcatenatedList concatenatedList = concatenatedListRepository.getExactlyOne(id);
        List<FileExportFormat> exportFormatsForConcatenatedList = concatenatedList.getConcatenatedListExportFormats();
        List<GeneratedFile> filesToBeRedelivered = generatedFileService.getLatestGeneratedFiles(concatenatedList.getGeneratedFiles()).stream()
                .filter(generatedFile -> exportFormatsForConcatenatedList.contains(generatedFile.getFileExportFormat()))
                .filter(GeneratedFile::isCorrectlyGenerated)
                .collect(toList());
        service.scheduleRedeliveries(filesToBeRedelivered, subscriberList);
    }

    public void redeliverFilesForSubscriber(Long subscriberId){

        Subscriber subscriber = subscriberRepo.getExactlyOne(subscriberId);
        for (Subscription subscription : subscriber.getSubscriptions()) {
            List<GeneratedFile> files = generatedFileRepository
                    .getGeneratedFilesForAllPublishedVersionsForSubscriberAndSubscription(subscriberId, subscription.getId());
            generatedFileService.getLatestGeneratedFiles(files).stream()
                    .filter(GeneratedFile::isCorrectlyGenerated)
                    .filter(gf -> gf.getGeneratedFileFormat().equals(subscription.getGeneratedFileFormat()))
                    .forEach(f -> service.createDeliveryWithInitialRetries(f, subscription, requestContext.getRequestTime(),
                            requestContext.getCurrentUserRef()));
        }
// [MISSING FROM SCREENSHOTS: lines 121-121 of the original file were not visible in the photos]
        Set<Subscription> ofacSubscriptions = service.getOfacSubscriptions(subscriberId);
        if (!ofacSubscriptions.isEmpty()) {
            List<GeneratedFile> ofacGeneratedFiles = generatedFileRepository.getOfacGeneratedFiles();
            for (Subscription subscription : ofacSubscriptions) {
                List<GeneratedFile> files = ofacGeneratedFiles.stream()
                        .filter(GeneratedFile::isCorrectlyGenerated)
                        .filter(gf -> gf.getGeneratedFileFormat().equals(subscription.getGeneratedFileFormat()))
                        .collect(toList());
                generatedFileService.getLatestGeneratedFiles(files).stream()
                        .forEach(f -> service.createDeliveryWithInitialRetries(f, subscription, requestContext.getRequestTime(),
                                requestContext.getCurrentUserRef()));
            }
        }
    }

    public void redeliverFilesForVersion(Long versionId, List<Long> subscriberList) {
        List<GeneratedFile> filesToBeRedelivered = generatedFileService.getLatestGeneratedFilesForVersion(versionId).stream()
                .filter(GeneratedFile::isCorrectlyGenerated)
                .collect(toList());
        service.scheduleRedeliveries(filesToBeRedelivered, subscriberList);
    }

    private void normalizePartialMatchCriteriaForBroadcastFollowUpSearch(FileDeliverySearchCriteria criteria) {
        criteria.setListName(normalizePartialMatchCriteria(criteria.getListName()));
        criteria.setFluxName(normalizePartialMatchCriteria(criteria.getFluxName()));
        criteria.setSubscriberName(normalizePartialMatchCriteria(criteria.getSubscriberName()));
    }

}

