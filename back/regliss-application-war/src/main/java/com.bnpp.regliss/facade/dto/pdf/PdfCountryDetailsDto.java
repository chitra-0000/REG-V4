package com.bnpp.regliss.facade.dto.pdf;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class PdfCountryDetailsDto {
    @Getter public String typeLabel;
    @Getter public String countryLabel;
    @Getter public Boolean principal;
    @Getter public String additionalInfo;

    public PdfCountryDetailsDto(CountryDetailsDto o, String typeLabel, String countryLabel) {
        this.typeLabel = typeLabel;
        this.countryLabel = countryLabel;
        this.principal = o.principal != null ? o.principal : false;
        this.additionalInfo = o.additionalInfo;
    }
}

