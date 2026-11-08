package com.bnpp.regliss.external.facade.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SanctionReferenceVigilanceDto {
    @JsonIgnore
    public Long id;
    public String remarks;
    public String name;
    public String sinceDate;
    public String endDate;
}

