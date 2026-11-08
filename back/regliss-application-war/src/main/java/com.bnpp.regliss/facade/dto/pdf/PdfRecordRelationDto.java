package com.bnpp.regliss.facade.dto.pdf;


// [MISSING FROM SCREENSHOTS: lines 4-5 of the original file were not visible in the photos]

public class PdfRecordRelationDto {
    @Getter public String category;
    @Getter public String name;
    @Getter public String typeLabel;
    @Getter public Boolean ex;
    @Getter public String relationTypeLabel;

    public PdfRecordRelationDto(RecordRelationDto o, String category, String typeLabel, String relationTypeLabel) {
        this.category = category;
        this.name = o.name;
        this.typeLabel = typeLabel;
        this.ex = o.ex != null ? o.ex : false;
        this.relationTypeLabel = relationTypeLabel;
    }
}

