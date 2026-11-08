package com.bnpp.regliss.facade.dto.administration;

// [MISSING FROM SCREENSHOTS: lines 3-8 of the original file were not visible in the photos]

public class SubscriptionDto {

    public Long id;
    public ReglissListAndFilterSubscriptionDto list;
    public FileExportFormat fileFormat;
    public FileExportMode exportMode;
    public FileExportExtension fileExtension;
    public String filterName;
    public LocalDateTime creationDate;
}

