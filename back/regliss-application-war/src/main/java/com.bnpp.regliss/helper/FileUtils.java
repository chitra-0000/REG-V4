package com.bnpp.regliss.helper;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.config.thread.autocloseable.AutocloseableResourcesHelper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
public class FileUtils {

    @Autowired
    private AutocloseableResourcesHelper fileHelper;
// [MISSING FROM SCREENSHOTS: lines 25-29 of the original file were not visible in the photos]
            try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
                ZipEntry zipEntry = new ZipEntry(originalFileName);
                zipStream.putNextEntry(zipEntry);
                Files.copy(rawFile.toPath(), zipStream);
                zipStream.closeEntry();
            }
            return zipFile;
        } catch (IOException e) {
            throw new ReglissException(e, e.getMessage());
        }
    }

    public File getInputFile(MultipartFile multipartFile){
        try {
            File file = fileHelper.createTempFileForThisThread();
            multipartFile.transferTo(file);
            return file;
        } catch (IOException e) {
            throw new ReglissException(e, e.getMessage());
        }

    }
}

