package com.bnpp.regliss.client.refog.jaxb;

import javax.xml.bind.annotation.*;
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "name",
    "value",
    "values"
})
@XmlRootElement(name = "Field")
public class Field {
    @XmlElement(name = "Name", required = true)
    protected String name;
    @XmlElement(name = "Value")
    protected String value;
    @XmlElement(name = "Values")
    protected Values values;
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Values getValues() { return values; }
    public void setValues(Values value) { this.values = value; }
}
