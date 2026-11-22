package com.bnpp.regliss.vo;

import lombok.Data;

@Data
public class AssociationExternalReference {

    private String externalReference;

    public AssociationExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

}
