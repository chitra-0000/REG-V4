package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.utils.DJFilenameHelper;
import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import static com.bnpp.regliss.entity.ImportFileType.*;
import static com.bnpp.regliss.vo.DJFileImportMode.INCREMENTAL;

@Embeddable
public class ImportConfiguration {

    @EqualityField
    @AuditedField(AuditedFieldCode.LIST_FILE_NAME)
    @Column(name = "FILE_NAME", length = 256)
    private String fileNamePrefix;

    @Getter
    @AuditedField(AuditedFieldCode.LIST_IMPORT_FILE_TYPE)
    @Column(name = "IMPORT_FILE_TYPE")
    @Enumerated(EnumType.STRING)
    private ImportFileType importFileType = UNKNOWN;

    @Getter
    @AuditedField(AuditedFieldCode.ALLOW_INCREMENTAL_IMPORT)
    @Column(name = "ALLOW_INCREMENTAL_IMPORT")
    private boolean allowIncrementalImport;

    @Getter
    @AuditedField(AuditedFieldCode.LIST_CHECKSUM_RECEPTION)
    @Column(name = "CHECKSUM_RECEPTION")
    private boolean checksumReception;

    @Getter
    @AuditedField(AuditedFieldCode.LIST_LOG_RECEPTION)
    @Column(name = "LOG_RECEPTION")
    private boolean logReception;

    @Getter
    @AuditedField(AuditedFieldCode.LIST_CONFIRMATION_REQUIRED)
    @Column(name = "CONFIRMATION_REQUIRED")
    private boolean confirmationRequired;

    @Getter
    @Column(name = "AUTO_CONFIRMATION_THRESHOLD")
    private Long autoConfirmationThreshold;

    @Getter
    @AuditedField(AuditedFieldCode.LIST_ALLOW_AUTO_UPLOAD)
    @Column(name = "ALLOW_AUTO_UPLOAD")
    private boolean allowAutoUpload;

    @Getter
    @AuditedField(AuditedFieldCode.LIST_ALLOW_MANUAL_UPLOAD)
    @Column(name = "ALLOW_MANUAL_UPLOAD")
    private boolean allowManualUpload;

    public ImportConfiguration() {}

    public ImportConfiguration(ImportConfiguration other) {
        this.fileNamePrefix = other.fileNamePrefix ;
        this.importFileType = other.importFileType;
        this.logReception = other.logReception;
        this.checksumReception = other.checksumReception;
        this.allowIncrementalImport = other.allowIncrementalImport;
        this.confirmationRequired = other.confirmationRequired;
        this.autoConfirmationThreshold = other.autoConfirmationThreshold;
        this.allowAutoUpload = other.allowAutoUpload;
        this.allowManualUpload = other.allowManualUpload;
    }

    public ImportConfiguration setFileNamePrefix(String fileName) {
        this.fileNamePrefix = fileName;
        return this;
    }

    public ImportConfiguration setImportFileType(ImportFileType importFileType) {
        this.importFileType = importFileType;
        return this;
    }

    public ImportConfiguration setAllowIncrementalImport(boolean allowIncrementalImport) {
        this.allowIncrementalImport = allowIncrementalImport;
        return this;
    }

    public ImportConfiguration setChecksumReception(boolean checksumReception) {
        this.checksumReception = checksumReception;
        return this;
    }

    public ImportConfiguration setLogReception(boolean logReception) {
        this.logReception = logReception;
        return this;
    }

    public ImportConfiguration setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
        return this;
    }

    public ImportConfiguration setAutoConfirmationThreshold(Long autoConfirmationThreshold) {
        this.autoConfirmationThreshold = autoConfirmationThreshold;
        return this;
    }

    public ImportConfiguration setAllowAutoUpload(boolean allowAutoUpload) {
        this.allowAutoUpload = allowAutoUpload;
        return this;
    }

    public ImportConfiguration setAllowManualUpload (boolean allowManualUpload) {
        this.allowManualUpload = allowManualUpload;
        return this;
    }

    public boolean isWatchlist() { return importFileType == DOW_JONES_WATCHLIST; }
    public boolean isWatchlistExtra() { return importFileType == DOW_JONES_WATCHLIST_EXTRA; }

    public String getFileNamePrefix() {
        if(importFileType == DOW_JONES_WATCHLIST || importFileType == DOW_JONES_AME) {
            return DJFilenameHelper.getWlAmeXmlPrefix(importFileType);
        }
        return fileNamePrefix;
    }

    public boolean allowIncrementalImport(){
        return getImportFileType()!=null &&
                getImportFileType().djFileImportModes().contains(INCREMENTAL)&&
                isAllowIncrementalImport();
    }
}
