package com.bnpp.regliss.client.refog.jaxb;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "name",
    "value"
})
@XmlRootElement(name = "InnerField")
public class InnerField {

    @XmlElement(name = "Name", required = true)
    protected String name;
    @XmlElement(name = "Value", required = true)
    protected String value;
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

}
