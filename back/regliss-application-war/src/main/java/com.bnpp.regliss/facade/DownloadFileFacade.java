package com.bnpp.regliss.facade;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.config.facade.FacadeLoggingInterceptor.NotLogged;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.ExportFile.GenerationStatus;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.AddModSupAuditTrailFileRepository;
import com.bnpp.regliss.repository.AddModSupExportFileRepository;
import com.bnpp.regliss.repository.GeneratedFileRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.utils.BlobService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Blob;

@Facade
@ReglissWebProfile
public class DownloadFileFacade {

    @Autowired
    private GeneratedFileRepository generatedFileRepo;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private AddModSupExportFileRepository addModSupExportFileRepo;

    @Autowired
    private AddModSupAuditTrailFileRepository addModSupAuditTrailFileRepository;

    @Autowired
    private BlobService blobService;

    @NotLogged
    public void downloadGeneratedFile(Long id, OutputStream outputStream) {
        GeneratedFile generatedFile = generatedFileRepo.getExactlyOne(id);
        downloadUnzippedFile(generatedFile.getFileContent(), outputStream);
    }

    @NotLogged
    public void downloadImportFile(Long versionId, OutputStream outputStream) throws IOException {
        Version version = versionRepo.getExactlyOne(versionId);
        ImportedFile importedFileOk = version.getImportedFileOk()
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        IOUtils.copy(blobService.getBlob(importedFileOk.getFileContent()), outputStream);
    }

    public String getImportedFileName(Long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        return version.getImportedFileOk()
                .map(ImportedFile::getOriginalFileName)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
    }

    @NotLogged
    public void downloadAddModSupFile(long addModSupFileId, OutputStream outputStream) {
        AddModSupExportFile addModSupFile = addModSupExportFileRepo.getExactlyOne(addModSupFileId);
        if (addModSupFile.getGenerationStatus() != GenerationStatus.OK) {
            throw new ReglissException(ErrorCode.ADD_MOD_SUP_NOT_GENERATED);
        }
        downloadUnzippedFile(addModSupFile.getFileContent(), outputStream);
    }

    @NotLogged
    public void downloadAddModSupAuditTrailFile(long addModSupFileId, OutputStream outputStream) {
        AddModSupAuditTrailFile addModSupAuditTrailFile = addModSupAuditTrailFileRepository.getExactlyOne(addModSupFileId);
        if (addModSupAuditTrailFile.getGenerationStatus() != GenerationStatus.OK) {
            throw new ReglissException(ErrorCode.ADD_MOD_SUP_NOT_GENERATED);
        }
        downloadUnzippedFile(addModSupAuditTrailFile.getFileContent(), outputStream);
    }

    private void downloadUnzippedFile(Blob fileContentBlob, OutputStream outputStream) {
        blobService.unzipBlob(fileContentBlob, outputStream);
    }

}

