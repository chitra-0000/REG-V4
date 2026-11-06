package com.bnpp.regliss.client.refog.jaxb;

import javax.xml.bind.annotation.*;   // TODO verify: lines 1-4 were not in the photo

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "statusCode",
    "description"
})
@XmlRootElement(name = "status")
public class Status {

    @XmlElement(name = "StatusCode", required = true)
    protected String statusCode;
    @XmlElement(name = "Description3", required = true)
    protected DescriptionCode description;
    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String value) { this.statusCode = value; }
    public DescriptionCode getDescription() { return description; }
    public void setDescription(DescriptionCode value) { this.description = value; }

}
