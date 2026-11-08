package com.bnpp.regliss.facade.dto.administration;

// [MISSING FROM SCREENSHOTS: lines 3-8 of the original file were not visible in the photos]

public class SubscriberDetailsDto {
    public Long id;
    public String name;
    public String flux;
    public String entityOrSector;
    public String contact;
    public String description;
    public Long country;
    public LocalDateTime modificationDate;
    public boolean isActive;
    public boolean encryption;
    public boolean hasDeliveryReport;
    public GenerationStatus deliveryReportStatus;
    public List<SubscriptionDto> subscriptions = new ArrayList<>();
    public String diffusionTime;
    public String partner;
    public WatchlistBroadcastFrequency wlFullBroadcastFrequency;
    public WatchlistBroadcastFrequency wlDeltaBroadcastFrequency;
    public String receiverKeyId;
}

