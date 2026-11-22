package com.bnpp.regliss.repository.batch;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.batch.ListImportNodeLock;
import com.bnpp.regliss.repository.base.EntityRepository;

import java.util.Collection;


public interface ListImportNodeLockRepository extends EntityRepository< ListImportNodeLock, Long>{

    ListImportNodeLock findByListAndNodeName(ReglissList list, String nodeName);
    ListImportNodeLock findByListIdAndNodeName(Long listId, String nodeName);

    long countByListIdIn(Collection<Long> listIds);

}

