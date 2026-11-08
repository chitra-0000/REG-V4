package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.entity.BroadcastMessage;
import com.bnpp.regliss.facade.dto.BroadcastMessageDto;
import com.bnpp.regliss.profile.ReglissWebProfile;
import org.springframework.stereotype.Service;

@Service
@ReglissWebProfile
public class BroadcastMessageMapper {

    public BroadcastMessageDto toBroadcastMessageDto(BroadcastMessage message){
        BroadcastMessageDto dto = new BroadcastMessageDto();
        dto.id = message.getId();
        dto.message = message.getMessage();
        dto.title = message.getTitle();
        dto.isPublished = message.isPublished();
        return dto;
    }

    public BroadcastMessage fromDtoToEntity(BroadcastMessageDto dto){
        BroadcastMessage entity = new BroadcastMessage();
        entity.setMessage(dto.message);
        entity.setTitle(dto.title);
        entity.setStatus(dto.isPublished ? BroadcastMessage.Status.PUBLISHED : BroadcastMessage.Status.DRAFT);
        return entity;
    }

}
