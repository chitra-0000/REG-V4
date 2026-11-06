package com.bnpp.regliss.client.refog.jaxb;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "code",
    "message"
})
@XmlRootElement(name = "Error")
public class Error {

    @XmlElement(name = "Code", required = true)
    protected String code;
    @XmlElement(name = "Message", required = true)
    protected String message;

    public String getCode() { return code; }
    public void setCode(String value) { this.code = value; }
    public String getMessage() { return message; }
    public void setMessage(String value) { this.message = value; }

}
