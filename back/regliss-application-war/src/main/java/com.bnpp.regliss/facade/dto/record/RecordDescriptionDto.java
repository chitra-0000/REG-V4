package com.bnpp.regliss.facade.dto.record;


// [MISSING FROM SCREENSHOTS: lines 4-5 of the original file were not visible in the photos]

public  class RecordDescriptionDto {
    public Long id;
    public String label;
    public List<RecordDescriptionDto> descriptions = new ArrayList<>();
}

