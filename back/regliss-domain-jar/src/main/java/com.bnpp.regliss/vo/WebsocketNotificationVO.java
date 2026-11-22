package com.bnpp.regliss.vo;

import lombok.Data;

@Data
public class WebsocketNotificationVO {

    public enum CompletionMessage {
        GENERATION,
        DELIVERY
    }

    private final String message;

    public WebsocketNotificationVO(CompletionMessage websocketMessage) {
        this.message = websocketMessage.name();
    }

}
