package com.bnpp.regliss.client.refog.jaxb;

import javax.xml.bind.annotation.*;
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "status",
    "errors",
    "info",
    "parameters",
    "data"
})
@XmlRootElement(name = "RefogSearch")
public class RefogSearch {

    @XmlElement(required = true)
    protected Status status;
    protected Errors errors;
    @XmlElement(required = true)
    protected Info info;
    @XmlElement(required = true)
    protected Parameters parameters;
    protected Data data;
    public Status getStatus() { return status; }
    public void setStatus(Status value) { this.status = value; }
    public Errors getErrors() { return errors; }
    public void setErrors(Errors value) { this.errors = value; }
    public Info getInfo() { return info; }
    public void setInfo(Info value) { this.info = value; }
    public Parameters getParameters() { return parameters; }
    public void setParameters(Parameters value) { this.parameters = value; }
    public Data getData() { return data; }
    public void setData(Data value) { this.data = value; }

}
