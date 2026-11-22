package com.bnpp.regliss.utils;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.service.IAutocloseableResources;
import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.*;
import java.nio.file.Files;
import java.sql.Blob;
import java.util.zip.*;

@Service
public class BlobService {

    @Autowired
    @PersistenceContext
    private EntityManager em;

    @Autowired
    private IAutocloseableResources fileHelper;

    public Blob createBlob(long fileLength, InputStream is) {
        Session session = em.unwrap(SessionImplementor.class);
        return Hibernate.getLobCreator(session).createBlob(is, fileLength);
    }

    public Blob createBlob(File file) {
        try {
            InputStream fileInputStream = fileHelper.openInputStreamForThisThread(file);// NOSONAR
            return createBlob(file.length(), fileInputStream);
        } catch (IOException e) {
            throw new ReglissException(e, e.getMessage());
        }
    }

    public Blob getZippedBlob(File rawFile, String fileName) { return createBlob(createZippedArchive(rawFile, fileName)); }

    private File createZippedArchive(File rawFile, String originalFileName) {
        try {
            File zipFile = fileHelper.createTempFileForThisThread(rawFile.getName() + "-zip", ".zip");
            try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
                ZipEntry zipEntry = new ZipEntry((originalFileName == null) ? rawFile.getName() : originalFileName);
                zipStream.putNextEntry(zipEntry);
                Files.copy(rawFile.toPath(), zipStream);
                zipStream.closeEntry();
            }

            return zipFile;
        } catch (IOException e) {
            throw new ReglissException(e, e.getMessage());
        }
    }

    public void unzipBlob(Blob zippedBlob, OutputStream outputStream) {
        try {
            IOUtils.copy(new GZIPInputStream(zippedBlob.getBinaryStream()), outputStream);
        } catch (ZipException ze) {
            if (ze.getMessage().equals("Not in GZIP format")) {
                unzipZipBlob(zippedBlob, outputStream);
            } else {
                throw new ReglissException(ze, ze.getMessage());
            }
        } catch (Exception e) {
            throw new ReglissException(e, e.getMessage());
        }
    }

    private void unzipZipBlob(Blob zippedBlob, OutputStream outputStream) {
        try (ZipInputStream zipInputStream = new ZipInputStream(zippedBlob.getBinaryStream())) {
            zipInputStream.getNextEntry();
            IOUtils.copy(zipInputStream, outputStream);
        } catch (Exception e) {
            throw new ReglissException(e, e.getMessage());
        }
    }
}
