package com.bnpp.regliss.service.message;

import com.bnpp.regliss.entity.BroadcastMessage;
import com.bnpp.regliss.facade.dto.BroadcastMessageDto;
import com.bnpp.regliss.facade.mapper.BroadcastMessageMapper;
import com.bnpp.regliss.profile.ReglissWebProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ReglissWebProfile
public class BroadcastMessageService {

    @Autowired
    BroadcastMessageMapper mapper;

    public BroadcastMessageDto getMessage(List<BroadcastMessage> messageList) {
        if(messageList.isEmpty()){
            BroadcastMessageDto emptyMessage = new BroadcastMessageDto();
            emptyMessage.message = "";
            return emptyMessage;
        }
        return mapper.toBroadcastMessageDto(messageList.get(0));
    }

    public BroadcastMessage createMessage(BroadcastMessageDto dto) { return mapper.fromDtoToEntity(dto); }

}


