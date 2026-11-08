package com.bnpp.regliss.facade;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.FileDelivery.Status;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.entity.referential.FileExportMode;
import com.bnpp.regliss.exporter.BatchExportService;
import com.bnpp.regliss.exporter.GeneratedFileService;
import com.bnpp.regliss.facade.dto.administration.*;
import com.bnpp.regliss.facade.dto.list.ReglissListAndFilterSubscriptionDto;
import com.bnpp.regliss.facade.mapper.SubscriberMapper;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.search.SubscriberSearchCriteria;
import com.bnpp.regliss.service.GenerationReason;
import com.bnpp.regliss.service.SubscriberAuditService;
import com.bnpp.regliss.service.SubscriberService;
import com.bnpp.regliss.service.permission.CurrentListPermissions;
import com.bnpp.regliss.service.permission.ListPermissionRequest;
import com.bnpp.regliss.vo.SubscriberBreadcrumbVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Facade
@ReglissWebProfile
@Slf4j
public class SubscriberFacade {

    @Autowired
    private SubscriberRepository subscriberRepo;

    @Autowired
    private SubscriptionRepository subscriptionRepo;

    @Autowired
    private DJExportFilterRepository filterRepo;

    @Autowired
    private SubscriberMapper mapper;

    @Autowired
    private SubscriberAuditService auditService;

    @Autowired
    private SubscriberService service;

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private CurrentListPermissions currentListPermissions;

    @Autowired
    private GeneratedFileService generatedFileService;

    @Autowired
    private BatchExportService batchExportService;

    @Autowired
    private ConcatenatedListRepository concatenatedListRepository;

    public SubscriberDetailsDto getSubscriber(long id) { return mapper.toDto(subscriberRepo.getExactlyOne(id)); }

    public List<SubscriberDto> search(SubscriberSearchCriteria criteria){
        auditService.logSubscriberSearch(criteria);
        List<Subscriber> subscribers = subscriberRepo.search(criteria);
        if(!subscribers.isEmpty()){
            Map<Long, Status> statusById = subscriberRepo.getAggregateStatuses(subscribers.stream().map(Subscriber::getId).collect(toList()));
            return subscribers.stream()
                    .map(mapper::toSearchDto)
                    .peek(dto -> dto.status = statusById.get(dto.id))
                    .collect(toList());
        }
        return emptyList();
    }

    public Long createSubscriber(SubscriberDetailsDto dto) {
        Subscriber subscriber = mapper.toEntity(new Subscriber(), dto);
        subscriberRepo.save(subscriber);
        auditService.auditCreate(subscriber);
        if (subscriberRepo.countByCftFluxNameIgnoreCase(dto.flux) > 1) {
            throw new ReglissException(ErrorCode.SUBSCRIPTION_DUPLICATE_CFT_FLUX_NAME);
        }
        return subscriber.getId();
    }

    public void updateSubscriber(SubscriberDetailsDto dto) {
        Subscriber subscriberInDb = subscriberRepo.getExactlyOne(dto.id);
        Subscriber oldSubscriber = new Subscriber(subscriberInDb);
        Subscriber newSubscriber = mapper.toEntity(subscriberInDb, dto);
        newSubscriber = subscriberRepo.save(newSubscriber);
        auditService.auditUpdate(oldSubscriber, newSubscriber);
        if (subscriberRepo.countByCftFluxNameIgnoreCase(dto.flux) > 1) {
            throw new ReglissException(ErrorCode.SUBSCRIPTION_DUPLICATE_CFT_FLUX_NAME);
        }
    }

    public void updateSubscriptionsOfSubscriber(Long subscriberId, List<SubscriptionDto> subscriptions) {
        Subscriber subscriberInDb = subscriberRepo.getExactlyOne(subscriberId);
        Subscriber oldSubscriber = new Subscriber(subscriberInDb);
        Subscriber newSubscriber = mapper.updateSubscriptionsOfSubscriber(subscriberInDb, subscriptions);
        newSubscriber = subscriberRepo.save(newSubscriber);
        auditService.auditUpdate(oldSubscriber, newSubscriber);
    }

    public void updateSubscriptionsOfSubscriberForListAndConcatenatedList(Long subscriberId, List<SubscriptionDto> subscriptions){
        Subscriber subscriberInDb = subscriberRepo.getExactlyOne(subscriberId);
        Subscriber oldSubscriber = new Subscriber(subscriberInDb);
        subscriberInDb = mapper.updateSubscriptionsOfSubscriberForListAndConcatenatedList(subscriberInDb, subscriptions);
        subscriberInDb.setModificationDate(LocalDateTime.now());
        Subscriber newSubscriber = subscriberRepo.save(subscriberInDb);
        auditService.auditUpdate(oldSubscriber, newSubscriber);
    }

    public void updateDeletedStatusOfSubscriptionsForSubscriber(Long subscriberId, List<Long> subscriptionIds) {
        Subscriber subscriberInDb = subscriberRepo.getExactlyOne(subscriberId);
        Subscriber oldSubscriber = new Subscriber(subscriberInDb);
        subscriberInDb.setModificationDate(LocalDateTime.now());
        subscriptionIds.forEach(subscriberInDb::removeSubscription);
        auditService.auditUpdate(oldSubscriber, subscriberInDb);
    }

    public List<SubscriptionDto> getAllSubscriptionCombinations() {
        return service.getAllSubscriptionCombinations().stream()
                .filter(file->file.getGeneratedFileFormat().getFilter()==null||file.getGeneratedFileFormat().getFilter()
                        .getExtensions().contains(file.getGeneratedFileFormat().getFileExtension()))
                .map(mapper::toSubscriptionDto)
                .sorted(SubscriberMapper.subscriberListCustomComparator.thenComparing(s -> s.list.listFilterName))
                .collect(toList());
    }

    public List<SubscriptionDto> getAllSubscriptionsForListAndConcatenatedList() {
        return service.getAllSubscriptionsForListAndConcatenatedList().stream()
                .filter(file->file.getGeneratedFileFormat().getFilter()==null||file.getGeneratedFileFormat().getFilter().getExtensions()
                        .contains(file.getGeneratedFileFormat().getFileExtension()))
                .map(mapper::toSubscriptionForListAndConcatenatedListDto)
                .sorted(SubscriberMapper.subscriberListCustomComparator.thenComparing(s -> s.list.listFilterName))
                .collect(toList());
    }

    public List<SubscriberDto> getAllSubscribersForList(Long listId) {
        ReglissList list = listRepo.getExactlyOne(listId);
        currentListPermissions.checkListAccess(new ListPermissionRequest(list));
        return list.getActiveSubscriptions().stream()
                .map(Subscription::getSubscriber)
                .distinct()
                .sorted(comparing(Subscriber::getName))
                .map(sub -> mapper.toSearchDto(sub, listId))
                .collect(toList());
    }

    public List<SubscriberDto> getActiveSubscribersForList(Long listId) {
        return getAllSubscribersForList(listId).stream()
                .filter(s -> s.isActive)
                .collect(toList());
    }

    public List<SubscriberDetailsDto> getOfacSubscribers() {
        return subscriptionRepo.findOfacSubscriptions().stream()
                .map(Subscription::getSubscriber)
                .filter(Subscriber::isActive)
                .distinct()
                .map(mapper::toDto)
                .collect(toList());
    }

    public List<String> getSubscribersForFilter(Long filterId) {
        DJExportFilter filter = filterRepo.getExactlyOne(filterId);
        currentListPermissions.checkListAccess(new ListPermissionRequest(filter.getList()));
        return subscriptionRepo.findByFilter(filter).stream()
                .filter(Subscription::isNotDeleted)
                .map(Subscription::getSubscriber)
                .filter(Subscriber::isActive)
                .distinct()
                .map(Subscriber::getName)
                .sorted()
                .collect(toList());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void regenerateDeliveryReport(Long subscriberId) {
        Optional<GeneratedFile> pvToGenerateOpt = generatedFileService.getNewPersistedPVGeneratedFileStubInTx(subscriberId);
        if (!pvToGenerateOpt.isPresent()) {
            log.debug("No PV to writeRecords");
            return;
        }
        GeneratedFile pvToGenerate = pvToGenerateOpt.get();
        Optional<Version> version = pvToGenerate.getVersionOpt();
        if( !version.isPresent() ) {
            log.debug("No Version found");
            return;
        }
        batchExportService.persistDeliveryReportExportGenerationInTx(version.get(), pvToGenerate.getId(), GenerationReason.REGENERATION);
    }

    public SubscriberBreadcrumbDto getBreadcrumbForSubscriber(long subscriberId){
        SubscriberBreadcrumbVO subscriber = subscriberRepo.getBreadcrumbForSubscriber(subscriberId)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        SubscriberBreadcrumbDto dto = mapper.toSubscriberBreadcrumbDto(subscriber);
        return dto;
    }

    public List<SubscriptionsDetailsDto> getListPerimeterSubscriptions(long subscriberId){
        Map<String, List<SubscriptionDto>> mapByList = mapper.toSubscriptionDto(subscriberRepo.getExactlyOne(subscriberId)).stream()
                .collect(Collectors.groupingBy(x -> {
                    ReglissListAndFilterSubscriptionDto list = x.list;
                    return list.listFilterName;
                }));

        return mapByList.entrySet().stream()
                .map(e -> new SubscriptionsDetailsDto(e.getKey(), e.getValue()))
                .collect(toList());
    }

    public SubscriptionsByPropertiesDetailsDto  getAllListPerimeterSubscriptionCombinations(){
        SubscriptionsByPropertiesDetailsDto dto = new SubscriptionsByPropertiesDetailsDto();
        Map<String, List<SubscriptionDto>> mapByList = getAllSubscriptionsForListAndConcatenatedList().stream()
            .collect(Collectors.groupingBy(x -> {
            ReglissListAndFilterSubscriptionDto list = x.list;
            return list.listFilterName;
        }));
        List<SubscriptionsDetailsDto> listByList = mapByList.entrySet().stream()
                .map(e -> new SubscriptionsDetailsDto(e.getKey(), e.getValue()))
                .collect(toList());
        dto.listByList = listByList;

        Map<FileExportFormat, List<SubscriptionDto>> mapByFormat = getAllSubscriptionsForListAndConcatenatedList().stream()
                .collect(Collectors.groupingBy(x -> x.fileFormat()));
        List<SubscriptionsDetailsDto> listByFormat = mapByFormat.entrySet().stream()
                .map(e -> new SubscriptionsDetailsDto(e.getKey(), e.getValue()))
                .collect(toList());
        dto.listByFormat = listByFormat;

        Map<FileExportMode, List<SubscriptionDto>> mapByType = getAllSubscriptionsForListAndConcatenatedList().stream()
                .collect(Collectors.groupingBy(x -> x.exportMode));
        List<SubscriptionsDetailsDto> listByType = mapByType.entrySet().stream()
                .map(e -> new SubscriptionsDetailsDto(e.getKey(), e.getValue()))
                .collect(toList());
        dto.listByType = listByType;

        Map<FileExportExtension, List<SubscriptionDto>> mapByExtension = getAllSubscriptionsForListAndConcatenatedList().stream()
                .collect(Collectors.groupingBy(x -> x.fileExtension));
        List<SubscriptionsDetailsDto> listByExtension = mapByExtension.entrySet().stream()
                .map(e -> new SubscriptionsDetailsDto(e.getKey(), e.getValue()))
                .collect(toList());
        dto.listByExtension = listByExtension;

        return dto;
    }

    public List<SubscriberDto> getSubscribersForConcatenatedList(Long id){
        ConcatenatedList concatenatedList = this.concatenatedListRepository.getExactlyOne(id);
        List<Subscriber> subscribers = concatenatedList.getSubscribers();
        return subscribers.stream().map(sub->mapper.toSearchDto(sub)).collect(Collectors.toList());
    }
}

