package com.bnpp.regliss.facade.dto.list;

public class ReglissListsForConcListsDto {

    private long listId;
    private String reference;
    private String shortName;
    private String name;
    private String listName;

    public ReglissListsForConcListsDto(Long listId, String reference, String shortName, String name){
        this.listId = listId;
        this.reference = reference;
        this.shortName = shortName;
        this.name = name;
        this.listName = reference + "-" + shortName;
    }
    public long getListId() { return listId; }
    public String getReference() { return reference; }
    public String getShortName() { return shortName; }
    public String getName() { return name; }
    public String getListName() { return listName; }
}

