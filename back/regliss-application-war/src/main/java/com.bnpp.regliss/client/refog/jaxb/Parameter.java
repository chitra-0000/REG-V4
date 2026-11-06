package com.bnpp.regliss.client.refog.jaxb;
// imports (lines 2-4) were folded in the IDE; reconstructed from usage
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "name",
    "value"
})
@XmlRootElement(name = "Parameter")
public class Parameter {
    @XmlElement(required = true)
    protected String name;
    @XmlElement(required = true)
    protected String value;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getId() { return id; }
    public void setId(String value) { this.id = value; }
}
