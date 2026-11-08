package com.bnpp.regliss.external.facade.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RecordRelationVigilanceDto {
    @JsonIgnore
    public Long id;
    public String categoryLabel;
    public String name;
    public String typeLabel;
    public Boolean ex;
    public String relationTypeLabel;
    public String externalReference;
}

