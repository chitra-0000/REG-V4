package com.bnpp.regliss.repository;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;

import com.bnpp.regliss.entity.ListPermissionMatrix;
import com.bnpp.regliss.entity.Profile;
import com.bnpp.regliss.repository.base.EntityRepository;

public interface ListPermissionMatrixRepository extends EntityRepository<ListPermissionMatrix, Long>{

    @Cacheable("listPermissionMatrix")
    List<ListPermissionMatrix> findByProfile(Profile profile);
}

