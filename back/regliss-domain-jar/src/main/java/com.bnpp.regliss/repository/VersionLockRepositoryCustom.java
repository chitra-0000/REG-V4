package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.VersionLock;

import java.util.List;
import java.util.Set;

public interface VersionLockRepositoryCustom {

    List<VersionLock> findLastLock(Set<Long> versionIds);
}

