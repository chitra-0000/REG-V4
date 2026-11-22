package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;

import java.util.List;

public interface WatchlistAmeRecordExportFilterRepository {

    List<Object[]> getRecordCategories(Long idVersion);

    List<Object[]> getSanctionTypes(Long idVersion);

    List<Object[]> getRecordDescriptions3(Long idVersion);

    List<Object[]> getRecordPepOccupations(Long idVersion);

    List<Object[]> getRecordCountriesInVersionByBusinessConcept(Long idVersion, ReferentialBusinessConcept countryType);

    List<Object[]> getRecordDatesInVersionByBusinessConcept(Long idVersion, ReferentialBusinessConcept dateType);

    List<Object[]> getRecordGenders(Long idVersion);


}


