package com.bnpp.regliss.facade.dto;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class VersionBreadcrumbDto {
    public Long idList;
    public String listName;
    public String shortName;
    public Long versionNumber;
    public String reference;

    public VersionBreadcrumbDto() {}
    public VersionBreadcrumbDto(ListVersionBreadcrumbDto version) {
        idList = version.getList().getId();
        listName = version.getList().getName();
        shortName = version.getList().getShortName();
        versionNumber = version.getVersionNumber();
        reference = version.getList().getReference();
    }
}


