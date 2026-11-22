package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.VersionStatus;
import com.bnpp.regliss.entity.ImportedFile.UploadType;

import java.time.LocalDateTime;
import java.util.Date;

public interface ListVersionVO {
    public Long getId();
    public Long getVersionNumber();
    public VersionStatus getStatus();
    public LocalDateTime getPublicationDate();
    public LocalDateTime getCertificationDate();
    public Date getStartDate();
    public String getUsername();
    public UploadType getUploadType();
    public Long getCreated();
    public Long getUpdated();
    public Long getDeleted();
    public Long getRecordNumber();
}
