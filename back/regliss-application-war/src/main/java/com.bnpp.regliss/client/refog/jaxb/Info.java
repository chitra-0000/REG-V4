package com.bnpp.regliss.client.refog.jaxb;

// imports (lines 3-5) were folded in the IDE; reconstructed from usage
import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "serviceName",
    "requestDate",
    "requestDuration",
    "resultObjects"
})
@XmlRootElement(name = "info")
public class Info {
    @XmlElement(name = "ServiceName", required = true)
    protected String serviceName;
    @XmlElement(name = "RequestDate", required = true)
    protected XMLGregorianCalendar requestDate;
    @XmlElement(name = "RequestDuration", required = true)
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger requestDuration;
    @XmlElement(name = "ResultObjects", required = true)
    protected BigInteger resultObjects;
    public String getServiceName() { return serviceName; }
    public void setServiceName(String value) { this.serviceName = value; }
    public XMLGregorianCalendar getRequestDate() { return requestDate; }
    public void setRequestDate(XMLGregorianCalendar value) { this.requestDate = value; }
    public BigInteger getRequestDuration() { return requestDuration; }
    public void setRequestDuration(BigInteger value) { this.requestDuration = value; }
    public BigInteger getResultObjects() { return resultObjects; }
    public void setResultObjects(BigInteger value) { this.resultObjects = value; }
}
