package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.Subscriber;
import com.bnpp.regliss.repository.base.EntityRepository;
import com.bnpp.regliss.vo.SubscriberBreadcrumbVO;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SubscriberRepository extends EntityRepository<Subscriber, Long>, SubscriberRepositoryCustom {

    int countByCftFluxNameIgnoreCase(String cftFluxName);

    Subscriber findByCftFluxName(String cftFluxName);


    @Query("SELECT s.id as id, s.name as name FROM Subscriber s WHERE s.id=?1")
    Optional<SubscriberBreadcrumbVO> getBreadcrumbForSubscriber(long subscriberId);


}

