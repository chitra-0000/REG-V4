package com.bnpp.regliss.repository;

import java.util.List;

import com.bnpp.regliss.entity.CftgtwRequest;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;

public interface CftgtwRequestRepository extends EntityRepository<CftgtwRequest, Long> {

    @Query(
            value = "SELECT * FROM CFT_GTW_REQUEST WHERE DJ_FORMAT LIKE '%DOW%' AND ACTUAL_RUNTIME IS NULL",
            nativeQuery = true
    )
    List<CftgtwRequest> findByActualRunTimeIsNull();

    @Query(
            value = "SELECT * FROM CFT_GTW_REQUEST WHERE DJ_FORMAT LIKE '%SIX%' AND ACTUAL_RUNTIME IS NULL",
            nativeQuery = true
    )
    List<CftgtwRequest> findByActualSixRunTimeIsNull();


    public List<CftgtwRequest> findByRemainingCftLogTimeIdsIsNotNull();

    @Query("SELECT cgr.id FROM CftgtwRequest cgr WHERE cgr.remainingCftLogTimeIds is not null")
    List<Long> getAllIdsByRemainingCftLogTimeIdsIsNotNull();

}

