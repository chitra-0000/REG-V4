package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends EntityRepository<Country, Long>{

    @Query("Select c from Country c join RefogCountryMapping rm on c.ISO2Code = rm.isoCode where c.v3Only=false and LOWER(rm.refogLabel)=LOWER(?1)")
    Optional<Country> findByRefogLabelMapping(String label);

    Optional<Country> findByLabelIgnoreCaseAndV3OnlyFalse(String label);

    Country findByLabel(String label);

    Country findByLabelIgnoreCase(String label);

    List<Country> findByBusinessConcept(ReferentialBusinessConcept businessConcept);

}

