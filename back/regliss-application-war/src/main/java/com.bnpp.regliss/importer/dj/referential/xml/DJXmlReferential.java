package com.bnpp.regliss.importer.dj.referential.xml;

import lombok.Data;

@Data
public class DJXmlReferential {

    private String code;
    private String value;

    public DJXmlReferential(String code, String value) {
        this.code = code;
        this.value = value;
    }

    public DJXmlReferential() {
    }
}

