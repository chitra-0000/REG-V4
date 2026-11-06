package com.bnpp.regliss.client.refog.jaxb;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "fields"
})
@XmlRootElement(name = "RefogObject")
public class RefogObject {

    @XmlElement(required = true)
    protected Fields fields;
    public Fields getFields() { return fields; }
    public void setFields(Fields value) { this.fields = value; }

}
