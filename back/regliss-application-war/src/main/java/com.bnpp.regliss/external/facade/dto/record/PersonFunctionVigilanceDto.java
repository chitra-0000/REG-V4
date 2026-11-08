package com.bnpp.regliss.external.facade.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PersonFunctionVigilanceDto {
    @JsonIgnore
    public Long id;
    public String pepOccupationCategoryLabel;
    public String functionName;
    public String from;
    public String to;
}

