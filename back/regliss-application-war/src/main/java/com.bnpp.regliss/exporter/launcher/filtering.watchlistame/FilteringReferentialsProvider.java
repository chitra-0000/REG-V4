package com.bnpp.regliss.exporter.launcher.filtering.watchlistame;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.vo.FilteringReferentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@ReglissBatchProfile
public class FilteringReferentialsProvider {

    @Autowired
    private RecordCategoryRepository recordCategoryRepo;

    @Autowired
    private SanctionTypeRepository sanctionTypeRepo;

    @Autowired
    private Description3Repository description3Repo;

    @Autowired
    private PEPOccupationCategoryRepository pepOccupationRepo;

    @Autowired
    private CountryRepository countryRepo;

    public FilteringReferentials getFilteringReferentials() {
        FilteringReferentials filteringReferentials = new FilteringReferentials();
        filteringReferentials.setCategories(recordCategoryRepo.findAll());
        filteringReferentials.setSanctionTypes(sanctionTypeRepo.findAll());
        filteringReferentials.setDescriptions3(description3Repo.findAll());
        filteringReferentials.setPepOccupationCategories(pepOccupationRepo.findAll());
        List<Country> internationalCountries = countryRepo.findByBusinessConcept(ReferentialBusinessConcept.COUNTRY_INTERNATIONAL);
        if (internationalCountries.isEmpty()) {
            throw new IllegalArgumentException("No Country found for business concept = '" + ReferentialBusinessConcept.COUNTRY_INTERNATIONAL
                    + "'");
        }
        filteringReferentials.setInternationalCountriesId(internationalCountries.stream().map(Country::getId).collect(toList()));
        return filteringReferentials;
    }
}
