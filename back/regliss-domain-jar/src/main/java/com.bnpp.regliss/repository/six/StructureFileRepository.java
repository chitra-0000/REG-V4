package com.bnpp.regliss.repository.six;

import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.entity.six.entity.StructuredFile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface StructureFileRepository extends EntityRepository<StructuredFile, Long> {
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "MERGE INTO six_structured sf" +
            "    USING (" +
            "        SELECT DISTINCT ch_valor, confidence_level" +
            "          FROM six_instruments" +
            "         WHERE version_id = ?2" +
            "    ) i" +
            "      ON (i.ch_valor = sf.underlying_ch" +
            "          AND sf.version_id = ?1)" +
            "    WHEN MATCHED THEN" +
            "     UPDATE SET sf.confidence_level = i.confidence_level",
           nativeQuery = true)
    int bulkUpdateConfidenceFromInstrument(Long structVerionId, Long instruVerionId);

    @Query(value = "select count (distinct i.version_id) from six_structured i", nativeQuery = true)
    int getTotalNumberOfVersionIds();
}

