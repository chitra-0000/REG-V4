package com.bnpp.regliss.service;

import com.bnpp.regliss.entity.ServerEventsChannelType;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Failover for websockets (that don't work now through ZUUL)
 */
public interface ServerEventsGateway {
    void broadcastEvent(ServerEventsChannelType eventsChannelType);
    void executeAtNextEvent(Map<ServerEventsChannelType, String> lastKnownTokenPerChannel, Consumer<Map<ServerEventsChannelType, String>> callback);
}
