package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.Log;
import com.bnpp.regliss.entity.ObjectType;
import com.bnpp.regliss.repository.base.EntityRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LogRepository extends EntityRepository<Log, Long> {

    List<Log> findAllByObjectIdAndObjectType(Long objectId, ObjectType objectType);

    @Modifying
    @Query("DELETE FROM Log l WHERE  l.version.id =?1")
    void deleteLogByVersion(Long versionId);
}

