package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.batch.service.BatchManagementService;
import com.bnpp.regliss.batch.service.NodeDetailsSupplier;
import com.bnpp.regliss.config.thread.autocloseable.CloseResourcesAfter;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.ImportedFile;
import com.bnpp.regliss.entity.ValidationError;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.facade.dto.version.ImportLineErrorsDto;
import com.bnpp.regliss.importer.manual.*;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.ImportedFileRepository;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.service.VersionService;
import com.bnpp.regliss.service.lastModificationDate.LastModificationDateCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.OptimisticLockException;
import java.io.File;
import java.util.*;

import static com.bnpp.regliss.entity.UploadedFileExtension.XLSX;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Service
@ReglissBatchProfile
@Slf4j
public class ManualImportService {

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ObjectFactory<ManualImportPagedJob> pagedJobFactory;

    @Autowired
    private ManualImportBulkJob xlsxBulkJob;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private VersionService versionService;

    @Autowired
    private ImportedFileService importedFileService;

    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;
    @Autowired
    private BatchManagementService batchManagementService;

    @Autowired
    private ImportedFileRepository repo;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UnblockImportService unblockImportService;

    @Autowired
    private ManualImportFileRepository fileRepository;

    @Autowired
    private LastModificationDateCalculationService lastModificationDateCalculationService;


    @Value("${version.delete.timeout.seconds:500}")
    private int versionDeletetimeoutSeconds;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ImportedFile> getNextUploadedFileForManualProcessingImportInTx(ImportFileType importFileType) {
        List<ImportedFile> candidates = repo.findUnprocessedUploadedFiles(importFileType, ImportedFile.UploadType.MANUAL,
                ImportedFile.UploadStatus.UPLOAD_OK,    PageRequest.of(0, 5));
        for (ImportedFile candidate : candidates) {
            try {
                Map<String, Object> mp = new HashMap<>();
                mp.put("javax.persistence.lock.timeout", Session.LockRequest.PESSIMISTIC_NO_WAIT);
                entityManager.lock(candidate, LockModeType.PESSIMISTIC_READ, mp);
                entityManager.refresh(candidate);

                if (candidate.getProcessNode() == null) {
                    if (batchManagementService.attemptLockList(candidate.getVersion().getList())) {
                        log.debug("list locked {} ", candidate.getVersion().getList().getId());
                        candidate.setProcessNode(nodeDetailsSupplier.getNodeId());
                        return Optional.of(candidate);
                    } else {
                        log.debug("unable to lock list {} ", candidate.getVersion().getList().getId());
                    }
                }
            } catch (LockTimeoutException e) {
                log.debug("lock contended for uploaded file {}", candidate.getId());
            } catch (OptimisticLockException e) {
                log.debug("Could not lock batch row {}, an other node is processing it", candidate.getId());
            }
        }
        return Optional.empty();
    }

    @Async("manualImportExecutor")
    public void processUploadedFile(ImportedFile importedFile) {
        requestContext.setCurrentUser(referenceService.getSysUser());
        Long batchExecutionId = null;
        Long versionId = importedFile.getVersion().getId();
        Version version = importedFile.getVersion();
        try {
            log.debug("Start processing manual imported file:{}", importedFile.getId());
            batchExecutionId = batchManagementService.createNewSemiAutoBatchExecutionInTx(importedFile, version);
            File tempFile = fileRepository.getFileInUploadFolderByName(importedFile.getFileName());

            if (tempFile.exists() && tempFile.isFile()) {
                List<ImportLineErrorsDto> errors = new ArrayList<>();
                try {
                    importFile(importedFile, tempFile, errors, versionId, batchExecutionId);
                } catch (ReglissException e) {
                    log.error("{} : {}", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "-");
                    unblockImportService.tryRevertSemiAutoVersionInTx(versionId, versionDeletetimeoutSeconds * 1000);
                    importedFileService.setImportedKOInTx(importedFile.getId(), errors, e);
                } catch (Exception e) {
                    log.error("{} : {}", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "-");
                    unblockImportService.tryRevertSemiAutoVersionInTx(versionId, versionDeletetimeoutSeconds * 1000);
                    importedFileService.setImportedKOInTx(importedFile.getId(), errors, new ReglissException(e));
                }
            } else {
                log.error("Import file not found {}", importedFile.getFileName());
                importedFileService.setImportedKOInTx(importedFile.getId(), singletonList(new ImportLineErrorsDto(-1, singletonList("File Not Found"))));
            }
        } finally {
            unblockImportService.finishImportExecutionByRequesterUser(batchExecutionId);
            unblockImportService.unblockSemiAutoImportRegardlessTheStatusInTx(batchExecutionId, importedFile.getId());
        }
    }

    @CloseResourcesAfter
    private void importFile(ImportedFile importedFileInfo, File tempFile, List<ImportLineErrorsDto> errors, Long versionId, long batchExecutionId)  {
        if (importedFileInfo.getExtension() != XLSX) {
            ManualImportPagedJob csvJob = pagedJobFactory.getObject();
            errors.addAll(translateErrors(csvJob.launchJob(versionId, tempFile, importedFileInfo, batchExecutionId)));
        } else {
            errors.addAll(translateErrors(xlsxBulkJob.allInMemoryImplem(versionId, tempFile, importedFileInfo, batchExecutionId)));
        }
        if (errors.isEmpty()) {
            batchManagementService.updateBatchExecutionCompletedProgress(batchExecutionId);
            importedFileService.updateImportedFileOk(importedFileInfo.getId(), tempFile, importedFileInfo.getOriginalFileName());
            Version version = importedFileInfo.getVersion();
            lastModificationDateCalculationService.updateLastModificationDate(version.getId(),version.getListId());
        } else {
            // TO DO send error mail ? for manual import
            importedFileService.setImportedKOInTx(importedFileInfo.getId(), errors);
            versionService.revertVersionInTx(versionId);
        }
    }

    private List<ImportLineErrorsDto> translateErrors(List<LineImportErrors> errors) {
        return errors.stream().map(this::translate).collect(toList());
    }

    private String translateError(ValidationError error) {
        return messageSource.getMessage("error." + error.getErrorCode(), error.getParameters(), Locale.ENGLISH);
    }

    private ImportLineErrorsDto translate(LineImportErrors lineErrors) {
        return new ImportLineErrorsDto(lineErrors.getLineNumber(),
                lineErrors.getErrors().stream().map(this::translateError).collect(toList()));
    }
}

