package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.facade.BroadcastMessageFacade;
import com.bnpp.regliss.facade.dto.BroadcastMessageDto;
import com.bnpp.regliss.facade.dto.SingleValue;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.vo.FeaturePermission;
import com.bnpp.regliss.web.filter.SecuredFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/message")
@ReglissWebProfile
public class BroadcastMessageController {

    @Autowired
    private BroadcastMessageFacade facade;

    @GetMapping
    public BroadcastMessageDto getMessage() {
        return facade.getMessage();
    }

    @SecuredFeature(FeaturePermission.manageMessages)
    @PostMapping
    public SingleValue createMessage(@RequestBody BroadcastMessageDto broadcastMessageDto) {
        long newId = facade.createMessage(broadcastMessageDto);
        return new SingleValue(newId);
    }

    @SecuredFeature(FeaturePermission.manageMessages)
    @PutMapping
    public void updateMessage(@RequestBody BroadcastMessageDto broadcastMessageDto) {
        facade.updateMessage(broadcastMessageDto);
    }

    @SecuredFeature(FeaturePermission.manageMessages)
    @DeleteMapping("/{id}")
    public void deleteMessage(@PathVariable long id) {
        facade.deleteMessage(id);
    }
}

