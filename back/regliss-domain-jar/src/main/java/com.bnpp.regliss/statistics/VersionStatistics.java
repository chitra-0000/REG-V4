package com.bnpp.regliss.statistics;
import lombok.Getter;

@Getter
public class VersionStatistics {

    private  long createdRecords;
    private  long updatedRecords;
    private  long deletedRecords;
    private  long totalRecords;
    private long strongRecords;
    private long weakRecords;
    private VersionPersonStatistics versionPersonStatistics;

    public VersionStatistics(long createdRecords, long updatedRecords, long deletedRecords, long totalRecords, long strongRecords, long weakRecords,VersionPersonStatistics versionPersonStatistics) {
        this.createdRecords = createdRecords;
        this.updatedRecords = updatedRecords;
        this.deletedRecords = deletedRecords;
        this.totalRecords = totalRecords;
        this.strongRecords = strongRecords;
        this.weakRecords = weakRecords;
        this.versionPersonStatistics=versionPersonStatistics;
    }
}
