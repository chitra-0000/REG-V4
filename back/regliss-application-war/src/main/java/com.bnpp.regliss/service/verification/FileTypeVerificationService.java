package com.bnpp.regliss.service.verification;

import com.bnpp.regliss.ReglissException;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;

@Service
public class FileTypeVerificationService {
    private static HashMap<String, String> mimeTypeExtensionMap = new HashMap<>();
    private static final String FILE_NAME_REGEX_PARTIAL = "^[a-zA-Z0-9._ -]";

    public static final String FILE_EXTENSION_PDF = "(?i)pdf";

    @Value("${allowable.upload.file.extensions}")
    private String uploadAddDocumentFileExtensions;

    @Value("${allowable.upload.file.size.max}")
    private Integer uploadAddDocumentFileSizeLimit;

    static {


        mimeTypeExtensionMap.put("ppt", "application/vnd.ms-powerpoint;application/x-tika-msoffice");
        mimeTypeExtensionMap.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation;application/x-tika-ooxml");
        mimeTypeExtensionMap.put("doc", "application/msword;application/x-tika-msoffice;text/html");
        mimeTypeExtensionMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document;application/x-tika-ooxml");
        mimeTypeExtensionMap.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;application/x-tika-ooxml");
        mimeTypeExtensionMap.put("xls", "application/vnd.ms-excel;application/x-tika-msoffice");
        mimeTypeExtensionMap.put(FILE_EXTENSION_PDF, "application/pdf");
        mimeTypeExtensionMap.put("txt", "text/plain");
        mimeTypeExtensionMap.put("csv", "text/plain;text/csv");
        mimeTypeExtensionMap.put("pdf", "application/pdf");
    }

    public void checkNameAndType(MultipartFile file){
        if(file == null){
            return;
        }
        Long s= file.getSize();
        String fileExtension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (!uploadAddDocumentFileExtensions.contains(fileExtension)) {
            throw new ReglissException("Invalid file extension", ReglissException.ErrorCode.FILE_EXTENSION_INVALID);
        } else if ((s > uploadAddDocumentFileSizeLimit)) {
            throw new ReglissException("File size exceeds ", ReglissException.ErrorCode.FILE_SIZE_EXCEEDS);
        }
        checkFile(file, fileExtension);
    }

    public void checkFile(MultipartFile file, String extension){
        String originalFilename = file.getOriginalFilename();
        String regex = FILE_NAME_REGEX_PARTIAL + "+." + extension + "$";
        boolean matches = originalFilename != null && originalFilename.matches(regex);
        Tika tika = new Tika();
        try {
            if (!matches){
                throw new ReglissException("File name contains forbidden characters", ReglissException.ErrorCode.FILE_NAME_DOES_NOT_MATCH_REGEX);
            }else if (!mimeTypeExtensionMap.get(extension).contains(tika.detect(file.getInputStream()))){
                throw new ReglissException("Wrong file type", ReglissException.ErrorCode.FILE_TYPE_INVALID);
            }
        } catch (IOException e) {
            throw new ReglissException(e, "Could not upload file: " + e.getMessage());
        }
    }
}

