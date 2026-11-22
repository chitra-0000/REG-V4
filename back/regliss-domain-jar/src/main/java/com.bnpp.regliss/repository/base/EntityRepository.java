package com.bnpp.regliss.repository.base;

import java.io.Serializable;

import com.bnpp.regliss.entity.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

@NoRepositoryBean
public interface EntityRepository<T extends Entity<ID>, ID extends Serializable> extends JpaRepository<T, ID> {

    T getReference(ID id);
    void detach(T oldRecord);

    T getExactlyOne(ID id);
}

