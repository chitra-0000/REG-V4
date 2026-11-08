package com.bnpp.regliss.facade.dto.version;

import com.bnpp.regliss.entity.ExportFile.GenerationStatus;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.entity.referential.FileExportMode;

import java.time.LocalDateTime;

public class GeneratedFileDto{

    public long id;
    public FileExportExtension extension;
    public FileExportFormat format;
    public FileExportMode exportMode;
    public String fileName;
    public String fileNameWithExtension;
    public boolean hasFileContent;
    public GenerationStatus status;
    public String errorMessage;
    public LocalDateTime generationDate;
    public String filterName;
    public Long versionId;

    public GeneratedFileDto(GeneratedFile entity) {
        id = entity.getId();
        fileName = entity.getFileName();
        fileNameWithExtension = entity.getFullName();
        extension = entity.getGeneratedFileFormat().getFileExtension();
        exportMode = entity.getGeneratedFileFormat().getFileExportMode();
        format = entity.getGeneratedFileFormat().getFileFormat();
        status = entity.getGenerationStatus();
        errorMessage = entity.getErrorMessage();
        generationDate = entity.getGenerationDate();
        hasFileContent = entity.getFileContent() != null;
        if (entity.getGeneratedFileFormat().getFilter() != null) {
            filterName = entity.getGeneratedFileFormat().getFilter().getFilterName();
        }
        versionId = entity.getVersionOpt().map(Version::getId).orElse(null);
    }
}

