package com.bnpp.regliss.vo;

public enum AddModSupCountType {
    PRIMARY_NAME("Primary name"),
    ALIAS("Alias"),
    BIC("BIC code"),
    PASSPORT("Passport number"),
    ISIN("ISIN code"),
    PERSON("Individual"),
    ENTITY("Entity"),
    VESSEL("Vessel"),
    OTHER("Other"),
    COUNTRY("Country"),
    CITY("City"),
    SEAPORT("Seaport"),
    AIRPORT("Airport"),
    TOTAL("Total");


    public final String label;

    AddModSupCountType(String label) {
        this.label = label;
    }
}
