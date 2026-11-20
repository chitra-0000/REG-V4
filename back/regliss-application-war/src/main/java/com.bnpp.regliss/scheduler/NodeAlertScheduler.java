package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.batch.service.BatchJobExecutionService;
import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.batch.service.NodeDetailsSupplier;
import com.bnpp.regliss.entity.batch.Heartbeat;
import com.bnpp.regliss.repository.batch.HeartbeatRepository;
import com.bnpp.regliss.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class NodeAlertScheduler {
    @Autowired
    private HeartbeatService heartbeatService;
    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;
    @Autowired
    private HeartbeatRepository heartbeatRepo;
    @Autowired
    private BatchJobExecutionService jobExecutionService;
    @Autowired
    private EmailService emailService;

    @Scheduled(fixedDelayString = "${heartbeat.interval.milliseconds}")
    public void processNodesWithNoHearbeat() {
        List<String> inactiveNodes = heartbeatService.getAllInactiveNodeIds();
        List<String> activeNodes = heartbeatService.getAllActiveNodeIds();

        inactiveNodes.forEach(this::trySendNodeDownMailAlert);
        activeNodes.forEach(this::trySendMailForEachStalledBatchJob);

        processActivePublisherWithNoHeartbeat();
    }

    private void processActivePublisherWithNoHeartbeat() {
        try{
            //if current node is publisher node and no other  Active node has heartbeat, allow this node to become active publisher
            heartbeatService.markNodeAsActivePublisherIfOtherNodeIsDown().ifPresent(this::trySendNewActivePublisherNodeMailAlert);
        }catch(RuntimeException e){
            log.error("CouldNot set node as active publisher",e);
            log.error("Could not set node {} as active publisher. {}",nodeDetailsSupplier.getNodeId(),e.getMessage());
        }
    }


    private void trySendNodeDownMailAlert(String nodeId) {
        heartbeatService.getNodeIfInactive(nodeId)
            .ifPresent(emailService::sendEmailHeartbeatNodeDown);
    }

    private void trySendNewActivePublisherNodeMailAlert(String nodeId) {
        heartbeatRepo.findByNodeId(nodeId)
                .ifPresent(o->{
                    Optional<Heartbeat> newHeartbeatOpt = heartbeatRepo.findByNodeId(nodeDetailsSupplier.getNodeId());
                    newHeartbeatOpt.ifPresent(heartbeat -> emailService.sendEmailNewActivePublisherNode(o, heartbeat));
                });
    }

    private void trySendMailForEachStalledBatchJob(String nodeId) {
        jobExecutionService.getStalledJobsForNode(nodeId)
            .forEach(emailService::sendEmailBatchJobStalled);
    }
}

