package com.bnpp.regliss.facade.dto.list;

// [MISSING FROM SCREENSHOTS: lines 3-7 of the original file were not visible in the photos]

public class CriteriaDto {
    public List<Description1FilterValues> description1Ids = new ArrayList<>();
    public List<Long> description2Ids = new ArrayList<>();
    public List<Long> description3Ids = new ArrayList<>();
    public List<Long> pepOccupationCategoryIds = new ArrayList<>();
    public List<Long> residenceCountryIds = new ArrayList<>();
    public List<Long> jurisdictionCountryIds = new ArrayList<>();
    public List<Long> relationTypeIds = new ArrayList<>();
    public List<Long> gendersIds = new ArrayList<>();
    public OperatorCriteriaDto sinceDeathRca;
    public OperatorCriteriaDto sinceDeathPep;
    public OperatorCriteriaDto inactivityPep;
    public OperatorCriteriaDto inactivityRCA;

}

