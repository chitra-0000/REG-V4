package com.bnpp.regliss.facade.dto.record;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

public class CompareVersionsDto {
    public List<CompareVersionsLine>  addedRecordsBetweenVersions = new ArrayList<>();
    public List<CompareVersionsLine>  modifiedRecordsBetweenVersions = new ArrayList<>();
    public List<CompareVersionsLine>  deletedRecordsBetweenVersions = new ArrayList<>();
    public long totalNumberVersion1;
    public long totalNumberVersion2;
    public long versionNumber1;
    public long versionNumber2;
    public long versionId1;
    public long versionId2;
}

