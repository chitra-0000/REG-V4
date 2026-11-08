package com.bnpp.regliss.facade.dto;

import java.time.LocalDateTime;

public class AllListsIncludedInConcatenatedList {
    public String name;
    public LocalDateTime lastPublicationDate;

    public AllListsIncludedInConcatenatedList(String name, LocalDateTime lastPublicationDate){
        this.name = name;
        this.lastPublicationDate = lastPublicationDate;
    }
}

