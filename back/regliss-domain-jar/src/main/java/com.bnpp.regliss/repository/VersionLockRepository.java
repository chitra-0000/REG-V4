package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.VersionLock;
import com.bnpp.regliss.repository.base.EntityRepository;


public interface VersionLockRepository extends EntityRepository<VersionLock, Long> , VersionLockRepositoryCustom{

}

