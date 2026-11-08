package com.bnpp.regliss.facade.dto.record;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

public class PersonRecordDto {

    public List<Long> genders = new ArrayList<>();
    public String title;
    public String firstName;
    public String middleName;
    public String lastName;
    public String maidenName;
    public String suffix;
    public Boolean isDeceased;

    public Set<CodeIbanDto> iban = new HashSet<>();
    public Set<PersonImageDto> personImages = new HashSet<>();
    public List<AddressDto> addresses = new ArrayList<>();
    public List<IdentityDocumentDto> identityDocuments = new ArrayList<>();
    public List<CountryDetailsDto> countryDetails = new ArrayList<>();
    public List<PersonBiographyDto> biography = new ArrayList<>();
    public List<PersonFunctionDto> personFunctions = new ArrayList<>();

}

