package com.bnpp.regliss.entity;

import java.io.Serializable;

public interface Entity<ID extends Serializable> {
    ID getId();
    Object setId(ID id); //Lombok is set to use setters that return <this> so we return Object instead of void to overcome this
}
