package com.bnpp.regliss.entity;

import java.util.Date;

public interface ListVersionDTO {
    Long getVersionId();
    Date getPublicationDate();
    Long getVersionNumber();
    Date getStartDate();

    Long getListId();
    String getListName();
    String getListFormat();

    String getListReference();
}
