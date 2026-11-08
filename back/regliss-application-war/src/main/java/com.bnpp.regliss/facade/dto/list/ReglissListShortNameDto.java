package com.bnpp.regliss.facade.dto.list;

public class ReglissListShortNameDto {

    public String listName;
    public long listId;
    public ReglissListShortNameDto(String listShortName, Long listId){
        this.listName = listShortName;
        this.listId=listId;
    }
}

