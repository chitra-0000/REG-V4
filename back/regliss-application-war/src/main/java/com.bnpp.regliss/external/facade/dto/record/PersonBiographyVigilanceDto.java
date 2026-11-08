package com.bnpp.regliss.external.facade.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PersonBiographyVigilanceDto {
    @JsonIgnore
    public Long id;
    public String dateValue;
    public String dateTypeLabel;
    public String birthPlace;
    public String countryLabel;
    public String additionalInfo;

}

