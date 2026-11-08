package com.bnpp.regliss.facade.dto.version;

import com.bnpp.regliss.entity.ImportedFile;

import java.util.ArrayList;
import java.util.List;

public class UploadedFileDto {

    public Long id;
    public ImportedFile.UploadStatus uploadStatus;
    public float importProgressPercent;
    public List<ImportLineErrorsDto> errors = new ArrayList<>();
    public String importError;
    public String userName;

    public UploadedFileDto() {
    }

    public UploadedFileDto(Long id) { this.id = id; }

    public UploadedFileDto(ImportedFile entity, String importErrorMessage, List<ImportLineErrorsDto> importLineErrorsDto,
                           Float importProgressPercent) {
        this(entity, importProgressPercent);
        errors = importLineErrorsDto;
        importError = importErrorMessage;
    }

    public UploadedFileDto(ImportedFile entity, String importErrorMessage, Float importProgressPercent) {
        this(entity, importProgressPercent);
        importError = importErrorMessage;
    }

    public UploadedFileDto(ImportedFile entity, Float importProgressPercent) {
        this.id = entity.getId();
        this.uploadStatus = entity.getStatus();
        this.userName = entity.getUserName();
        this.importProgressPercent = importProgressPercent;
    }
}

