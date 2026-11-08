package com.bnpp.regliss.external.facade.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DateDetailsVigilanceDto {
    @JsonIgnore
    public Long id;
    public String dateValue;
    public String dateTypeLabel;
}

