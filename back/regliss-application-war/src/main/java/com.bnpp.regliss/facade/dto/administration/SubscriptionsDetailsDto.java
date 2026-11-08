package com.bnpp.regliss.facade.dto.administration;

// [MISSING FROM SCREENSHOTS: lines 3-9 of the original file were not visible in the photos]

public class SubscriptionsDetailsDto {
    public String listName;
    public FileExportFormat format;
    public FileExportMode type;
    public FileExportExtension extension;
    public int countFormat;
    public int countType;
    public int countExtension;
    public List<SubscriptionDto> combinations = new ArrayList<>();

    public SubscriptionsDetailsDto(){}
    public SubscriptionsDetailsDto(String listName, List<SubscriptionDto> subscriptions) {
        this.listName=listName;
        this.setSubscriptionsData(subscriptions);
    }

    public SubscriptionsDetailsDto(FileExportFormat format, List<SubscriptionDto> subscriptions) {
        this.format=format;
        this.setSubscriptionsData(subscriptions);
    }

    public SubscriptionsDetailsDto(FileExportMode type, List<SubscriptionDto> subscriptions) {
        this.type=type;
        this.setSubscriptionsData(subscriptions);
    }

    public SubscriptionsDetailsDto(FileExportExtension extension, List<SubscriptionDto> subscriptions) {
        this.extension=extension;
        this.setSubscriptionsData(subscriptions);
    }

    private void setSubscriptionsData(List<SubscriptionDto> subscriptions) {
        this.countFormat = subscriptions.stream().map(x-> x.fileFormat).distinct().collect(Collectors.toList()).size();
        this.countExtension = subscriptions.stream().map(x-> x.fileExtension).distinct().collect(Collectors.toList()).size();
        this.countType = subscriptions.stream().map(x-> x.exportMode).distinct().collect(Collectors.toList()).size();
        this.combinations = subscriptions;
    }
}
