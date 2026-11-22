package com.bnpp.regliss.repository.six;

import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.entity.six.entity.OptionsFile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface OptionsFileRepository extends EntityRepository<OptionsFile, Long> {
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "MERGE INTO SIX_OPTION sf" +
            "    USING (" +
            "        SELECT DISTINCT ch_valor, confidence_level" +
            "          FROM six_instruments" +
            "         WHERE version_id = ?2" +
            "    ) i" +
            "       ON (i.ch_valor = sf.underlying_ch" +
            "           AND sf.version_id = ?1)" +
            "    WHEN MATCHED THEN" +
            "      UPDATE SET sf.confidence_level = i.confidence_level",
            nativeQuery = true)
    int bulkUpdateConfidenceFromInstrument(Long optionsVerionId, Long instruVerionId);

    @Query(value = "select count (distinct i.version_id) from SIX_OPTION i", nativeQuery = true)
    int getTotalNumberOfVersionIds();
}

