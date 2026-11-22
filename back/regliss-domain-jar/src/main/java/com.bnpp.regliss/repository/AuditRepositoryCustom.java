package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.repository.pagination.PaginationOptions;

import java.util.List;

public interface AuditRepositoryCustom {

    public List<Audit> queryAudits4MinorUpdates(Long versionId, PaginationOptions paginationOptions);

    public Long countAudits4MinorUpdates(Long versionId);
    List<Long> getObjectIdsWithMinorUpdates(Long versionId);
    Long countTotalAuditsForObjectIds(Long versionId, List<Long> objectIds);
    List<Audit> getPaginatedAuditsForObjectIds(Long versionId, List<Long> objectIds, PaginationOptions pagination);


    Long countTotalAuditsForVersion(Long versionId);

}

