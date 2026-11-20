package com.bnpp.regliss.repository.base;

import com.bnpp.regliss.entity.Entity;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EntityRepositoryImpl<T extends Entity<ID>, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements EntityRepository<T, ID> {

    protected EntityManager entityManager;

    public EntityRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager ;
    }

    @Override
    public T getReference(ID id) { return entityManager.getReference(getDomainClass(), id); }

    @Override
    public void detach(T entity) { entityManager.detach(entity); }

    /unchecked/
    public List<T> getByPrimaryKeys(Collection<ID> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        } else {
            return entityManager.createQuery("SELECT e FROM " + getDomainClass().getSimpleName() + " e WHERE e.id IN (:ids)")
                    .setParameter("ids", ids).getResultList();
        }
    }

    @Override
    public T getExactlyOne(ID id) {
        T entity = findById(id).orElse(null);
        if (entity == null) {
            throw new EntityNotFoundException("No " + getDomainClass().getSimpleName() + " with id " + id);
        }
        return entity;
    }

}

