package com.bnpp.regliss.facade.dto.record;

import com.bnpp.regliss.entity.Language;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.facade.dto.BreadcrumbDto;
import com.bnpp.regliss.facade.mapper.ExportToPdfMapperService;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ElementDetailExportDto {

    public String reference;
    public String userUid;
    public String userFullName;
    public String currentDate;
    public String language;
    public String elementType;
    public String importFileType;

    public RecordDto elementDetail;

    public ExportToPdfMapperService mapperService;

    public ElementDetailExportDto(RecordDto elementDetail, User user, BreadcrumbDto version, ExportToPdfMapperService mapperService) {
        this.reference = version.listReglissReference + "." + version.listName + " / Version " + version.versionNumber;
        this.userUid = user.getUsername();
        this.userFullName = user.getFirstName() + user.getLastName();
        this.currentDate = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        this.language =Language.FR.name();
        this.elementDetail = elementDetail;
        this.mapperService = mapperService;
        this.elementType = this.mapperService.getTypeLabelByType(elementDetail.type);
        this.importFileType = this.mapperService.getImportFileLabelByType(version.importFileType);

    }
    public ElementDetailExportDto(RecordDto elementDetail, ExportToPdfMapperService mapperService) {
        BreadcrumbDto breadcrumbDto = elementDetail.generalFields.breadcrumbDto;
        this.reference = breadcrumbDto.listReglissReference + "." + breadcrumbDto.listName;
        this.currentDate = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        this.language = Language.FR.name();
        this.elementDetail = elementDetail;
        this.mapperService = mapperService;
        this.elementType = this.mapperService.getTypeLabelByType(elementDetail.type);
        this.importFileType = this.mapperService.getImportFileLabelByType(breadcrumbDto.importFileType);
    }

    public String getReference() { return reference; }

    public String getUserUid() { return userUid; }

    public String getUserFullName() { return userFullName; }

    public String getCurrentDate() { return currentDate; }

    public String getLanguage() { return language; }

    public RecordDto getElementDetail() { return elementDetail; }

    public String getElementType() {  return elementType;  }

    public String getImportFileType() {  return importFileType;  }

    public ExportToPdfMapperService getMapperService() { return mapperService; }
}


