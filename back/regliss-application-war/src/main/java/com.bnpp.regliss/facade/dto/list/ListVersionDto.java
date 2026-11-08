package com.bnpp.regliss.facade.dto.list;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

public class ListVersionDto {

    public Long id;
    public Long versionNumber;
    public String status;
    public LocalDateTime publicationDate;
    public LocalDate startDate;
    public String username;
    public boolean isLocked = false;
    public String lockingUser;
    public boolean isAutomatic;
    public VersionStatus statusValue;
    public boolean isReadOnly;

    public Long getVersionNumber() { return versionNumber; }
    public Long getId() { return id; }
}

