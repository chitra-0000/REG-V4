package com.bnpp.regliss.config.thread.autocloseable;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.helper.TempFileUtils;
import com.bnpp.regliss.service.IAutocloseableResources;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AutocloseableResourcesHelper implements IAutocloseableResources {

    @Autowired
    private CloseableThreadResources resourcesToClose;

    @Autowired
    private TempFileUtils tempFileUtils;

    @Override
    public File createTempFileForThisThread() { return createTempFileForThisThread("regliss-file", ".dat"); }

    @Override
    public File createTempFileForThisThread(String prefix, String suffix) {
        File folder = tempFileUtils.getNodeBaseTempFolder();
        File file = null;
        try {
            if((!folder.exists())&&!folder.mkdirs()) {
                throw new ReglissException( "Could not create temp file: prefix=" + prefix + ", suffix=" + suffix + ", attempted folder: "
                        + folder.getAbsolutePath()+" base folder does not exist and could nt be created");
            }
            file = createTempFile(prefix, suffix, folder);
            if (!file.createNewFile()) {
                throw new IllegalArgumentException("Could not create temporary file: " + file.getAbsolutePath());
            }
            resourcesToClose.deleteLater(file);
            return file;
        } catch (IOException e) {
            throw new ReglissException(e, "Could not create temp file: prefix=" + prefix + ", suffix=" + suffix + ", attempted file name: "
                    + file.getAbsolutePath());
        }
    }

    @Override
    public InputStream openInputStreamForThisThread(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        resourcesToClose.closeLater(inputStream);
        return inputStream;
    }

    public void closeAfterThread(Closeable closeable) { resourcesToClose.closeLater(closeable); }


    public static File createTempFile(String prefix, String suffix, File directory) {
        File file;
        do {
            String dateStr = DateTimeFormatter.ofPattern("yyyyMMdd-hhmmss.SSS").format(LocalDateTime.now());
            String fileName = prefix + "-" + dateStr + suffix;

            file = new File(directory, FilenameUtils.getName(fileName));
        } while (file.exists());
        return file;
    }

}

