package com.bnpp.regliss.vo;

import java.time.LocalDateTime;

public interface ConcatenatedListDisplayVO {
    Long getId();
    String getLabel();
    LocalDateTime getPublicationDate();
    boolean isActive();
}
