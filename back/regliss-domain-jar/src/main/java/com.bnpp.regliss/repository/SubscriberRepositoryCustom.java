package com.bnpp.regliss.repository;

import java.util.List;
import java.util.Map;

import com.bnpp.regliss.entity.FileDelivery.Status;
import com.bnpp.regliss.entity.Subscriber;
import com.bnpp.regliss.search.SubscriberSearchCriteria;

public interface SubscriberRepositoryCustom {

    List<Subscriber> search(SubscriberSearchCriteria criteria);

    Map<Long, Status> getAggregateStatuses(List<Long> subscriberIds);

}

