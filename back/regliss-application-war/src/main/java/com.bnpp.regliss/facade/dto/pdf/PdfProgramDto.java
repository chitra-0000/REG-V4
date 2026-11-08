package com.bnpp.regliss.facade.dto.pdf;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class PdfProgramDto {
    @Getter public String label;
    @Getter public String sanctionType; //mapping
    @Getter public String description;

    public PdfProgramDto(ProgramDto o, String sanctionType) {
        this.label = o.label;
        //reference.programSanctionType_148
        this.sanctionType = sanctionType;
        this.description = o.description;
    }
}

