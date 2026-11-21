package com.bnpp.regliss.entity.referential.record;

public enum CodeType {
    // WARNING: The order that these enums appear is reflected in the UI (field combobox - Manual Import)
    ISIN("isinCodes"), BIC("bicCodes"), IMO("imo"), IBAN("ibanCodes"), CUSIP("cusipCodes"),
    // DO NOT REORDER this ENUM !!
    EXTERNAL_REFERENCE("externalReference"), REGLISS_REFERENCE("reglissReference"), RECORD_ID("recordId"), SEARCH_CODE("searchCodeNormalized");
    private String jpqlField;
    CodeType(String jpqlField) { this.jpqlField=jpqlField; }

    public String getJpql() { return jpqlField; }
}
