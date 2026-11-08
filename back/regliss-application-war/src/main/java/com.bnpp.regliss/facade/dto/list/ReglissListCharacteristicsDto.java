package com.bnpp.regliss.facade.dto.list;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

public class ReglissListCharacteristicsDto {

    public Long id;
    public String reference; //should be number?
    public String shortName;
    public String name;
    public String description;
    public Long regulatorOrInternalPolicy;
    public ReglissListType type;
    public Confidentiality confidentiality;
    public Scope scope;
    public Long country;
    public Long territory;
    public String owner;
    public Long issuer;
    public Long listCategory;
    public Boolean active;
    public Boolean isGlobalPlusOfac;
    public Long geographicData;

    public ReglissListCharacteristicsDto(){}
    public ReglissListCharacteristicsDto(Long id){this.id=id;}

}

