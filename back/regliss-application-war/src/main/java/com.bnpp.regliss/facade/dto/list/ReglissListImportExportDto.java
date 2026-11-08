package com.bnpp.regliss.facade.dto.list;

import com.bnpp.regliss.entity.MappingFile;

public class ReglissListImportExportDto {

    public Long id;
    public ExportFormatDto exportFormat;
    public MappingFileDto mappingFile;
    public ImportConfigurationDto importConfiguration;

    public ReglissListImportExportDto(ExportFormatDto exportFormat, ImportConfigurationDto importConfigurationDto,
                                      MappingFileDto mappingFileDto) {
        this.exportFormat = exportFormat;
        this.importConfiguration=importConfigurationDto;
        this.mappingFile=mappingFileDto;
    }

    public ReglissListImportExportDto(Long id) { this.id = id; }

    public ReglissListImportExportDto(){}
}

