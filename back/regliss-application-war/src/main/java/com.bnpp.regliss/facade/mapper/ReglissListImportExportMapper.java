package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.entity.ImportConfiguration;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.VersionStatus;
import com.bnpp.regliss.facade.dto.list.*;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.vo.MappingColumnVO;
import com.bnpp.regliss.vo.ListImportExportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ReglissWebProfile
public class ReglissListImportExportMapper {

    @Autowired
    private ReglissListRepository listRepository;

    @Autowired
    private MappingFileMapper mappingFileMapper;

    public ReglissListImportExportDto toReglissListImportExportDto(ListImportExportVO list, List<MappingColumnDto> mappingColumns,
                                                                   ReglissList reglissList){
        return toDto(list, mappingColumns, reglissList);

    }
    public ReglissListImportExportDto toReglissListImportWhenNoCsvFileExportDto(ReglissList reglissList) {
        ExportFormatDto exportFormatDto=new ExportFormatDto();
        exportFormatDto.genericFormat=reglissList.isGenericFormat();
        exportFormatDto.reglissFormat=reglissList.isReglissFormat();
        exportFormatDto.vigilanceFormat=reglissList.isVigilanceFormat();
        exportFormatDto.safewatchFormat=reglissList.isSafewatchFormat();
        exportFormatDto.fircosoftFormat=reglissList.isFircosoftFormat();
        ImportConfigurationDto importConfigurationDto=new ImportConfigurationDto();
        importConfigurationDto.importFileType=reglissList.getImportConfiguration().getImportFileType();
        importConfigurationDto.checksumReception=reglissList.getImportConfiguration().isChecksumReception();
        importConfigurationDto.allowIncrementalImport=reglissList.getImportConfiguration().isAllowIncrementalImport();
        importConfigurationDto.logReception=reglissList.getImportConfiguration().isLogReception();
        importConfigurationDto.confirmationRequired=reglissList.getImportConfiguration().isConfirmationRequired();
        importConfigurationDto.autoConfirmationThreshold=reglissList.getImportConfiguration().getAutoConfirmationThreshold();
        importConfigurationDto.autoConfirmation=reglissList.getImportConfiguration().getAutoConfirmationThreshold()!=null;
        importConfigurationDto.allowAutoUpload=reglissList.getImportConfiguration().isAllowAutoUpload();
        importConfigurationDto.allowManualUpload=reglissList.getImportConfiguration().isAllowManualUpload();
        importConfigurationDto.setHasVersions(reglissList.hasVersions());
// [MISSING FROM SCREENSHOTS: lines 49-51 of the original file were not visible in the photos]
        MappingFileDto mappingFileDto=new MappingFileDto();
        mappingFileDto.fileName=null;

        ReglissListImportExportDto dto=new ReglissListImportExportDto(exportFormatDto, importConfigurationDto, mappingFileDto);

        dto.id=reglissList.getId();
        return dto;
    }
    private ReglissListImportExportDto toDto(ListImportExportVO list, List<MappingColumnDto> mappingColumns, ReglissList reglissList){
        ExportFormatDto exportFormatDto=new ExportFormatDto();
        exportFormatDto.genericFormat=list.isGenericFormat();
        exportFormatDto.reglissFormat=list.isReglissFormat();
        exportFormatDto.vigilanceFormat=list.isVigilanceFormat();
        exportFormatDto.safewatchFormat=list.isSafewatchFormat();
        exportFormatDto.fircosoftFormat=list.isFircosoftFormat();

        ImportConfigurationDto importConfigurationDto=new ImportConfigurationDto();
        importConfigurationDto.fileName=list.getFileName();
        importConfigurationDto.importFileType=list.getImportFileType();
        importConfigurationDto.checksumReception=list.isChecksumReception();
        importConfigurationDto.allowIncrementalImport=list.isAllowIncrementalImport();
        importConfigurationDto.logReception=list.isLogReception();
        importConfigurationDto.confirmationRequired=list.isConfirmationRequired();
        importConfigurationDto.autoConfirmationThreshold=list.getAutoConfirmationThreshold();
        importConfigurationDto.autoConfirmation=list.getAutoConfirmationThreshold()!=null;
        importConfigurationDto.allowAutoUpload=list.isAllowAutoUpload();
        importConfigurationDto.allowManualUpload=list.isAllowManualUpload();
        importConfigurationDto.setHasVersions(reglissList.hasVersions());
        importConfigurationDto.setLastVersionPublishedOrDelivered(reglissList.getLastVersion().map(v -> v.getStatus() ==
                        VersionStatus.PUBLISHED_OK ||  v.getStatus() == VersionStatus.PUBLISHED_KO ||  v.getStatus() ==
                        VersionStatus.DELIVERED_OK ||  v.getStatus() == VersionStatus.DELIVERED_KO)
                .orElse(false));
        MappingFileDto mappingFileDto=new MappingFileDto();
        mappingFileDto.fileName=list.getMappingFile();

        ReglissListImportExportDto dto=new ReglissListImportExportDto(exportFormatDto, importConfigurationDto, mappingFileDto);

        dto.id=list.getId();
        dto.mappingFile.mappingColumns=mappingColumns;

        return dto;
    }

    public MappingColumnDto toReglissListImportExportMappingColumnDto(MappingColumnVO mappingColumns){
        MappingColumnDto mappingColumnDto=new MappingColumnDto();
        mappingColumnDto.columnLabel=mappingColumns.getColumnLabel();
        mappingColumnDto.recordFieldCode=mappingColumns.getFieldCode();
        return mappingColumnDto;
    }

    public ReglissList toEntity(ReglissList entity, ReglissListImportExportDto dto){
        entity.setGenericFormat(dto.exportFormat.genericFormat);
        entity.setFircosoftFormat(dto.exportFormat.fircosoftFormat);
        entity.setReglissFormat(dto.exportFormat.reglissFormat);
        entity.setSafewatchFormat(dto.exportFormat.safewatchFormat);
        entity.setVigilanceFormat(dto.exportFormat.vigilanceFormat);
        ImportConfiguration importConfiguration =new ImportConfiguration();
        importConfiguration.setChecksumReception(dto.importConfiguration.checksumReception)
                .setAllowIncrementalImport(dto.importConfiguration.allowIncrementalImport)
                .setLogReception(dto.importConfiguration.logReception)
                .setImportFileType(dto.importConfiguration.importFileType)
                .setFileNamePrefix(dto.importConfiguration.fileName)
                .setFileNamePrefix(importConfiguration.getFileNamePrefix())
                .setConfirmationRequired(dto.importConfiguration.confirmationRequired)
                .setAutoConfirmationThreshold(dto.importConfiguration.autoConfirmationThreshold)
                .setAllowAutoUpload(dto.importConfiguration.allowAutoUpload)
                .setAllowManualUpload(dto.importConfiguration.allowManualUpload);
        entity.setImportConfiguration(importConfiguration);
        return entity;
    }

}
