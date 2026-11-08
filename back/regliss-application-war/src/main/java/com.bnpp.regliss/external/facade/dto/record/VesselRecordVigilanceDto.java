package com.bnpp.regliss.external.facade.dto.record;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class VesselRecordVigilanceDto{

    public String imo;
    public String flag;
    public String owner;
    public String vesselType;
    public String tonnage;
    public String grossTonnage;
    public String callSign;
    public List<DateDetailsVigilanceDto> datesDetails = new ArrayList<>();
    public List<CountryDetailsVigilanceDto> countryDetails = new ArrayList<>();
    public List<IdentityDocumentVigilanceDto> identityDocuments = new ArrayList<>();
    public List<AddressVigilanceDto> addresses = new ArrayList<>();

}

