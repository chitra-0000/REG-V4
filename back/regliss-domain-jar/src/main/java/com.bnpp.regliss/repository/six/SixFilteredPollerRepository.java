package com.bnpp.regliss.repository.six;

import com.bnpp.regliss.entity.SixFilteredPoller;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SixFilteredPollerRepository extends EntityRepository<SixFilteredPoller, Long> {

    List<SixFilteredPoller> findByStatus(@Param("status") String status);

    List<SixFilteredPoller> findByStatusAndInsertionTimeGreaterThanEqual(@Param("status") String status,
                                                                         @Param("insertionTime") LocalDateTime insertionTime);

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM SIX_FILTERED_POLLER p"
            + " LEFT JOIN CTR_BATCH_JOB_EXECUTION j ON j.ID = p.BATCH_JOB_EXECUTION_ID"
            + " WHERE p.SIX_LIST_REFERENCE = :listRef AND p.GENERATION_REASON = 'REGENERATION'"
            + " AND (p.STATUS = 'BUILT'"
            + "   OR (p.STATUS = 'BUILDING' AND p.UPDATED_TIME > :since)"
            + "   OR (p.STATUS IN ('PENDING', 'READY') AND j.END_DATE IS NULL AND j.LAST_UPDATE_DATE > :since))")
    long countRunningRegenerationRows(@Param("listRef") String listRef, @Param("since") LocalDateTime since);

}


