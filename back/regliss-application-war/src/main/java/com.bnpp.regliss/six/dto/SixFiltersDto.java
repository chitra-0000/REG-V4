package com.bnpp.regliss.six.dto;

import lombok.Getter;

public class SixFiltersDto {
    @Getter
    public Long id;
    @Getter
    public String filterName;
    @Getter
    public String fileType;
    @Getter
    public String filterTag;
    @Getter
    public boolean deleted;
    @Getter
    public boolean isActive;
    @Getter
    public boolean excludeCmic;
    @Getter
    public boolean excludeE014071;
}
