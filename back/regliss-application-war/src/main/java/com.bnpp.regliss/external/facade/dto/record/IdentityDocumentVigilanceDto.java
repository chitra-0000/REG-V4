package com.bnpp.regliss.external.facade.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class IdentityDocumentVigilanceDto {
    @JsonIgnore
    public Long id;
    public String typeLabel;
    public String number;
    public String countryLabel;
    public String issuingDate;
    public String expirationDate;
    public String additionalInfo;

}

