package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.ImportedFile;
import lombok.Data;

/**
 * This class is used to serialize the parameters of the semi-automatic import process
 * and log them in the database in the column JOB_PARAMS of the table CTR_BATCH_JOB_EXECUTION.
 * WARNING: The data in the column JOB_PARAMS of the table CTR_BATCH_JOB_EXECUTION is later deserialized to an object of this type,
 * so any changes performed on current class may also affect the deserialization process for older parameters serialized from previous versions of this class.
 */
@Data
public class BatchImportSemiAutoLoggedParameters implements BatchJobParameterLoggable {
    private static final long serialVersionUID = 1L;
    private long uploadedFileId;
    private String fileName;
    private String originalFileName;
    private String userName;
    private Long versionId;

    //used by JACKSON DONT DELETE IT!!!
    public BatchImportSemiAutoLoggedParameters() {}

    public BatchImportSemiAutoLoggedParameters(ImportedFile importedFileInfo) {
        this.uploadedFileId = importedFileInfo.getId();
        this.fileName = importedFileInfo.getFileName();
        this.originalFileName = importedFileInfo.getOriginalFileName();
        this.userName = importedFileInfo.getUserName();
        this.versionId = importedFileInfo.getVersion().getId();
    }


    @Override
    public String getPrettyValue() {
        return "Import for version id: " + versionId
                + "; " + "Uploaded file id: " + uploadedFileId
                + "; " + "Filename: " + fileName
                + "; " + "Original filename: " + originalFileName
                + "; " + "Launching user: " + userName;
    }

}