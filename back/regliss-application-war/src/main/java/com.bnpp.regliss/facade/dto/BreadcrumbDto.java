package com.bnpp.regliss.facade.dto;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class BreadcrumbDto {

    public Long idList;
    public String listName;
    public ImportFileType importFileType;
    public String listReglissReference;
    public Long versionNumber;

    public BreadcrumbDto() {
    }
    public BreadcrumbDto(Version version) {
        idList = version.getList().getId();
        listName = version.getList().getShortName();
        importFileType = version.getImportFileType();
        listReglissReference = version.getList().getReference();
        versionNumber = version.getVersionNumber();
    }
}

