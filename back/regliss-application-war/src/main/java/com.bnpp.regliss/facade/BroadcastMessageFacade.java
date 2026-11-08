package com.bnpp.regliss.facade;

import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.BroadcastMessage;
import com.bnpp.regliss.facade.dto.BroadcastMessageDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.BroadcastMessageRepository;
import com.bnpp.regliss.service.message.BroadcastMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Facade
@ReglissWebProfile
@Slf4j
public class BroadcastMessageFacade {

    @Autowired
    private BroadcastMessageRepository repository;

    @Autowired
    private BroadcastMessageService service;

    public BroadcastMessageDto getMessage() {
        List<BroadcastMessage> messageList = repository.findAll();
        return service.getMessage(messageList);
    }

    public Long createMessage(BroadcastMessageDto dto){
        BroadcastMessage message = service.createMessage(dto);
        repository.save(message);
        return message.getId();
    }

    public void updateMessage(BroadcastMessageDto dto){
        BroadcastMessage message = repository.getExactlyOne(dto.id);
        message.setMessage(dto.message);
        message.setTitle(dto.title);
        message.setStatus(dto.isPublished ? BroadcastMessage.Status.PUBLISHED : BroadcastMessage.Status.DRAFT);
    }

    public void deleteMessage(Long id){
        BroadcastMessage message = repository.getExactlyOne(id);
        repository.delete(message);
    }

}
