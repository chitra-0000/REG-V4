package com.bnpp.regliss.facade.mapper;


import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.facade.dto.administration.SubscriberBreadcrumbDto;
import com.bnpp.regliss.facade.dto.administration.SubscriberDetailsDto;
import com.bnpp.regliss.facade.dto.administration.SubscriberDto;
import com.bnpp.regliss.facade.dto.administration.SubscriptionDto;
import com.bnpp.regliss.facade.dto.list.ReglissListAndFilterSubscriptionDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.service.EntityCollectionDiff;
import com.bnpp.regliss.vo.SubscriberBreadcrumbVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Component
@ReglissWebProfile
public class SubscriberMapper {

    @Autowired
    private CountryRepository countryRepo;

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private GeneratedFileRepository generatedFileRepo;

    @Autowired
    private DJExportFilterRepository watchlistFormatRepo;

    @Autowired
    private ConcatenatedListRepository concatenatedListRepository;

    public static final Comparator<SubscriptionDto> subscriberListCustomComparator = new Comparator<SubscriptionDto>() {
        public int compare(SubscriptionDto o1, SubscriptionDto o2) {
            return extractSubscriberId(o1.list.listFilterName)-extractSubscriberId(o2.list.listFilterName);
        }

        private int extractSubscriberId(String subscriberName){
            List<String> nameElements = Arrays.asList(subscriberName.split("-"));
            String subscriberId = nameElements.size() > 1 ? nameElements.get(0) : null;
            return  StringUtils.isNumeric(subscriberId) ? Integer.parseInt(subscriberId) : Integer.MAX_VALUE;
        }
    };

    public SubscriberDto toSearchDto(Subscriber subscriber){
        SubscriberDto dto = new SubscriberDto();
        dto.id = subscriber.getId();
        dto.name = subscriber.getName();
        dto.flux = subscriber.getCftFluxName();
        dto.country = subscriber.getCountryOpt().map(Country::getId).orElse(null);
        dto.entityOrSector = subscriber.getEntityOrSector();
        dto.isActive = subscriber.isActive();
        dto.encryption = subscriber.isEncryption();
        dto.hasDeliveryReport = subscriber.hasDeliveryReport();
        return dto;
    }

    public SubscriberDetailsDto toDto(Subscriber subscriber){
        SubscriberDetailsDto dto = new SubscriberDetailsDto();
        dto.id = subscriber.getId();
        dto.name = subscriber.getName();
        dto.flux = subscriber.getCftFluxName();
        dto.country = subscriber.getCountryOpt().map(Country::getId).orElse(null);
        dto.contact = subscriber.getContact();
        dto.description = subscriber.getDescription();
        dto.entityOrSector = subscriber.getEntityOrSector();
        dto.isActive = subscriber.isActive();
        dto.encryption = subscriber.isEncryption();
        dto.hasDeliveryReport = subscriber.hasDeliveryReport();
        GeneratedFile lastDeliveryReport = generatedFileRepo.getLastGeneratedFileBySubscriberId(subscriber.getId());
        if (lastDeliveryReport != null) {
            dto.deliveryReportStatus = lastDeliveryReport.getGenerationStatus();
        }
        dto.modificationDate = subscriber.getModificationDate();
        Comparator<Subscription> comparator = Comparator.nullsLast(Comparator.comparing(s -> s.getList()!=null?
                s.getList().getReference(): s.getConcatenatedList().getLabel()));
        dto.subscriptions = subscriber.getSubscriptions().stream()
                .filter(Subscription::isNotDeleted).sorted(comparator)
                .map(this::toSubscriptionForListAndConcatenatedListDto)
                .collect(toList());
        dto.diffusionTime = subscriber.getDiffusionTime();
        dto.partner = subscriber.getCftPartner();
        dto.wlFullBroadcastFrequency = subscriber.getWlFullBroadcastFrequency();
        dto.wlDeltaBroadcastFrequency = subscriber.getWlDeltaBroadcastFrequency();

        dto.receiverKeyId = subscriber.getReceiverKeyId();
        return dto;
    }

    public SubscriberDto toSearchDto(Subscriber subscriber, Long listId) {
        SubscriberDto dto = toSearchDto(subscriber);
        dto.listSubscriptionDate = subscriber.getListSubscriptionCreationDate(listId).orElse(null);
        return dto;
    }

    public Subscriber toEntity(Subscriber subscriber, SubscriberDetailsDto dto){
        subscriber.setId(dto.id);
        subscriber.setActive(dto.isActive);
        subscriber.setEncryption(dto.encryption);
        subscriber.setDeliveryReport(dto.hasDeliveryReport);
        subscriber.setContact(dto.contact);
        if (dto.country != null) {
            subscriber.setCountry(countryRepo.getExactlyOne(dto.country));
        } else {
            subscriber.setCountry(null);
        }
        subscriber.setDescription(dto.description);
        subscriber.setEntityOrSector(dto.entityOrSector);
        subscriber.setCftFluxName(dto.flux);
        subscriber.setModificationDate(LocalDateTime.now());
        subscriber.setName(dto.name);
        subscriber.setDiffusionTime(dto.diffusionTime);
        subscriber.setCftPartner(dto.partner);
        subscriber.setWlFullBroadcastFrequency(dto.wlFullBroadcastFrequency);
        subscriber.setWlDeltaBroadcastFrequency(dto.wlDeltaBroadcastFrequency);
        subscriber.setReceiverKeyId(dto.receiverKeyId);
        mapSubscriptions(subscriber, dto);
        return subscriber;
    }

    public Subscriber updateSubscriptionsOfSubscriber(Subscriber subscriber, List<SubscriptionDto> subscriptions){
        mapSubscriptionsOfSubscriber(subscriber, subscriptions);
        return subscriber;
    }

    public Subscriber updateSubscriptionsOfSubscriberForListAndConcatenatedList(Subscriber subscriber, List<SubscriptionDto> subscriptions){
        Set<Subscription> newSubscriptions =  mapSubscriptionsOfSubscriberForListAndConcatenatedList(subscriber, subscriptions);
        manageSubscriptionsDiff(subscriber, newSubscriptions);
        return subscriber;
    }

    private void mapSubscriptionsOfSubscriber(Subscriber entity, List<SubscriptionDto> subscriptions) {
        Set<Subscription> newSubscriptions = subscriptions.stream()
                .map(this::toSubscription).collect(Collectors.toSet());

        EntityCollectionDiff<Subscription> subscriptionDiff = new EntityCollectionDiff<>(entity.getSubscriptions(),
                newSubscriptions, e -> Collections.singletonList(e.getId()));

        for (Subscription toCreate : subscriptionDiff.getCreatedElements()) {
            entity.addSubscription(toCreate);
        }

        for (Subscription toDelete : subscriptionDiff.getDeletedElements()) {
            entity.removeSubscription(toDelete);
        }
    }

    private Set<Subscription> mapSubscriptionsOfSubscriberForListAndConcatenatedList(Subscriber subscriber,
                                                                                     List<SubscriptionDto> subscriptions) {
        return subscriptions.stream()
                .map(s->this.toSubscriptionForListAndConcatenatedList(subscriber,s)).collect(Collectors.toSet());
    }

    private void mapSubscriptions(Subscriber entity, SubscriberDetailsDto dto) {
        Set<Subscription> newSubscriptions = dto.subscriptions.stream()
                .map(this::toSubscription).collect(Collectors.toSet());

        manageSubscriptionsDiff(entity, newSubscriptions);
    }

    private void manageSubscriptionsDiff(Subscriber entity, Set<Subscription> newSubscriptions) {
        EntityCollectionDiff<Subscription> subscriptionDiff = new EntityCollectionDiff<>(entity.getSubscriptions(),
                newSubscriptions, e -> Collections.singletonList(e.getId()));

        for (Subscription toCreate : subscriptionDiff.getCreatedElements()) {
            entity.addSubscription(toCreate);
        }

        for (Subscription toDelete : subscriptionDiff.getDeletedElements()) {
            entity.removeSubscription(toDelete);
        }
    }

    public SubscriptionDto toSubscriptionDto(Subscription entity){
        SubscriptionDto dto = new SubscriptionDto();
        dto.id = entity.getId();
        dto.fileExtension = entity.getGeneratedFileFormat().getFileExtension();
        dto.fileFormat = entity.getGeneratedFileFormat().getFileFormat();
        dto.exportMode = entity.getGeneratedFileFormat().getFileExportMode();
        dto.list = new ReglissListAndFilterSubscriptionDto(entity.getList(), entity.getGeneratedFileFormat().getFilter());
        return dto;
    }

    public SubscriptionDto toSubscriptionForListAndConcatenatedListDto(Subscription entity){
        SubscriptionDto dto = new SubscriptionDto();
        dto.id = entity.getId();
        dto.creationDate = entity.getCreationDate();
        dto.fileExtension = entity.getGeneratedFileFormat().getFileExtension();
        dto.fileFormat = entity.getGeneratedFileFormat().getFileFormat();
        dto.exportMode = entity.getGeneratedFileFormat().getFileExportMode();
        dto.list = new ReglissListAndFilterSubscriptionDto(entity.getList(), entity.getGeneratedFileFormat().getFilter(),
                entity.getConcatenatedList());
        return dto;
    }

    private Subscription toSubscription(SubscriptionDto dto){
        ReglissList list = null;
        DJExportFilter watchlistFormat;
        if(dto.list.listId != null){
            list = listRepo.findById(dto.list.listId).orElseThrow(NoSuchElementException::new);
        }
        Subscription entity;
        if(dto.list.filterId != null){
            watchlistFormat = watchlistFormatRepo.findById(dto.list.filterId).orElseThrow(NoSuchElementException::new);
            entity = new Subscription(new GeneratedFileFormat(watchlistFormat,dto.fileExtension), list);
        } else {
            entity = new Subscription(new GeneratedFileFormat(dto.fileFormat, dto.exportMode, dto.fileExtension), list);
        }
        entity.setId(dto.id);
        return entity;
    }

    private Subscription toSubscriptionForListAndConcatenatedList(Subscriber s, SubscriptionDto dto){
        ReglissList list = null;
        DJExportFilter watchlistFormat;
        if(dto.list.listId != null){
            list = listRepo.findById(dto.list.listId).orElseThrow(NoSuchElementException::new);
        }
        Subscription entity;
        if(dto.list.filterId != null){
            watchlistFormat = watchlistFormatRepo.findById(dto.list.filterId).orElseThrow(NoSuchElementException::new);
            entity = new Subscription(new GeneratedFileFormat(watchlistFormat,dto.fileExtension), list, s, dto.creationDate);
        } else if (list != null) {
            entity = new Subscription(new GeneratedFileFormat(dto.fileFormat, dto.exportMode, dto.fileExtension), list, s, dto.creationDate);
        } else {
            entity = new Subscription(new GeneratedFileFormat(dto.fileFormat, dto.exportMode, dto.fileExtension),
                    concatenatedListRepository.getExactlyOne(dto.list.concatenatedListId), s, dto.creationDate);
        }
        entity.setId(dto.id);
        return entity;
    }

    public SubscriberBreadcrumbDto toSubscriberBreadcrumbDto(SubscriberBreadcrumbVO entity){
        SubscriberBreadcrumbDto  dto = new SubscriberBreadcrumbDto();
        dto.id = entity.getId();
        dto.name = entity.getName();
        return dto;
    }

    public List<SubscriptionDto> toSubscriptionDto(Subscriber subscriber){
        return subscriber.getSubscriptions().stream()
                .filter(Subscription::isNotDeleted)
                .map(this::toSubscriptionForListAndConcatenatedListDto)
                .collect(toList());
    }

}
