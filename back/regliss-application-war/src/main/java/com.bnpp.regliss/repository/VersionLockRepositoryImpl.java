package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.VersionLock;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VersionLockRepositoryImpl extends BaseRepositoryImpl<VersionLock> implements VersionLockRepositoryCustom {
    /** @Autowired
    private EntityManager entityManager;*/

    private static final String MAIN_VERION_LOCK_QUERY=" select * from version_lock  vl1\n"+
        "join versionids ids on vl1.version_id=ids.id\n"+
        "and vl1.acquire_date=(select max(vl2.acquire_date) from version_lock vl2 join  versionids ids2 on vl2.version_id=ids2.id)";

    @Override
    public List<VersionLock> findLastLock(Set<Long> versionIds) {
        String query = constructWithClause(versionIds)+"\n"+MAIN_VERION_LOCK_QUERY;
        return entityManager.createNativeQuery(query,VersionLock.class).getResultList();
    }

    public static String constructWithClause(Set<Long> versionIds){
        return "with versionids as ( "+versionIds.stream().map(id->" select "+id+" as id from dual").collect(Collectors.joining(" union all "))+" ) ";
    }
}
