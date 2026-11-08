package com.bnpp.regliss.facade.dto.pdf;

import com.bnpp.regliss.facade.dto.record.AliasDto;
import lombok.Getter;

public class PdfAliasDto {
    @Getter public String reglissReference;
    @Getter public String externalReference;
    @Getter public String title;
    @Getter  public String firstName;
    @Getter public String middleName;
    @Getter public String lastName;
    @Getter public String type; //mappping
    @Getter public String quality; //mapping
    @Getter public String language;
    @Getter public String suffix;

    public PdfAliasDto(AliasDto o, String type, String quality) {
        this.reglissReference = o.reglissReference;
        this.externalReference = o.externalReference;
        this.title = o.title;
        this.firstName = o.firstName;
        this.middleName = o.middleName;
        this.lastName = o.lastName;
        this.type = type;
        this.quality = quality;
        this.language = o.language;
        this.suffix = o.suffix;
    }
}
