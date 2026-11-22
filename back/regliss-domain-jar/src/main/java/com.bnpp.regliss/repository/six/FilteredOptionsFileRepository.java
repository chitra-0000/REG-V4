package com.bnpp.regliss.repository.six;

import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.entity.six.entity.FilteredOptionsFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface FilteredOptionsFileRepository extends EntityRepository<FilteredOptionsFile, Long>, JpaRepository<FilteredOptionsFile, Long> {
    @Modifying
    @Transactional
    @Query(value =
            "DELETE /*+ PARALLEL(f 8) */ " +
                    "FROM   FILTERED_SIX_OPTION f "+
                    "WHERE  f.VERSION_ID = :v "+
                    " AND  f.SIX_LIST_REF = :ref "+
                    "AND  ROWNUM <= :batch "
            , nativeQuery = true)
    int deleteOptionsBatch(@Param("v") Long versionId,
                           @Param("ref") String listRef,
                           @Param("batch") int batchSize);
}

