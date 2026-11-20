package com.bnpp.regliss.scheduler;


import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.helper.TempFileUtils;
import com.bnpp.regliss.importer.UnicodeBOMInputStream;
import com.bnpp.regliss.vo.AutomaticImportFeedBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component
@Slf4j
public class AutomaticImportFileRepository {

    @Autowired
    private TempFileUtils tempFileUtils;

    @Value("${automatic.import.IN.directory}")
    private File inputDirectory;

    @Value("${automatic.import.IGNORE.directory}")
    private File ignoreDirectory;

    @Value("${automatic.import.ERROR.directory}")
    private File errorDirectory;

    @Autowired
    private FileRepository fileRepo;

    @PostConstruct
    public void prepareFolders() {
        fileRepo.makeFolder(inputDirectory);
        fileRepo.makeFolder(ignoreDirectory);
        fileRepo.makeFolder(errorDirectory);
    }

    public List<String> getFilesInInputFolder() {
        List<File> inputFiles = fileRepo.getUnlockedFilesInFolder(inputDirectory);
        return inputFiles.stream().map(File::getName).collect(toList());
    }

    public void deleteFiles(AutomaticImportFeedBase feed) { feed.getAllFileNames().forEach(this::deleteInputFile); }

    public void moveToIgnoreDirectory(AutomaticImportFeedBase feed) {
        for (String fileName : feed.getAllFileNames()) {
            fileRepo.moveFilesToDirectory(fileRepo.getFileByName(inputDirectory, fileName), ignoreDirectory);
        }
    }

    public void moveToErrorDirectory(String fileName) {
        fileRepo.moveFilesToDirectory(fileRepo.getFileByName(inputDirectory, fileName), errorDirectory);
    }

    public void moveToErrorDirectory(AutomaticImportFeedBase feed) {
        if(feed.isAuto())
            feed.getAllFileNames().forEach(this::moveToErrorDirectory);
    }

    public File createInputFileByName(String fileName) { return fileRepo.createFileByName(inputDirectory, fileName); }


    public Path getPathForFilename(String fileName) { return Paths.get(inputDirectory.toURI()).resolve(fileName); }

    public File getInputDirectory() { return inputDirectory; }

    public List<File> getDirectoriesWithFreeSpaceBellowThreshold(long minSpaceThresholdMB) {
        long minSpaceThresholdBytes = minSpaceThresholdMB * 1024 * 1024;
        return Stream.of(inputDirectory, ignoreDirectory, errorDirectory, tempFileUtils.getNodeBaseTempFolder())
                .filter(d -> d.getFreeSpace() < minSpaceThresholdBytes)
                .collect(Collectors.toList());
    }

    public void deleteInputFile(String fileName) { fileRepo.deleteFile(inputDirectory, fileName); }

    public int getFileAgeInSeconds(String fileName) {
        long lastModifiedTime = 0;
        try {
            lastModifiedTime = Files.getLastModifiedTime(Paths.get(inputDirectory.getAbsolutePath(),fileName)).toMillis(); //NOSONAR false positive, the path doesn't come from user input
        } catch (IOException e) {
            throw new ReglissException(e,"Could not checklast update time for file "+fileName);
        }
        return (int) ((System.currentTimeMillis() - lastModifiedTime) / 1000);
    }

    public File getInputFileByName(String fileName) { return fileRepo.getFileByName(inputDirectory, fileName); }

    public String computeMD5HashOfInputFile(String fileName) {
        return fileRepo.computeMD5HashOfFile(inputDirectory, fileName);
    }

    public String readInputFileFully(String fileName) { return fileRepo.readFully(inputDirectory, fileName); }

    public void createInputFileWithContent(String fileName, InputStream is) throws IOException {
        File targetFile = createInputFileByName(fileName);
        try  {
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to create file with name {} in IN folder : {}", fileName, e.getMessage());
            throw e;
        }
    }

    public  Supplier<UnicodeBOMInputStream> openFromZipIfNecessary(String fileName){
        return fileRepo.openFromZipIfNecessary(inputDirectory, fileName, true);
    }

}

