package com.bnpp.regliss.repository;

import java.util.List;
import java.util.Set;

import com.bnpp.regliss.entity.*;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.bnpp.regliss.repository.base.EntityRepository;

import javax.transaction.Transactional;

public interface SubscriptionRepository extends EntityRepository<Subscription, Long> {

    @Query("SELECT DISTINCT s "
            + "FROM Subscription s "
            + "LEFT JOIN FETCH s.subscriber sub "
            + "WHERE s.list = ?1 "
            + "AND s.deleted = false "
            + "AND s.subscriber.active = true")
    Set<Subscription> findListSubscriptions(ReglissList list);

    @Query("SELECT DISTINCT s "
            + "FROM Subscription s "
            + "LEFT JOIN FETCH s.subscriber sub "
            + "WHERE s.list is NULL "
            + "AND s.deleted = false "
            + "AND s.subscriber.active = true")
    Set<Subscription> findOfacSubscriptions();

    @Query("SELECT DISTINCT s "
            + "FROM Subscription s "
            + "LEFT JOIN FETCH s.subscriber sub "
            + "WHERE s.concatenatedList = ?1 "
            + "AND s.deleted = false "
            + "AND s.subscriber.active = true")
    Set<Subscription> findConcatenatedListSubscriptions(ConcatenatedList concatenatedList);

    @Query("FROM Subscription s "
            + "WHERE s.generatedFileFormat.filter = ?1 "
            + "AND s.deleted = false "
            + "AND s.subscriber.active = true")
    List<Subscription> findByFilter(DJExportFilter filter);

    @Query("SELECT s.list "
            + "FROM Subscription s "
            + "WHERE s.deleted = false "
            + "AND s.list != null "
            + "AND s.subscriber = ?1 "
            + "AND (select count(*) from s.list.versions v where v.publicationDate!=null)>0")
    Set<ReglissList> findActiveSubscribedListsForSubscriber(Subscriber subscriber);

    @Transactional
    @Modifying
    @Query("UPDATE Subscription s SET s.deleted=true WHERE s.subscriber.id= ?1 AND s.id IN ?2")
    void updateDeletedStatusOfSubscriptionsForSubscriber(Long subscriberId, List<Long> subscriptionIds);

}

