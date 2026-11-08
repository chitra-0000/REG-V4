package com.bnpp.regliss.facade.dto;

public class RecordCountSearchDto {

    public Long value;
    public Long limitValue;

    public RecordCountSearchDto(Long value, Long limitValue) {
        this.value = value;
        this.limitValue = limitValue;
    }
}

