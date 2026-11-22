package com.bnpp.regliss.repository;

import java.util.*;

import org.springframework.data.jpa.repository.*;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.repository.base.*;


public interface GeneratedFileRepository extends EntityRepository<GeneratedFile, Long>, GeneratedFileRepositoryCustom {

    @Query("SELECT gf FROM GeneratedFile gf WHERE gf.version.id = ?1 "
            + "AND gf.generatedFileFormat.fileFormat != 'DELIVERY_REPORT'")
    List<GeneratedFile> getVersionGeneratedFiles(Long versionId);

    @Query("SELECT gf FROM GeneratedFile gf " +
            " Join gf.version v "
            + "JOIN v.list l "
            + "JOIN l.subscriptions s "
            + "JOIN s.subscriber sub "
            + "WHERE sub.id=?1 "
            + "AND s.id=?2 "
            + "AND s.deleted = false "
            + "AND sub.active = true "
            + "AND v.publicationDate != null "
            + "AND v.versionNumber = "
            + "(SELECT MAX(v2.versionNumber) "
            + "FROM l.versions v2 "
            + "WHERE v2.publicationDate != null) ")
    List<GeneratedFile> getGeneratedFilesForAllPublishedVersionsForSubscriberAndSubscription(Long subscriberId, Long subscriptionId);

    @Query("SELECT gf FROM GeneratedFile gf WHERE gf.version IS NULL")
    List<GeneratedFile> getOfacGeneratedFiles();

    @Query("SELECT gf FROM GeneratedFile gf WHERE gf.id = (SELECT max (gf1.id) FROM GeneratedFile gf1 where gf1.subscriber.id = ?1 )")
    GeneratedFile getLastGeneratedFileBySubscriberId(Long subscriberId);

    @Query("SELECT gf FROM GeneratedFile gf "
            + "LEFT JOIN FETCH gf.generatedFileFormat.filter f "
            + "LEFT JOIN FETCH f.subfilters sf " // TOTO suspect: does Ofac need subfilter
            + "WHERE gf.id IN ?1")

    Set<GeneratedFile> getByIdsForConcatenatedList(List<Long> generatedFilesIds);

    @Query("SELECT gf.concatenatedList.id FROM GeneratedFile gf "
            + "WHERE gf.id IN ?1")
    Set<Long> findAllConcatenatedListIdByGeneratedFileIds(List<Long> generatedFilesIds);

}

