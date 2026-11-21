package com.bnpp.regliss.six.exporter.generated;

import javax.xml.bind.annotation.*;
import lombok.*;

/**
 * JAXB-ready version of the XSD-generated ListType.
 * All Lombok-generated builder classes are hidden from JAXB.
 */
@XmlRootElement(name = "list")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "listType", propOrder = {
        "applicationDate",
        "juridictionBase",
        "label",
        "sourceVersion",
        "type",
        "records"
})
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ListType {

    @XmlElement(required = true)
    @XmlSchemaType(name = "date")
    protected String applicationDate;

    @XmlElement(required = true)
    protected String juridictionBase;

    @XmlElement(required = true)
    protected String label;

    @XmlElement(required = true)
    protected String sourceVersion;

    @XmlElement(required = true)
    protected String type;

    @XmlElement(required = true)
    protected RecordsType records;

    /** Hide Lombok’s static builder from JAXB */
    @XmlTransient
    public static class ListTypeBuilder { /* generated - empty */ }
}
