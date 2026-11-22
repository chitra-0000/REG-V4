package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.referential.record.RelationType;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface RelationTypeRepository extends EntityRepository<RelationType, Long> {

    @Cacheable("threadCache")
    List<RelationType> findAll();

    RelationType findByLabel(String description1Label);

}

