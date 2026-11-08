package com.bnpp.regliss.facade.dto.record;

import lombok.Getter;

public class SanctionReferenceDto {
    public Long id;
    @Getter public String remarks;
    @Getter public String name;
    @Getter public String sinceDate;
    @Getter public String endDate;
    @Getter public String code;
    @Getter public String listName;
}

