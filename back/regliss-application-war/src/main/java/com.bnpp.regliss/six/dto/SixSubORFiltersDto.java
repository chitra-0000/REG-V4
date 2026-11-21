package com.bnpp.regliss.six.dto;

import lombok.Getter;

public class SixSubORFiltersDto {
    @Getter
    public Long id;
    @Getter
    public Integer sixORIndex;
    @Getter
    public String fieldName;
    @Getter
    public String operator;
    @Getter
    public String filterValue;
    @Getter
    public boolean deleted;
}
