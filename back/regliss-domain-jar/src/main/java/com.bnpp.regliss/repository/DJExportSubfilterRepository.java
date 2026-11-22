package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.DJExportSubfilter;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;

public interface DJExportSubfilterRepository extends EntityRepository<DJExportSubfilter, Long> {

    @Query("FROM DJExportSubfilter sf " +
        "LEFT JOIN FETCH sf.description1.recordCategory rc "
        + "LEFT JOIN FETCH sf.sanctionTypes st "
        + "LEFT JOIN FETCH sf.genders g "
        + "LEFT JOIN FETCH sf.descriptions3 d "
        + "LEFT JOIN FETCH sf.pepOccupationCategories poc "
        + "LEFT JOIN FETCH sf.residenceCountries resc "
        + "LEFT JOIN FETCH sf.jurisdictionCountries jurc "
        + "LEFT JOIN FETCH sf.relationTypes rel WHERE sf.id=?1")
    DJExportSubfilter findFetchAll(long subfilterId);


}

