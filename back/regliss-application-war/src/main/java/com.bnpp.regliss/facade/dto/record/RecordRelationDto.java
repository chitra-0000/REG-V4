package com.bnpp.regliss.facade.dto.record;

import com.bnpp.regliss.entity.referential.record.RecordType;

public class RecordRelationDto {
    public Long id;
    public Long category;
    public String name;
    public RecordType type;
    public Boolean ex;
    public Long relationTypeId;
}

