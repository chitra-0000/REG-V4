package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.referential.record.*;
import lombok.Data;

import java.util.Set;

@Data
public class RecordDescriptions {

    public RecordDescriptions(Set<RecordCategory> categories, Set<SanctionType> sanctionTypes, Set<Description3> descriptions3,
                              Set<PEPOccupationCategory> pepOccupationCategories) {
        super();
        this.categories = categories;
        this.sanctionTypes = sanctionTypes;
        this.descriptions3 = descriptions3;
        this.pepOccupationCategories = pepOccupationCategories;
    }

    private final  Set<RecordCategory> categories;
    private final  Set<SanctionType> sanctionTypes;
    private final  Set<Description3> descriptions3;
    private final  Set<PEPOccupationCategory> pepOccupationCategories;





}
