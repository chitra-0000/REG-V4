package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.importer.UnicodeBOMInputStream;
import com.bnpp.regliss.scheduler.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

@Component
@Slf4j
public class ManualImportFileRepository {

    @Value("${shared.file.folder}")
    private File baseSharedFolder;

    @Value("${shared.file.folder.import}")
    private String importSharedFolder;

    @Autowired
    private FileRepository fileRepo;

    private static final List<String> manualUploadAcceptedExtensions = asList("csv", "xls", "xlsx", "xml", "zip");

    private static final String CORE_FILE_NAME = "S_XML";

    @PostConstruct
    public void prepareFolders() { fileRepo.makeFolder(baseSharedFolder, FilenameUtils.getName(importSharedFolder)); }

    public Path getPathForFilename(String fileName) {
        return Paths.get(baseSharedFolder.toURI()).resolve(importSharedFolder).resolve(org.apache.tika.io.FilenameUtils.normalize(fileName));
    }

    public File uploadFileOnDisk(String fileName, InputStream inputStream, Version version) {
        log.debug("Start saving manual import uploaded file {} for version {}", fileName, version.getId());
        File tempFile = createFileInUploadFolder(fileName, version);
        log.debug("Temp file created {} for version {}", fileName, version.getId());
        try (OutputStream tempOutputStream = new FileOutputStream(tempFile)) {
            int length = IOUtils.copy(inputStream, tempOutputStream);
            if (length == 0) {
                throw new ReglissException(ReglissException.ErrorCode.IMPORT_MANUAL_EMPTY_FILE);
            }
            log.debug("Temp file copied");
        } catch (IOException e) {
            log.debug("Could not store uploaded file:{} - {}", tempFile.getAbsolutePath(), e.getMessage());
            throw new ReglissException(e, "Could not store uploaded data in temporary file: " + tempFile.getAbsolutePath());
        }
        return tempFile;
    }

    public File getFileInUploadFolderByName(String sharedFileName) {
        return fileRepo.getFileByName(getUploadFolder(), sharedFileName);
    }

    public void deleteManualImportedFileFromDisk(String sharedFileName) {
        fileRepo.deleteFile(getUploadFolder(), sharedFileName);
    }

    private File createFileInUploadFolder(String uploadedFileName, Version version) {
        String fileName = computeUniqueFilename(uploadedFileName, version);
        return fileRepo.createFileByName(getUploadFolder(), fileName);
    }

    public File getUploadFolder() { return new File(baseSharedFolder, FilenameUtils.getName(importSharedFolder)); }

    private String computeUniqueFilename(String uploadedFileName, Version version) {

            String fileExtension = getExtension(uploadedFileName);
            ReglissList reglissList = version.getList();
            String prefix = reglissList.getImportConfiguration().isAllowAutoUpload() ? reglissList.getImportConfiguration()
                    .getFileNamePrefix() : reglissList.getShortName();
            if(uploadedFileName.toUpperCase().startsWith(CORE_FILE_NAME)){
                String filename = uploadedFileName.substring(0,uploadedFileName.indexOf('.'));
                log.info("Entered semiAutoDj condition");
                return filename+"." + fileExtension;
            }
            return prefix
                    + "." + version.getVersionNumber()
                    + "." + fileExtension;
    }

    private String getExtension(String uploadedFileName) {
        String uploadedFileExtension = FilenameUtils.getExtension(uploadedFileName).toLowerCase();
        return manualUploadAcceptedExtensions.stream()
                .filter(uploadedFileExtension::equals)
                .findFirst()
                .orElse("imp");
    }

    public Supplier<UnicodeBOMInputStream> openFromZipIfNecessary(String fileName){
        return fileRepo.openFromZipIfNecessary(getUploadFolder(), fileName, false);
    }

}

