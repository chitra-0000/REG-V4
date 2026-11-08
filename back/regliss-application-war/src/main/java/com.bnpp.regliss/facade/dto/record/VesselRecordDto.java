package com.bnpp.regliss.facade.dto.record;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class VesselRecordDto{

    public String imo;
    public String flag;
    public String owner;
    public String vesselType;
    public String tonnage;
    public String grossTonnage;
    public String callSign;
    public List<DateDetailsDto> datesDetails = new ArrayList<>();
    public List<CountryDetailsDto> countryDetails = new ArrayList<>();
    public List<IdentityDocumentDto> identityDocuments = new ArrayList<>();
    public List<AddressDto> addresses = new ArrayList<>();

}

