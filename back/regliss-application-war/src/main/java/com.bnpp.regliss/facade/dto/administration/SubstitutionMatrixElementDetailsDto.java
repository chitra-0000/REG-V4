package com.bnpp.regliss.facade.dto.administration;

import com.bnpp.regliss.entity.referential.FileExportFormat;

public class SubstitutionMatrixElementDetailsDto {

    public Long id;
    public FileExportFormat fileFormat;
    public String originalValue;
    public String substitutionValue;

    public SubstitutionMatrixElementDetailsDto() {}
    public SubstitutionMatrixElementDetailsDto(FileExportFormat fileFormat, String originalValue, String substitutionValue) {
        this.fileFormat = fileFormat;
        this.originalValue = originalValue;
        this.substitutionValue = substitutionValue;
    }
}

