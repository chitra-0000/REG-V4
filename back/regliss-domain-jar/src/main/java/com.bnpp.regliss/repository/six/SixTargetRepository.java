package com.bnpp.regliss.repository.six;

import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.entity.six.entity.SixTarget;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SixTargetRepository extends EntityRepository<SixTarget, Long>, JpaSpecificationExecutor<SixTarget> {

    @Query("SELECT DISTINCT t.regime FROM SixTarget t ORDER BY t.regime")
    List<String> findAllDistinctRegimes();


}


