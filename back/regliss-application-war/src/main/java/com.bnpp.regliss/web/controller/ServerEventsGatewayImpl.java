package com.bnpp.regliss.web.controller;

import com.bnpp.regliss.entity.ServerEventsChannelType;
import com.bnpp.regliss.service.ServerEventsGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ServerEventsGatewayImpl implements ServerEventsGateway {

    private static final Logger log = LoggerFactory.getLogger(ServerEventsGatewayImpl.class);

    // STATE !!!
    private Map<ServerEventsChannelType, String> currentTokenPerChannel = new EnumMap<>(ServerEventsChannelType.class);
    private List<Consumer<Map<ServerEventsChannelType, String>>> activeBrowsers = new ArrayList<>();

    @PostConstruct
    public void constructDefaultState() {
        Arrays.stream(ServerEventsChannelType.values()).forEach(v -> this.currentTokenPerChannel.put(v, this.getNewToken()));
    }

    private String getNewToken() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @Override
    public synchronized void broadcastEvent(ServerEventsChannelType eventsChannelType) {
        String newToken = getNewToken();
        currentTokenPerChannel.put(eventsChannelType, newToken);
        activeBrowsers.forEach(browser -> browser.accept(currentTokenPerChannel));
        activeBrowsers.clear();
    }

    @Scheduled(fixedDelayString = "${server-events.keep-alive.millis}")
    public synchronized void sendKeepAlive() {
        activeBrowsers.forEach(browser -> browser.accept(this.currentTokenPerChannel));
        activeBrowsers.clear();
    }

    @Override
    public synchronized void executeAtNextEvent(Map<ServerEventsChannelType, String> lastKnownTokenPerChannel,
                                                Consumer<Map<ServerEventsChannelType, String>> callback) {

        boolean atLeastOneTokenIsDifferent = lastKnownTokenPerChannel.entrySet().stream()
                .anyMatch(e -> !e.getValue().equals(currentTokenPerChannel.get(e.getKey())));

        if(atLeastOneTokenIsDifferent) {
            callback.accept(currentTokenPerChannel);
        } else {
            activeBrowsers.add(callback);
            log.trace("Registered new browser for server events");
        }
    }

}

