package com.bnpp.regliss.external.facade.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AliasVigilanceDto {
    @JsonIgnore
    public Long id;
    public String reglissReference;
    public String title;
    public String firstName;
    public String middleName;
    public String lastName;
    public String typeLabel;
    public String qualityLabel;
    public String language;
}

