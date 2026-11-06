package com.bnpp.regliss.client.refog.jaxb;

// imports (lines 3-5) were folded in the IDE; reconstructed from usage
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "parameter"
})
@XmlRootElement(name = "parameters")
public class Parameters {

    @XmlElement(name = "Parameter", required = true)
    protected List<Parameter> parameter;

    public List<Parameter> getParameter() {
        if (parameter == null) {
            parameter = new ArrayList<>();
        }
        return this.parameter;
    }

}
