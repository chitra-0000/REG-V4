package com.bnpp.regliss.six.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class SixFiltersAndSubFiltersDto {
    @Getter
    public SixFiltersDto sixFilter;
    @Getter
    public List<SixSubFiltersDto> sixSubfilters = new ArrayList<>();
}
