package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface ProgramRepository extends EntityRepository<Program, Long>, ProgramRepositoryCustom {

    @Query("SELECT p FROM Record r JOIN r.programs p WHERE r.versionLinks.listId=?1 ")
    Set<Program> getAllForList(Long listId);

    @Query("SELECT p, count(distinct r.id) "
            + "FROM Record r "
            + "JOIN r.programs p "
            + "WHERE r.versionLinks.listId=?1 "
            + "GROUP BY p.id, p.label, p.description, p.sanctionType, p.labelNormalized, p.refferenceGroup, p.givenId")
    List<Object[]> getAllForListWithRecordCount(Long listId);

    @Query("SELECT p FROM Record r JOIN Version v on r.versionLinks.listId=v.list.id and v.id=?1 JOIN r.programs p WHERE r.versionLinks.listId=?2 " +
            "AND r.versionLinks.fromVersionId<=?1 and r.versionLinks.toVersionId>=?1")
    Set<Program> getAllForVersion(Long versionId,Long listId);

    @Query("SELECT p FROM Record r JOIN Version v on r.versionLinks.listId=v.list.id and v.id=?1 JOIN r.programs p WHERE r.versionLinks.fromVersionId<=?1 and r.versionLinks.toVersionId>=?1")
    Set<Program> getAllForVersion(Long versionId);

    @Transactional(timeout = 120000)
    @Query(nativeQuery = true , value="WITH  prog AS " +
            "( SELECT /*+ materialize */ rp.program_id " +
            "  FROM RECORD r " +
            "      /* JOIN version v ON r.list_id = v.list_id " +
            "                    AND v.id = 120*/ " +
            "   JOIN record_program rp ON r.id = rp.record_id " +
            " WHERE " +
            "   r.list_id = ?2 " +
            "   AND r.from_version <= ?1 " +
            "   AND r.to_version >= ?1)     " +
            "SELECT /*gather_plan_statistics */ " +
            "    p.label " +
            "FROM " +
            "  program p  " +
            "WHERE EXISTS(SELECT 1 FROM prog r " +
            "                     WHERE r.program_id = p.id " +
            "                        ) " )
    Set<String> getAllProgramLabelsForVersion(Long versionId,Long listId);


    @Query(nativeQuery = true , value="" +
            " select p.* from program p " +
            " where p.id in ( " +
            "    select rp.program_id from record_program rp, record r " +
            "    WHERE " +
            "        r.list_id = ?2  AND " +
            "          ((r.deleted_in_version = ?1) OR (r.from_version = ?1)) and " +
            "        r.id = rp.record_id)")
    public List<Program> getCreatedModifiedDeletedRecordsProgramsForVersion(long newVersionId, long idList) ;

}

