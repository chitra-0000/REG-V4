package com.bnpp.regliss.facade.dto.version;

// [MISSING FROM SCREENSHOTS: lines 3-8 of the original file were not visible in the photos]

public class VersionDtoForm {

    public Long id;
    public Long versionNumber;
    public VersionStatus status;
    public String legalBase;
    public LocalDate startDate;
    public String commentFr;
    public String commentEn;
    public String returnComment;
    public boolean wasAutoUploaded;
    public boolean importInProgress;
    public boolean isTemporary;

    public VersionDtoForm() {
    }

    public VersionDtoForm(Long id) { this.id = id; }

}

