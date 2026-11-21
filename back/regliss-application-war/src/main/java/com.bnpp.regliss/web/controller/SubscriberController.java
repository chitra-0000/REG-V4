package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.facade.SubscriberFacade;
import com.bnpp.regliss.facade.dto.SingleValue;
import com.bnpp.regliss.facade.dto.administration.*;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.search.SubscriberSearchCriteria;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.web.filter.SecuredFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/subscriber")
@ReglissWebProfile
public class SubscriberController {

    @Autowired
    private SubscriberFacade facade;

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @PostMapping(value="/search")
    public List<SubscriberDto> search(@RequestBody SubscriberSearchCriteria subscriberSearchCriteria){
        return facade.search(subscriberSearchCriteria);
    }

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @PostMapping
    public SingleValue createSubscriber(@RequestBody SubscriberDetailsDto dto){
        return new SingleValue(facade.createSubscriber(dto));
    }

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @PutMapping
    public void updateSubscriber(@RequestBody SubscriberDetailsDto dto) {
        facade.updateSubscriber(dto);
    }

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @PutMapping("/{id}/subscriptions")
    public void updateSubscriptionsOfSubscriber(@PathVariable("id") Long id, @RequestBody List<SubscriptionDto> subscriptions){
        facade.updateSubscriptionsOfSubscriberForListAndConcatenatedList(id, subscriptions);
    }

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @PutMapping("/{id}/update-subscriptions-deleted-status")
    public void updateDeletedStatusOfSubscriptionsForSubscriber(@PathVariable("id") Long id, @RequestBody List<Long> subscriptionIds){
        facade.updateDeletedStatusOfSubscriptionsForSubscriber(id, subscriptionIds);
    }

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @GetMapping(value="/{id}")
    public SubscriberDetailsDto getSubscriber(@PathVariable("id") Long id) {
        return facade.getSubscriber(id);
    }

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @GetMapping(value="/subscriptions/all")
    public List<SubscriptionDto> getAllSubscriptionCombinations() {
        return facade.getAllSubscriptionCombinations();
    }

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @PostMapping(value="/regenerate-delivery-report")
    public void regenerateFiles(@RequestParam("id") Long subscriberId) {
        facade.regenerateDeliveryReport(subscriberId);
    }

    @GetMapping("/breadcrumb-for-subscriber/{id}")
    public SubscriberBreadcrumbDto getBreadcrumbForSubscriber(@PathVariable Long id){
        return facade.getBreadcrumbForSubscriber(id);
    }

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @GetMapping(value="/list-perimeter/{id}")
    public List<SubscriptionsDetailsDto> getListPerimeterSubscriptions(@PathVariable("id") Long id){
        return facade.getListPerimeterSubscriptions(id);
    }

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @GetMapping(value="/subscriptions/list-perimeter/all")
    public SubscriptionsByPropertiesDetailsDto getAllListPerimeterSubscriptionCombinations(){
        return facade.getAllListPerimeterSubscriptionCombinations();
    }

    @SecuredFeature(FeaturePermission.manageSubscribers)
    @GetMapping(value="/get-subscriptions/all")
    public List<SubscriptionDto> getAllSubscriptionsForListAndConcatenatedList(){
        return facade.getAllSubscriptionsForListAndConcatenatedList();
    }
}

