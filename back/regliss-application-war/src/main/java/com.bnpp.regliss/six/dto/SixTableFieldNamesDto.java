package com.bnpp.regliss.six.dto;

import lombok.Getter;

import java.util.List;

public class SixTableFieldNamesDto {

    public SixTableFieldNamesDto(String filterType, List<String> fieldNames) {
        this.filterType = filterType;
        this.fieldNames = fieldNames;
    }

    @Getter
    public String filterType;
    @Getter
    public List<String> fieldNames;
}
