package com.bnpp.regliss.exporter.writer;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.GeneratedFileFormat;
import com.bnpp.regliss.entity.SubstitutionMatrixElement;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.SubstitutionMatrixElementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.lang.Long.parseLong;

@Slf4j
@Service
@ReglissBatchProfile
public class GeneratedFileWriterFactory {

    @Value("${min.available.free.spaceMB}")
    private String freeSpaceBuffer;

    @Autowired
    private SubstitutionMatrixElementRepository substitutionMatrixElementRepo;

    public Writer openWriter(GeneratedFileFormat generatedFileFormat, File file) {
        try {
            log.debug("Open writer for format {} to file {}", generatedFileFormat, file.getAbsolutePath());

            OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);//NOSONAR
            return new BufferedWriterSpaceChecker(fileWriter, parseLong(freeSpaceBuffer), file);
        } catch (IOException e) {
            throw new ReglissException(e);
        }
    }

    public GeneratedFileWriter getWriterBasedOnType(FileExportFormat fileFormat, Writer bufferedWriter) {
        List<SubstitutionMatrixElement> substitutionMatrix = substitutionMatrixElementRepo.findByFileFormat(fileFormat.getFileFormat());
        return new GeneratedFileWriter(
                bufferedWriter,
                getDelimiters(fileFormat),
                substitutionMatrix,
                fileFormat);
    }

    private GeneratedFileWriter.Delimiters getDelimiters(GeneratedFileFormat generatedFileFormat) {
        if (generatedFileFormat.getFileFormat() == FileExportFormat.FIRCOSOFT) {
            return new GeneratedFileWriter.Delimiters("\t", "", "\n", "");
        }
        switch (generatedFileFormat.getFileExtension()) {
            case csv:
                return getCsvDelimiters(generatedFileFormat.getFileFormat());
            case txt:
                return getTxtDelimiters(generatedFileFormat.getFileFormat());
            case xml:
            case zip:
                return GeneratedFileWriter.Delimiters.NO_DELIMITERS;
            default:
                throw new ReglissException("File extension not implemented for specified file format."
                        + generatedFileFormat.getFileExtension());
        }
    }

    private GeneratedFileWriter.Delimiters getCsvDelimiters(FileExportFormat fileFormat) {
        switch (fileFormat) {
            case DJ_WATCHLIST:
            case DJ_AME:
                return new GeneratedFileWriter.Delimiters(";", ",", "\r\n", "");
            default:
                return new GeneratedFileWriter.Delimiters(";", ",", "\n", "\"");
        }
    }

    private GeneratedFileWriter.Delimiters getTxtDelimiters(FileExportFormat fileFormat) {
        switch (fileFormat) {
            case VIGILANCE:
                return new GeneratedFileWriter.Delimiters(",", ",", "\n", "\"");
            case FIRCOSOFT:
                return new GeneratedFileWriter.Delimiters("\t", null, "\n", "");
            case SAFEWATCH:
                return new GeneratedFileWriter.Delimiters(";", ",", "\n", "");
            default:
                throw new ReglissException("Txt extension is not allowed for file format: " + fileFormat.toString());
        }
    }
}
