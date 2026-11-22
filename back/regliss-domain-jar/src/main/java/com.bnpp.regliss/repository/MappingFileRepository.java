package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.MappingFile;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MappingFileRepository extends EntityRepository<MappingFile, Long> {

    @Query("SELECT mf FROM MappingFile mf LEFT JOIN FETCH mf.columns WHERE mf.list.id = ?1 AND mf.deleted=false")
    Optional<MappingFile> findActiveByListId(long reglissListId);

}

