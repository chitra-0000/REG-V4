package com.bnpp.regliss.facade.dto.administration;

public class SubstitutionMatrixElementDto {

    public Long id;
    public String originalValue;
    public String substitutionValue;

    public SubstitutionMatrixElementDto() {}
    public SubstitutionMatrixElementDto(String originalValue, String substitutionValue) {
        this.originalValue = originalValue;
        this.substitutionValue = substitutionValue;
    }
}

