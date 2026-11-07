package com.bnpp.regliss.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class ExcludeLogbackFilter extends Filter<ILoggingEvent> {

    private String messagePart;
    private String threadNamePart;

    public void setThreadNamePart(String threadNamePart) { this.threadNamePart = threadNamePart; }
    public void setMessagePart(String messagePart) { this.messagePart = messagePart; }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getThreadName().contains(threadNamePart) &&
            event.getFormattedMessage().toUpperCase().contains(messagePart.toUpperCase())) {
            return FilterReply.DENY;
        } else {
            return FilterReply.NEUTRAL;
        }
    }
}

