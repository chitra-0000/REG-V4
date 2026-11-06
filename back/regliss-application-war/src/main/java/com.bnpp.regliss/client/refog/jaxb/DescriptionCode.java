package com.bnpp.regliss.client.refog.jaxb;

// imports (lines 3-4) were folded in the IDE; reconstructed from usage
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


@XmlType(name = "descriptionCode")
@XmlEnum
public enum DescriptionCode {

    OK,
    KO;

    public String value() { return name(); }

    public static DescriptionCode fromValue(String v) { return valueOf(v); }

}
