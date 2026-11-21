package com.bnpp.regliss.six.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class SixSubFiltersDto {
    @Getter
    public Long id;
    @Getter
    public String subFilterName;
    @Getter
    public Integer sixIndex;
    @Getter
    public boolean deleted;
    @Getter
    public boolean nestedFilterStatus;
    @Getter
    public List<SixSubORFiltersDto> filters = new ArrayList<>();
}
