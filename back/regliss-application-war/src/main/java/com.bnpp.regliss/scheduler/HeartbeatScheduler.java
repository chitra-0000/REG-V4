package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.batch.service.NodeDetailsSupplier;
import com.bnpp.regliss.entity.batch.Heartbeat;
import com.bnpp.regliss.repository.batch.HeartbeatRepository;
import com.bnpp.regliss.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Component
@Slf4j
public class HeartbeatScheduler {

    @Autowired
    private HeartbeatService service;
    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;
    @Autowired
    private HeartbeatRepository heartbeatRepo;
    @Autowired
    private EmailService emailService;

    @PostConstruct
    private void insertNodeIntoHearbeat() {
        service.insertOrUpdateHeartbeat();
        processActivePublisherIfPrimaryIsUp();

    }

    private void processActivePublisherIfPrimaryIsUp() {
        try{
            //if current node is publisher node and no other  Active node has heartbeat, allow this node to become active publisher
            service.markNodeAsActivePublisherIfIsPrimary().ifPresent(this::trySendNewPrimaryPublisherNodeMailAlert);
        }catch(RuntimeException e){
            log.error("CouldNot set PRIMARY node as active publisher",e);
            log.error("Could not set PRIMARY node {} as active publisher. {}",nodeDetailsSupplier.getNodeId(),e.getMessage());
        }
    }
    private void trySendNewPrimaryPublisherNodeMailAlert(String nodeId) {
        heartbeatRepo.findByNodeId(nodeId)
                .ifPresent(o->{
                    Optional<Heartbeat> newHeartbeatOpt = heartbeatRepo.findByNodeId(nodeDetailsSupplier.getNodeId());
                    newHeartbeatOpt.ifPresent(heartbeat -> emailService.sendEmailPrimaryActivePublisherNode(o, heartbeat));
                });
    }

    @Scheduled(fixedDelayString = "${heartbeat.interval.milliseconds}")
    public void sendHeartbeat() { service.insertOrUpdateHeartbeat(); }
}

