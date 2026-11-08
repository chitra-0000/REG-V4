package com.bnpp.regliss.external.facade.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CountryDetailsVigilanceDto {
    @JsonIgnore
    public Long id;
    public String typeLabel;
    public String countryLabel;
    public Boolean principal;
    public String additionalInfo;
}

