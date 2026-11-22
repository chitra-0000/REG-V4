package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.BroadcastMessage;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.Optional;

public interface BroadcastMessageRepository extends EntityRepository<BroadcastMessage, Long> {

    Optional<BroadcastMessage> findByStatus(BroadcastMessage.Status status);
}

