package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.facade.FileDeliveryFacade;
import com.bnpp.regliss.facade.dto.administration.FileDeliverySearchResultDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.web.filter.SecuredFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.bnpp.regliss.search.FileDeliverySearchCriteria;

import java.util.List;

@RestController
@RequestMapping("api/file-delivery")
@ReglissWebProfile
public class FileDeliveryController {

    @Autowired
    private FileDeliveryFacade facade;

    @SecuredFeature(FeaturePermission.broadcastView)
    @GetMapping("broadcast-follow-up")
    public List<FileDeliverySearchResultDto> getFileDeliveries() {
        return facade.getFileDeliveries();
    }

    @SecuredFeature(FeaturePermission.broadcastView)
    @PostMapping("search")
    public PaginatedResults<FileDeliverySearchResultDto> search(@RequestBody FileDeliverySearchCriteria criteria){
        return facade.search(criteria);
    }

//  Without Feature
    @PostMapping("{id}/redeliver")
    public void redeliver(@PathVariable Long id) {
        facade.redeliver(id);
    }
    @SecuredFeature({FeaturePermission.versionRebroadcast, FeaturePermission.ofacRebroadcast})
    @PostMapping("subscriber/{subscriberId}/redeliver")
    public void redeliverForSubscriber(@PathVariable Long subscriberId){
        facade.redeliverFilesForSubscriber(subscriberId);
    }
}

