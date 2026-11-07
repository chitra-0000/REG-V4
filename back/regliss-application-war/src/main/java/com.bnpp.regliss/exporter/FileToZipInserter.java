package com.bnpp.regliss.exporter;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
@UtilityClass
public class FileToZipInserter {

    private final int BUFFER_SIZE = 4096;

    public void addToZip(String fileName, File file, ZipOutputStream zipStream) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipStream.putNextEntry(zipEntry);

            byte[] data = new byte[BUFFER_SIZE];
            CRC32 crc = new CRC32();
            int noBytesRead;
            while ((noBytesRead = is.read(data)) > 0) {
                zipStream.write(data, 0, noBytesRead);
                crc.update(data, 0, noBytesRead);
            }
            zipEntry.setCrc(crc.getValue());
            zipStream.closeEntry();
        }
    }
}
