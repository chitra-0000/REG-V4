package com.bnpp.regliss.entity;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import org.apache.commons.io.FilenameUtils;

import java.util.stream.Stream;

public enum UploadedFileExtension {
    CSV("csv"), XLS("xls"), XLSX("xlsx"), XML("xml"), ZIP("zip");

    private final String extension;

    UploadedFileExtension(String extension) { this.extension = extension; }

    public static UploadedFileExtension fromFileName(String fileName) {
        return Stream.of(values())
                .filter(e -> e.extension.equalsIgnoreCase(FilenameUtils.getExtension(fileName)))
                .findFirst()
                .orElseThrow(() -> new ReglissException(ErrorCode.IMPORT_FILE_ILLEGAL_EXTENSION));
    }
}
