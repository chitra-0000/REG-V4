package com.bnpp.regliss.facade;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.facade.dto.list.MappingColumnDto;
import com.bnpp.regliss.facade.dto.list.MappingFileDto;
import com.bnpp.regliss.facade.dto.version.ImportErrorDto;
import com.bnpp.regliss.facade.dto.version.UploadedFileDto;
import com.bnpp.regliss.facade.mapper.MappingFileMapper;
import com.bnpp.regliss.importer.manual.*;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.MappingFileRepository;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.repository.batch.BatchJobExecutionRepository;
import com.bnpp.regliss.scheduler.UTF8InvalidFileNotifier;
import com.bnpp.regliss.service.ReglissListAuditService;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.service.VersionLockService;
import com.bnpp.regliss.service.permission.CurrentVersionPermissions;
import com.bnpp.regliss.utils.JacksonUtil;
import com.bnpp.regliss.vo.ImportLineErrorReport;
import com.bnpp.regliss.vo.ListIdAndVersionId;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static com.bnpp.regliss.entity.ImportedFile.UploadStatus.IMPORT_KO;
import static com.bnpp.regliss.entity.ImportedFile.UploadStatus.UPLOAD_OK;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.isBlank;

@Facade
@ReglissWebProfile
@Slf4j
public class ManualImportFacade {

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private MappingFileRepository mappingFileRepo;

    @Autowired
    private MappingFileMapper mappingFileMapper;

    @Autowired
    private ReglissListAuditService auditService;

    @Autowired
    private MappingFileParser mappingFileParser;

    @Autowired
    private ImportedFileService importedFileService;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private CurrentVersionPermissions versionPermissions;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private VersionLockService versionLockService;

    @Autowired
    private UTF8InvalidFileNotifier utf8InvalidFileNotifier;

    @Autowired
    private ManualImportFileRepository fileRepository;

    @Autowired
    private BatchJobExecutionRepository batchJobExecutionRepo;

    @CloseResourcesAfter
    public UploadedFileDto uploadManualImportFile(long versionId, String fileName, InputStream inputStream) {
        log.debug("entered facade ", fileName);
        Version version = versionRepo.getExactlyOne(versionId);
        if (!versionPermissions.getActions(version).upload) {
            throw new ReglissException(ErrorCode.NOT_AUTHORIZED);
        }


        importedFileService.getLastUploadedFileForVersion(version)
                .filter(uploadedFile -> uploadedFile.getStatus() != IMPORT_KO)
                .ifPresent(uploadedFile -> {throw new ReglissException(ErrorCode.MANUAL_IMPORT_ALREADY_STARTED_FOR_VERSION);});

        ImportedFile uploadedFileInfo = uploadFileOnDisk(version, fileName, inputStream);

        if(uploadedFileInfo == null) {
            return new UploadedFileDto();
        }

        getUploadValidationError(version, uploadedFileInfo)
            .ifPresent(errorCode -> uploadedFileInfo.setErrorMessage(translateError(errorCode, null)));

        versionLockService.lockSemiAutoVersionInTx(version.getId());
        // the file will be processed by a process started from ManualImportScheduler
        log.debug("Created uploaded file with id {} for later processing ", uploadedFileInfo.getId());
        return new UploadedFileDto(uploadedFileInfo, uploadedFileInfo.getErrorMessageOpt().orElse(null), 0.0f);
    }

    public UploadedFileDto getManualImportStatus(long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        ImportedFile lastUploadedFile = importedFileService.getLastUploadedFileForVersion(version)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));

        Float importProgressPercent = batchJobExecutionRepo.findTop1ByVersionOrderByStartDateDesc(version)
                .map(batchJobExecution -> getPercent(batchJobExecution.getPercent(), lastUploadedFile.getStatus()))
                .orElse(0.0f);


        if(version.getImportFileType().isManualProcessingFormat() || version.getImportFileType().isCRBListFileType() ||
                version.getImportFileType().isCoreFormat()){
            Optional<ImportErrorDto> importErrorDtoOpt = lastUploadedFile.getErrorMessageOpt()
                    .map(errorMessage -> JacksonUtil.getDeserializedValue(errorMessage, ImportErrorDto.class))
                    .orElse(Optional.empty());
            return importErrorDtoOpt
                    .map(importErrorDto -> new UploadedFileDto(lastUploadedFile, getTranslatedImportErrorMessage(importErrorDto),
                            importErrorDto.importLineErrorsDtos, importProgressPercent))
                    .orElse(new UploadedFileDto(lastUploadedFile, importProgressPercent));
        } else {
            List<ImportLineErrorReport> listImportErrorDto = lastUploadedFile.getErrorMessageOpt()
                    .map(errorMessage -> JacksonUtil.getTypedDeserializedValue(errorMessage, List.class, ImportLineErrorReport.class))
                    .map(Optional::get)
                    .orElse(new ArrayList<>());
            if(listImportErrorDto.isEmpty()){
                return new UploadedFileDto(lastUploadedFile, importProgressPercent);
            }else{
                String importErrorMessage = listImportErrorDto.stream()
                        .map(err -> err.toastMessage)
                        .distinct()
                        .collect(joining(","));
                return new UploadedFileDto(lastUploadedFile, importErrorMessage, importProgressPercent);
            }
        }
    }

    public MappingFileDto uploadCsvMappingFile(Long listId, String fileName, InputStream inputStream) {
        if (!"csv".equalsIgnoreCase(FilenameUtils.getExtension(fileName))) {
            throw new ReglissException(ErrorCode.MAPPING_FILE_ILLEGAL_EXTENSION);
        }

        Map<String, AuditedFieldCode> previousMappings = listRepo.getExactlyOne(listId)
                .getActiveMappingFile()
                .map(MappingFile::getColumns)
                .orElse(emptyList())
                .stream()
                .collect(toMap(MappingColumn::getColumnLabel, MappingColumn::getFieldCode, (label, identicalLabel) -> label));


        MappingFileDto mappingFileDto = new MappingFileDto();
        mappingFileDto.fileName = fileName;

        try {
            List<ParsedLine> rows = CsvParser.readCsvFull(inputStream);
            if (rows.isEmpty()) {
                throw new ReglissException(ErrorCode.MAPPING_FILE_EMPTY);
            }
            for (String columnHeader : rows.get(0).cells) {
                AuditedFieldCode afc = previousMappings.get(columnHeader);
                if (afc == null) {
                    afc = enumByExactName(columnHeader);
                }
                mappingFileDto.mappingColumns.add(new MappingColumnDto(columnHeader, afc));
            }
        } catch (IOException e) {
            throw new ReglissException(e, ErrorCode.MAPPING_FILE_PARSE_ERROR);
        }
        return mappingFileDto;
    }

    public void setCsvMappingFile(Long listId, MappingFileDto mappingFileDto) {
        ReglissList newList = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(newList);
        newList.setAllowManualUpload(true);
        newList.setImportFileType(ImportFileType.MANUAL_PROCESSING);
        auditService.auditUpdate(oldList, newList);

        if (isBlank(mappingFileDto.fileName)) {
            return;
        }

        MappingFile newMappingFile = mappingFileMapper.toEntity(mappingFileDto);
        mappingFileParser.parseAndValidateFile(newMappingFile);
        setMapping(newList, newMappingFile);
    }

    private ImportedFile uploadFileOnDisk(Version version, String fileName, InputStream inputStream) {
        log.debug("upload file on disk method ", fileName, version.getId());
        ImportedFile uploadedFileInfo = null;
        File tempFile = null;
        try {
            tempFile = fileRepository.uploadFileOnDisk(fileName, inputStream, version);
            uploadedFileInfo = importedFileService.saveManualUploadedFileInTx(fileName, tempFile.getName(), version.getId(), UPLOAD_OK);
            log.debug("Semiauto uploaded file {} stored in shared folder {} for version {}; id:{}",
                    fileName, tempFile.getAbsolutePath(), version.getId(), uploadedFileInfo.getId());
        } finally {
            if (uploadedFileInfo == null && tempFile != null) {
                try {
                    log.info("Could not save file info to db for temp'file: {}. Deleting temp file.", tempFile.getAbsolutePath());
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    log.debug("Could not delete uploaded file:{}", tempFile.getAbsolutePath());
                }
            }
        }
        return uploadedFileInfo;
    }

    private Optional<ErrorCode> getUploadValidationError(Version version, ImportedFile uploadedFileInfo) {
        if(version.getImportFileType() == ImportFileType.MANUAL_PROCESSING) {
            Path uploadedFilePath = fileRepository.getPathForFilename(uploadedFileInfo.getFileName());
            Optional<String> utf8ErrorMessage = utf8InvalidFileNotifier.getUtf8ErrorMessage(uploadedFilePath);
            if(utf8ErrorMessage.isPresent()) {
                utf8InvalidFileNotifier.sendMailIfNotValidUTF8File(uploadedFilePath, utf8ErrorMessage.get(),
                        new ListIdAndVersionId(version.getList().getId(), version.getId()));
                return Optional.of(ErrorCode.WRONG_ENCODING_FILE);
            }
        }
        return Optional.empty();
    }

    private String getTranslatedImportErrorMessage(ImportErrorDto importErrorDto) {
        String translatedMessage = null;
        if (importErrorDto.importError != null) {
            ErrorCode errorCode = importErrorDto.importError.getErrorCode();
            translatedMessage = translateError(errorCode, importErrorDto.importError.getParameters());
        }
        return translatedMessage;
    }

    private String translateError(ErrorCode errorCode, String[] parameters) {
        return messageSource.getMessage("error." + errorCode, parameters, requestContext.getLocale());
    }

    private AuditedFieldCode enumByExactName(String columnHeader) {
        // if column header == name of enum, directly match that AFC
        return Stream.of(AuditedFieldCode.values())
                .filter(v -> v.name().equalsIgnoreCase(columnHeader))
                .findFirst()
                .orElse(null);
    }

    private void setMapping(ReglissList newList, MappingFile newMappingFile) {
        Optional<MappingFile> previousMappingFileOpt = newList.getMappingFiles().stream()
                .max(Comparator.comparingLong(Entity::getId));
        Optional<MappingFile> previousActiveMappingFileOpt = newList.getActiveMappingFile();
        mappingFileRepo.save(newMappingFile);
        newList.addMappingFile(newMappingFile);

        if (!previousActiveMappingFileOpt.isPresent()) {
            auditService.auditCreateMappingFile(newMappingFile);
        } else {
            if (previousActiveMappingFileOpt.isPresent()) {
                previousActiveMappingFileOpt.get().setDeleted(true);
                auditService.auditUpdateMappingFile(previousActiveMappingFileOpt.get(), newMappingFile);
            } else {
                auditService.auditUpdateMappingFile(previousMappingFileOpt.get(), newMappingFile);
            }
        }
    }

    private Float getPercent(Float percent, ImportedFile.UploadStatus status){
        if(status == UPLOAD_OK)
            return 0.0f;
        else
            return percent;
    }

}
