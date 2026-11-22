package com.bnpp.regliss.repository.six;

import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.entity.six.entity.InstrumentFile;
import org.springframework.data.jpa.repository.Query;

public interface InstrumentFileRepository extends EntityRepository<InstrumentFile, Long>  {
    @Query(value = "select count (distinct i.version_id) from six_instruments i", nativeQuery = true)
    int getTotalNumberOfVersionIds();

}


