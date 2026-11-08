package com.bnpp.regliss.external.facade.dto.record;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

public class PersonRecordVigilanceDto {

    public List<String> gendersLabel = new ArrayList<>();
    public String title;
    public String firstName;
    public String middleName;
    public String lastName;
    public String maidenName;
    public String suffix;
    public Boolean isDeceased;

    public PersonCodesVigilanceDto codes;
    public Set<PersonImageVigilanceDto> personImages = new HashSet<>();
    public List<AddressVigilanceDto> addresses = new ArrayList<>();
    public List<IdentityDocumentVigilanceDto> identityDocuments = new ArrayList<>();
    public List<CountryDetailsVigilanceDto> countryDetails = new ArrayList<>();
    public List<PersonBiographyVigilanceDto> biography = new ArrayList<>();
    public List<PersonFunctionVigilanceDto> personFunctions =  new ArrayList<>();

}

