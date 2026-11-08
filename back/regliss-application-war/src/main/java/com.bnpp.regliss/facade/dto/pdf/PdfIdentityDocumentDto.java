package com.bnpp.regliss.facade.dto.pdf;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class PdfIdentityDocumentDto {
    @Getter public String typeLabel;
    @Getter public String number;
    @Getter public String countryLabel;
    @Getter public String issuingDate;
    @Getter public String expirationDate;
    @Getter public String additionalInfo;

    public PdfIdentityDocumentDto(IdentityDocumentDto o, String typeLabel, String countryLabel) {
        this.typeLabel = typeLabel;
        this.number = o.number;
        this.countryLabel = countryLabel;
        this.issuingDate = o.issuingDate;
        this.expirationDate = o.expirationDate;
        this.additionalInfo = o.additionalInfo;
    }
}

