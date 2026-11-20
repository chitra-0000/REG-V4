package com.bnpp.regliss.repository;

import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;

public abstract class BaseRepositoryImpl<T> {
    @Autowired
    protected EntityManager entityManager;

}
