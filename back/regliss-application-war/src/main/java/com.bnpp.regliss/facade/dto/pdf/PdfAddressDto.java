package com.bnpp.regliss.facade.dto.pdf;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class PdfAddressDto {
    @Getter public String address;
    @Getter public String zipCode;
    @Getter public String city;
    @Getter public String state;
    @Getter public String remarks;
    @Getter public String countryLabel;

    public PdfAddressDto(AddressDto dto, String countryLabel) {
        this.address = dto.address;
        this.zipCode = dto.zipCode;
        this.city = dto.city;
        this.state = dto.state;
        this.remarks = dto.remarks;
        this.countryLabel = countryLabel;
    }
}

