package com.bnpp.regliss.entity;

// TODO: the project imports were not visible in the screenshots.
// Re-add them in the IDE (Alt+Enter / Optimize Imports) for: ListCharacteristicsDto

public interface ListCharacteristicsDto {

    public Long getId();
    public String getReference(); //should be number?
    public String getShortName();
    public String getName();
    public String getDescription();
    public RegulatorOrInternalPolicy getRegulatorOrInternalPolicy();
    public ReglissListType getType();
    public Confidentiality getConfidentiality();
    public Scope getScope();
    public Country getCountry();
    public Long getTerritory();
    public String getOwner();
    public Issuer getIssuer();
    public ListCategory getListCategory();
    public Boolean getActive();
    public Boolean getGlobalPlusOfac();
    public GeographicData getGeographicData();
}
