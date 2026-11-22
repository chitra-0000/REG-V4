package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.Reglementation;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReglementationRepository extends EntityRepository<Reglementation, Long> {

    @Query("SELECT r from Reglementation r JOIN r.versions v WHERE v.id = ?1 AND r.deleted = false")
    List<Reglementation> findActiveByVersion(Long versionId);

    @Query("SELECT r from Reglementation r WHERE r.list.id = ?1 AND r.deleted = false")
    List<Reglementation> findActiveByList(Long listId);

    @Query("SELECT r " +
            "from Reglementation r " +
            "where r.list.id = ?1 " +
            "AND r.deleted = false " +
            "AND ( r.program IS NULL OR r.program.id in " +
            " ( SELECT p.id from Record rec JOIN rec.programs p " +
            "WHERE  rec.versionLinks.listId = r.list.id AND rec.versionLinks.fromVersionId<=?2 AND rec.versionLinks.toVersionId>=?2 ))")
    List<Reglementation> findActiveInListSpecificToProgram(Long listId, Long versionId);
}

