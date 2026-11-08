package com.bnpp.regliss.facade.dto.record;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

public class EntityRecordDto{

    public Set<CodeIbanDto> iban = new HashSet<>();
    public Set<CodeBicDto> bic = new HashSet<>();
    public Set<CodeIsinDto> isin = new HashSet<>();
    public Set<CodeCusipDto> cusip = new HashSet<>();
    public List<AddressDto> addresses = new ArrayList<>();
    public List<DateDetailsDto> datesDetails = new ArrayList<>();
    public List<CountryDetailsDto> countryDetails = new ArrayList<>();
    public List<IdentityDocumentDto> identityDocuments = new ArrayList<>();

}

