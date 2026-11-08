package com.bnpp.regliss.facade.dto.version;

// [MISSING FROM SCREENSHOTS: lines 3-5 of the original file were not visible in the photos]

public class BaseReglementationDto {

    public Long id;
    public ReglementationType type;
    public String title;
    public String reference;
    public LocalDate officialDate;

    public BaseReglementationDto(long id) { this.id = id; }

    public BaseReglementationDto() {
    }
}

