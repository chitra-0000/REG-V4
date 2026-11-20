package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.batch.service.NodeDetailsSupplier;
import com.bnpp.regliss.entity.ImportedFile;
import com.bnpp.regliss.entity.ImportedFile.UploadStatus;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.facade.dto.version.ImportErrorDto;
import com.bnpp.regliss.facade.dto.version.ImportLineErrorsDto;
import com.bnpp.regliss.helper.FileUtils;
import com.bnpp.regliss.repository.ImportedFileRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.service.IAutocloseableResources;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.utils.BlobService;
import com.bnpp.regliss.utils.FeedIdHelper;
import com.bnpp.regliss.utils.JacksonUtil;
import com.bnpp.regliss.vo.ImportLineErrorReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ImportedFileService {

    @Autowired
    private ImportedFileRepository repo;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private BlobService blobService;

    @Autowired
    private IAutocloseableResources fileHelper;

    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;

    @Autowired
    private FileUtils fileUtils;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportedFile saveManualUploadedFileInTx(File inputFile, String originalFilename, Long versionId) {
        Version version = versionRepository.getExactlyOne(versionId);
        ImportedFile importedFile = new ImportedFile();
        importedFile.setOriginalFileName(originalFilename);
        importedFile.setFileName(originalFilename);
        importedFile.setUserName(requestContext.getUser().getUsername());
        importedFile.setProcessNode(nodeDetailsSupplier.getNodeId());
        importedFile.setUploadDate(LocalDateTime.now());
        version.addImportedFile(importedFile);
        importedFile.setList(version.getList());
        importedFile.setUploadType(ImportedFile.UploadType.MANUAL);
        return repo.save(importedFile);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveImportedFileInTx(File inputFile, String originalFilename, Long versionId, Long uploadedFileId) {
        Version version = versionRepository.getExactlyOne(versionId);
        ImportedFile importedFile = null;
        if(uploadedFileId == null){
            importedFile = new ImportedFile();
            importedFile.setUploadType(ImportedFile.UploadType.AUTO);
            importedFile.setOriginalFileName(originalFilename);
            importedFile.setFileName(originalFilename);
            importedFile.setList(version.getList());
            importedFile.setUserName(User.SYS_USERNAME);
            importedFile.setProcessNode(nodeDetailsSupplier.getNodeId());
            importedFile.setUploadDate(LocalDateTime.now());
        }else{
            importedFile = repo.getExactlyOne(uploadedFileId);
        }
        importedFile.setStatus(UploadStatus.IMPORTED_OK);
        version.addImportedFile(importedFile);
        version.setFileName(originalFilename);
        uploadFileContent(importedFile, inputFile, originalFilename);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateImportedFileOk(Long uploadedFileId, File tempFile, String originalFileName) {
        log.debug("import ok for uploaded file {} ", uploadedFileId);
        ImportedFile importedFile = repo.getExactlyOne(uploadedFileId);
        importedFile.setStatus(UploadStatus.IMPORTED_OK);
        uploadFileContent(importedFile, tempFile, originalFileName);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setProcessingInTX(long uploadedFileId) {
        log.debug("start processing for uploaded file {} ", uploadedFileId);
        repo.getExactlyOne(uploadedFileId).setStatus(ImportedFile.UploadStatus.IMPORTING);
    }

    public Optional<ImportedFile> getLastUploadedFileForVersion(Version version) {
        return repo.findTop1ByVersionOrderByUploadDateDesc(version);
    }

    public ImportedFile getByFileId(long fileId) {
        return repo.getExactlyOne(fileId);
    }

    public void setImportedKOInTx(Long uploadedFileId, ImportErrorDto importErrorDto) {
        setImportedKOInTx(uploadedFileId, importErrorDto, null);
    }

    public void setImportedKOInTx(Long uploadedFileId, ImportErrorDto importErrorDto, File tempFile) {
        setImportedKOInTx(uploadedFileId, importErrorDto);
    }

    public void setSemiAutoImportedKOInTx(Long uploadedFileId, List<ImportLineErrorsDto> lineErrors) {
        ImportedFile importedFile = repo.getExactlyOne(uploadedFileId);
        importedFile.setStatus(ImportedFile.UploadStatus.IMPORT_KO);
        JacksonUtil.getSerializedValue(lineErrors)
                .ifPresent(importedFile::setErrorMessage);
        repo.save(importedFile);
    }

    public void updateSemiAutoImportedOKInTx(Long uploadedFileId) {
        ImportedFile importedFile = repo.getExactlyOne(uploadedFileId);
        importedFile.setStatus(UploadStatus.IMPORTED_OK);
        repo.save(importedFile);
    }

    private void uploadFileContent(ImportedFile importedFile, File inputFile, String originalFilename) {
        File tempFile = fileHelper.createTempFile("import", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {
            zos.putNextEntry(new ZipEntry(originalFilename));
            Files.copy(inputFile.toPath(), zos);
            zos.closeEntry();
        } catch (IOException e) {
            throw new ReglissException(e);
        }
        tryToStoreBlob(importedFile, tempFile);
    }

    private void tryToStoreBlob(ImportedFile importedFile, File inputFile) {
        try {
            importedFile.setFileContent(blobService.createBlob(inputFile));
        } catch (ReglissException e) {
            log.warn("Could not store imported file {} for version {} - {}",
                    inputFile.getAbsoluteFile(), importedFile.getId(),
                    e.getMessage());
        }
    }

    private void setImportedKOInTx(Long uploadedFileId, ImportErrorDto importErrorDto) {
        ImportedFile importedFile = repo.getExactlyOne(uploadedFileId);
        importedFile.setStatus(ImportedFile.UploadStatus.IMPORT_KO);
        JacksonUtil.getSerializedValue(importErrorDto)
                .ifPresent(importedFile::setErrorMessage);
        repo.save(importedFile);
    }

    public LocalDate getLatestWLImportOKDate() {
        return repo.latestWLImportOKOrderByUploadDate().stream().findFirst().map(FeedIdHelper::djDateFromWLFilename).orElse(null);
    }
}
