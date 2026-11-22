package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.FileDelivery;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.search.FileDeliverySearchCriteria;

import java.util.List;

public interface FileDeliveryRepositoryCustom{
    List<FileDelivery> getFileDeliveries();
    PaginatedResults<FileDelivery> search(FileDeliverySearchCriteria criteria);
    List<FileDelivery> findScheduledDeliveriesOfOldVersions();
}

