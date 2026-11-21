package com.bnpp.regliss.usersync;

import com.bnpp.regliss.batch.service.NodeDetailsSupplier;
import com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter;
import com.bnpp.regliss.entity.CftType;
import com.bnpp.regliss.entity.batch.Heartbeat;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.batch.HeartbeatRepository;
import com.bnpp.regliss.utils.thread.ThreadScopeContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@Component
@ReglissWebProfile
@Slf4j
public class RefogPoller {

    @Autowired
    private UserUpdateService userUpdateService;

    @Autowired
    private HeartbeatRepository heartbeatRepository;

    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;

    @Scheduled(cron = "${task.users_sync.cron}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @CloseResourcesAfter
    public void usersSyncBetweenReglissAndRefog() {
        List<Heartbeat> hostnames = heartbeatRepository.findByCftTypeIn(Arrays.asList(CftType.PRIMARY, CftType.SECONDARY));
        Optional<Heartbeat> hostName = hostnames.stream().filter(a -> a.getLastBeat().truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))).findFirst();

        if (!hostName.isPresent()) {
            hostName = hostnames.stream().filter(a -> a.getLastBeat().truncatedTo(ChronoUnit.MINUTES).equals((LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)).minusMinutes(1))).findFirst();
        }

        if (hostName.isPresent()) {
            try {
                log.info("Started users sync process between Regliss and Refog");
                String nodeId = nodeDetailsSupplier.getHostName();
                if (nodeId.equalsIgnoreCase(hostName.get().getHostname())) {
                    userUpdateService.getUsersFromRefog();
                }
            } catch(IOException | MessagingException e){
                e.printStackTrace();
            } finally{
                // Clears DJReferentialHolder
                ThreadScopeContextHolder.clearThread();
                log.info("Ended users sync process between Regliss and Refog");
            }
        } else {
            log.info("No server is active at the moment to process the refog job");
        }
    }
}
