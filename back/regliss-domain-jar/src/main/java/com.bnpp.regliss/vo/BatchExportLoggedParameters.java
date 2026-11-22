package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.batch.BatchExportParams;
import com.bnpp.regliss.entity.batch.BatchExportType;
import com.bnpp.regliss.service.GenerationReason;
import lombok.Data;

import java.util.List;

/**
 * This class is used to serialize the parameters of the export process
 * and log them in the database in the column JOB_PARAMS of the table CTR_BATCH_JOB_EXECUTION.
 * WARNING: The data in the column JOB_PARAMS of the table CTR_BATCH_JOB_EXECUTION is later deserialized to an object of this type,
 * so any changes performed on current class may also affect the deserialization process for older parameters serialized from previous versions of this class.
 */
@Data
public class BatchExportLoggedParameters implements BatchJobParameterLoggable {
    private static final long serialVersionUID = 1L;

    private BatchExportType exportType;
    private List<Long> generatedFileIds;
    private String launcherUsername;
    private GenerationReason reason;
    private Long versionId;

    //used by JACKSON DONT DELETE IT!!!
    private BatchExportLoggedParameters() {}

    public BatchExportLoggedParameters(BatchExportParams exportParams) {
        this.exportType = exportParams.getExportType();
        this.generatedFileIds = exportParams.getGeneratedFileIds();
        this.launcherUsername = exportParams.getLauncherUsername();
        this.reason = exportParams.getReason();
        exportParams.getVersionOpt()
                .ifPresent(v -> this.versionId = v.getId());
    }

    @Override
    public String getPrettyValue() {
        String value = "Export type: " + exportType
                + "; " + "Generated files ids: " + generatedFileIds
                + "; " + "Type of generation: " + reason
                + "; " + "Launching user: " + launcherUsername;
        if (versionId != null) {
            value += "; " + "Version id: " + versionId;
        }
        return value;
    }
}
