package com.bnpp.regliss.facade.dto;

import com.bnpp.regliss.entity.ImportFileType;

public class ListBreadcrumbDto {
    public String name;
    public String shortName;
    public String reference;
    public ImportFileType importFileType;
    public boolean canCreateVersion;
    public boolean canDownloadImport;
    public boolean canDownloadExport;
}

