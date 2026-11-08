package com.bnpp.regliss.facade.dto.list;

// [MISSING FROM SCREENSHOTS: lines 3-8 of the original file were not visible in the photos]

public class VersionForVersionPageDto {

    public Long id;
    public LocalDateTime publicationDate;
    public LocalDate startDate;
    @Getter public Long versionNumber;
    public VersionStatus status;
    public boolean isAutoImport;
    public Long created;
    public Long updated;
    public Long deleted;
    public Long minorUpdates;
    public Long recordNumber;
    public boolean hasMinorUpdates;
}

