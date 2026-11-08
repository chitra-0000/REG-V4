package com.bnpp.regliss.facade.dto.administration;

// [MISSING FROM SCREENSHOTS: lines 3-5 of the original file were not visible in the photos]

public class FileDeliverySearchResultDto {
    public Long id;
    public LocalDateTime actualRunTime;
    public String listName;
    public Long versionNumber;
    public Long versionId;
    public String subscriberName;
    public String fluxName;
    public String fileName;
    public Status status;
    public String errorMessage;
    public Boolean canRetry;
}

