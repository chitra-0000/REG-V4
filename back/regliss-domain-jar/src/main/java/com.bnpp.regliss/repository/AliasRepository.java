package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.record.Alias;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AliasRepository extends EntityRepository<Alias, Long> {

    @Query("SELECT a FROM Alias a JOIN a.records r WHERE r.id = ?1")
    List<Alias> getAllForRecord(Long recordId);

    @Query("SELECT max(a.reglissReference.suffix) FROM Alias a WHERE a.reglissReference.recordReglissReference = ?1 " +
            " AND length(a.reglissReference.suffix) = (select max(length(ai.reglissReference.suffix)) from Alias ai where ai.reglissReference.recordReglissReference = ?1)")
    Optional<String> getMaxReglissReferenceSuffixByRecordReglissReference(String recordReglissReference);

}

