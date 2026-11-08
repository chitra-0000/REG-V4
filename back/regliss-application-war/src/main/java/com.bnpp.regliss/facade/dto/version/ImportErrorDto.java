package com.bnpp.regliss.facade.dto.version;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class ImportErrorDto {

    public ValidationError importError;

    public List<ImportLineErrorsDto> importLineErrorsDtos = new ArrayList<>();

    //Used for Jackson deserialization
    public ImportErrorDto(){}

    public ImportErrorDto(ReglissException reglissException, List<ImportLineErrorsDto> importLineErrorsDtos) {
        this(reglissException);
        this.importLineErrorsDtos = importLineErrorsDtos;
    }

    public ImportErrorDto(List<ImportLineErrorsDto> importLineErrorsDtos) {
        this.importLineErrorsDtos = importLineErrorsDtos;
    }

    public ImportErrorDto(ReglissException reglissException) {
        if(reglissException != null) {
            this.importError = new ValidationError(reglissException.getErrorCode(), reglissException.getParameters());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("#");
        if (importError != null) {
            sb.append(importError);
        }
        if (!importLineErrorsDtos.isEmpty()) {
            sb.append(" : ");
            sb.append(importLineErrorsDtos.toString());
        }
        return sb.toString();
    }

}

