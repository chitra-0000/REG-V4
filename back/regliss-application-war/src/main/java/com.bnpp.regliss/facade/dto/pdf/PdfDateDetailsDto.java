package com.bnpp.regliss.facade.dto.pdf;

// [MISSING FROM SCREENSHOTS: lines 3-5 of the original file were not visible in the photos]

public class PdfDateDetailsDto {
    @Getter public String dateValue;
    @Getter public String dateTypeLabel;
    @Getter public String birthPlace;
    @Getter public String countryLabel;
    @Getter public String additionalInfo;

    public PdfDateDetailsDto(DateDetailsDto o, String dateTypeLabel) {
        this.dateValue = o.dateValue;
        this.dateTypeLabel = dateTypeLabel;
        this.birthPlace = "";
        this.countryLabel = "";
        this.additionalInfo = "";
    }

    public PdfDateDetailsDto(PersonBiographyDto o, String dateTypeLabel, String countryLabel) {
        this.dateValue = o.dateValue;
        this.dateTypeLabel = dateTypeLabel;
        this.birthPlace = o.birthPlace;
        this.countryLabel = countryLabel;
        this.additionalInfo = o.additionalInfo;
    }
}

