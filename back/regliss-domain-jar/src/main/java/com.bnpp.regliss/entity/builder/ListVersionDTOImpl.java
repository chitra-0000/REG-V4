package com.bnpp.regliss.entity.builder;

import com.bnpp.regliss.entity.ListVersionDTO;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;

import java.util.Date;

public class ListVersionDTOImpl implements ListVersionDTO {

    private final Long versionId;
    private final Long listId;
    private final Date publicationDate;
    private final Date startDate;
    private final String listReference;
    private final Long versionNumber;

    public ListVersionDTOImpl (Version version, ReglissList list){
        this.versionId= version.getId();
        this.listId=list.getId();
        this.publicationDate=version.getPublicationDate()!=null?java.sql.Timestamp.valueOf(version.getPublicationDate()):null;
        this.startDate=version.getStartDate()!=null?java.sql.Date.valueOf(version.getStartDate()):null;
        this.versionNumber=version.getVersionNumber();
        this.listReference=list.getReference();
    }
    @Override
    public Long getVersionId() { return versionId; }

    @Override
    public Date getPublicationDate() { return publicationDate; }

    @Override
    public Long getVersionNumber() { return versionNumber; }

    @Override
    public Date getStartDate() { return startDate; }

    @Override
    public Long getListId() { return listId; }

    @Override
    public String getListName() { return null; }

    @Override
    public String getListFormat() { return null; }

    @Override
    public String getListReference() { return listReference; }
}
