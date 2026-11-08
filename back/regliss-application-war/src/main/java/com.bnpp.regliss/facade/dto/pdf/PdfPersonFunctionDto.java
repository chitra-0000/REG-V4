package com.bnpp.regliss.facade.dto.pdf;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class PdfPersonFunctionDto {
    @Getter public String pepOccupationCategory;
    @Getter public String functionName;
    @Getter public String from;
    @Getter public String to;

    public PdfPersonFunctionDto(PersonFunctionDto o, String pepOccupationCategory) {
        this.pepOccupationCategory = pepOccupationCategory;
        this.functionName = o.functionName;
        this.from = o.from;
        this.to = o.to;
    }
}

