package com.bnpp.regliss.facade.dto.list;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

public class DJExportFilterDto {
    public Long id;
    public String filterName;
    public String filterTag;
    public String comment;
    public FileExportMode type;
    public Set<FileExportExtension> extension;
    public boolean isActive;
    public boolean generateExportFiles;
}

