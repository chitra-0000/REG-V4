package com.bnpp.regliss.facade.dto;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]


public class BatchJobExecutionDto {
    public long id;
    public String nodeId;
    public String hostName;
    public BatchJobType jobType;
    public String jobParams;
    public float percent;
    public LocalDateTime startDate;
    public LocalDateTime endDate;
    public boolean isJobAlive;
    public ExportFile.GenerationStatus status;
}

