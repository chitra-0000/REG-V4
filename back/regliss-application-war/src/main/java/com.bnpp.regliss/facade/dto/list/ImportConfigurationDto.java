package com.bnpp.regliss.facade.dto.list;

import com.bnpp.regliss.entity.ImportFileType;
public class ImportConfigurationDto {
    public String fileName;
    public ImportFileType importFileType;
    public boolean checksumReception;
    public boolean allowIncrementalImport;
    public boolean logReception;
    public boolean confirmationRequired;
    public Long autoConfirmationThreshold;
    public boolean autoConfirmation;
    public boolean allowAutoUpload;
    public boolean allowManualUpload;
    private boolean hasVersions;
    private boolean isLastVersionPublishedOrDelivered;
    public boolean isLastVersionPublishedOrDelivered() { return isLastVersionPublishedOrDelivered; }
    public void setLastVersionPublishedOrDelivered(boolean lastVersionPublishedOrDelivered) {
        isLastVersionPublishedOrDelivered = lastVersionPublishedOrDelivered;
    }
    public boolean isHasVersions() { return hasVersions; }
    public void setHasVersions(boolean hasVersions) { this.hasVersions = hasVersions; }
}

