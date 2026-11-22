package com.bnpp.regliss.vo;

public enum AddModSupElementType {
    PRIMARY_NAME("Primary name"),
    ALIAS_STRONG("Alias (STRONG)"),
    ALIAS_WEAK("Alias (WEAK)"),
    ALIAS_WEAK_REC("Alias (WEAK-REC)"),
    ALIAS_OTHER("Alias (NOT_MATCHED)"),
    PASSPORT("Passport number"),
    BIC("BIC code"),
    ISIN("ISIN code"),
    CITIZENSHIP("Citizenship"),
    ADDRESS("Address(City &/or Country)"),
    DATE_OF_BIRTH("Date of birth"),
    DATE_OF_DEATH("Date of death"),
    PLACE_OF_BIRTH("Place of birth");

    public final String label;

    AddModSupElementType(String label) {
        this.label = label;
    }
}
