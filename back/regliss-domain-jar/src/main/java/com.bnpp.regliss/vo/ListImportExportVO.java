package com.bnpp.regliss.vo;

import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.ImportConfiguration;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.MappingColumn;
import com.bnpp.regliss.entity.MappingFile;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.OneToMany;
import java.util.*;

public interface ListImportExportVO {

    public Long getId();
    public boolean isGenericFormat();
    public boolean isReglissFormat();
    public boolean isVigilanceFormat();
    public boolean isSafewatchFormat();
    public boolean isFircosoftFormat();
    public String getFileName();
    public ImportFileType getImportFileType();
    public boolean isChecksumReception();
    public boolean isAllowIncrementalImport();
    public boolean isLogReception();
    public boolean isConfirmationRequired();
    public Long getAutoConfirmationThreshold();
    public boolean isAllowAutoUpload();
    public boolean isAllowManualUpload();
    public String getMappingFile();
}
