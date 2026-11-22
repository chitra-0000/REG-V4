package com.bnpp.regliss.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bnpp.regliss.entity.record.RecordAssociation;
import com.bnpp.regliss.repository.base.EntityRepository;

public interface RecordAssociationRepository extends EntityRepository<RecordAssociation, Long>{

    @Query(nativeQuery = true, value = "select r2.name, a.ex, r2.record_type, a.relation_type, "
            + "listagg(c.id, ',') WITHIN group(ORDER BY c.id) as ids , r2.EXTERNAL_UUID " +
            "from record r " +
                "inner join record_association ra on ra.record_id = r.id " +
                "inner join association a on a.id = ra.record_association_id " +
            "left join record r2 on r2.external_reference = a.external_reference " +
            "left join record_categories rc on rc.record_id = r2.id " +
            "left join ref_record_category c on c.id = rc.category_id " +
            "where r.external_uuid = :uuid " +
            "and r2.from_version <= r.from_version  AND   r2.to_version >= r.to_version AND r2.list_Id=r.list_Id " +
            "group by  r2.name, a.ex, r2.record_type, a.relation_type , r2.EXTERNAL_UUID")
    List<Object[]> getWatchlistAssocationsFields(@Param("uuid") String uuid);

    @Query(nativeQuery = true, value = "select r2.name, a.ex, r2.record_type, a.relation_type, "
            + "listagg(rc.category_id, ',') WITHIN group(ORDER BY rc.category_id) as ids " +
            "from record r " +
            "inner join record_association ra on ra.record_id = r.id " +
            "inner join association a on a.id = ra.record_association_id " +
            "left join record r2 on r2.external_reference = a.external_reference " +
            "left join record_categories rc on rc.record_id = r2.id " +
            "where r.id = :id " +
            "and r.list_id = :listId " +
            "and r2.list_id = :listId " +
            "and r2.from_version <= :versionId  AND   r2.to_version >= :versionId " +
            "group by  r2.name, a.ex, r2.record_type, a.relation_type")
    List<Object[]> getWatchlistAssociationsFieldsByIdAndList(@Param("id")Long id, @Param("listId") Long listId, @Param("versionId") Long versionId);

    @Query(nativeQuery = true, value = "select r2.name, a.ex, r2.record_type, a.relation_type, "
            + "listagg(rc.category_id, ',') WITHIN group(ORDER BY rc.category_id) as ids, " +
            "r2.external_reference "+
            "from record r " +
            "inner join record_association ra on ra.record_id = r.id " +
            "inner join association a on a.id = ra.record_association_id " +
            "left join record r2 on r2.external_reference = a.external_reference " +
            "left join record_categories rc on rc.record_id = r2.id " +
            "where r.id = :id " +
            "and r.list_id = :listId " +
            "and r2.list_id = :listId " +
            "and r2.from_version <= :versionId  AND   r2.to_version >= :versionId " +
            "group by  r2.name, a.ex, r2.record_type, a.relation_type, r2.external_reference")
    List<Object[]> getWatchlistAssociationsFieldsByIdAndListV3(@Param("id")Long id, @Param("listId") Long listId, @Param("versionId") Long versionId);


    @Query(nativeQuery = true, value = "select /*+ index_ffs(rc) */  r2.name, a.ex, r2.record_type, a.relation_type, " +
            " listagg(rc.category_id, ',') WITHIN group(ORDER BY rc.category_id) as ids " +
            " from record r " +
            " inner join record_association ra on ra.record_id = r.id " +
            " inner join association a on a.id = ra.record_association_id " +
            " inner join record r2 on r2.external_reference = a.external_reference " +
            " left join record_categories rc on rc.record_id = r2.id "+
            " where r.id = :id " +
            "   and r.list_id = :listId " +
            "   and r2.list_id = :listId " +
            "   and r2.from_version <= :versionId " +
            "   AND r2.to_version >= :versionId "+
            " group by r2.name, a.ex, r2.record_type, a.relation_type")
    List<Object[]> getWatchlistAssociationsFieldsByIdAndListPerf(@Param("id")Long id, @Param("listId") Long listId,  @Param("versionId") Long versionId);

}

