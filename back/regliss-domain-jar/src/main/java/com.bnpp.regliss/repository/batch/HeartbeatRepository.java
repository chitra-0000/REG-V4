package com.bnpp.regliss.repository.batch;

import com.bnpp.regliss.entity.batch.Heartbeat;
import com.bnpp.regliss.entity.batch.CftType;
import com.bnpp.regliss.repository.base.EntityRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HeartbeatRepository extends EntityRepository<Heartbeat, Long> {

    Optional<Heartbeat> findByNodeId(@Param("nodeId") String nodeId);

    List<Heartbeat> findByCftTypeIn(@Param("types") Collection<CftType> types);
}

