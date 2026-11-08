package com.bnpp.regliss.external.facade.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AddressVigilanceDto {
    @JsonIgnore
    public Long id;
    public String address;
    public String zipCode;
    public String city;
    public String state;
    public String countryLabel;
    public String remarks;
}

