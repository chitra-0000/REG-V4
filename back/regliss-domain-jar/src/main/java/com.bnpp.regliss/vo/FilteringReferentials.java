package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.referential.record.*;
import lombok.Data;

import java.util.List;

@Data
public class FilteringReferentials {

    private List<RecordCategory> categories;
    private List<SanctionType> sanctionTypes;
    private List<Description3> descriptions3;
    private List<PEPOccupationCategory> pepOccupationCategories;
    private List<Long> internationalCountriesId;

}

