package com.bnpp.regliss.repository;

import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Slf4j
public class ProgramRepositoryImpl implements ProgramRepositoryCustom {

    @Autowired
    private EntityManager em;

    @Override
    public void relinkRecordsTo(Map<Long, Long> newToCorrectProgramIds) {
        if (newToCorrectProgramIds.isEmpty()) {
            return;
        }
        relinkRecordProgram(newToCorrectProgramIds);

        deleteProgramsByIds(newToCorrectProgramIds.keySet());
    }

    private void relinkRecordProgram(Map<Long, Long> newToCorrectProgramIds) {
        log.info("Relink records to existing programs...");
        for (Entry<Long, Long> entry : newToCorrectProgramIds.entrySet()) {
            em.createNativeQuery("UPDATE RECORD_PROGRAM SET PROGRAM_ID = :uniqueProgramId WHERE PROGRAM_ID = :alreadyInsertedProgramId")
                .setParameter("uniqueProgramId", entry.getValue())
                .setParameter("alreadyInsertedProgramId", entry.getKey())
                .executeUpdate();
        }
    }

    private void deleteProgramsByIds(Set<Long> ids) {
        log.info("Deleting {} orphan Programs", ids.size());
        for (List<Long> id1000 : Iterables.partition(ids, 1000)) {
            int actualDeleted = em.createQuery("DELETE FROM Program p WHERE p.id in (:ids)")
                .setParameter("ids", id1000)
                .executeUpdate();
            log.debug("Deleted: " + actualDeleted);
        }
    }
}
