package com.bnpp.regliss.facade.dto.administration;

// [MISSING FROM SCREENSHOTS: lines 3-5 of the original file were not visible in the photos]

public class SubscriberDto {
    public Long id;
    public String name;
    public String flux;
    public String entityOrSector;
    public Long country;
    public boolean isActive;
    public boolean encryption;
    public boolean hasDeliveryReport;
    public Status status;
    public LocalDateTime listSubscriptionDate;
}

