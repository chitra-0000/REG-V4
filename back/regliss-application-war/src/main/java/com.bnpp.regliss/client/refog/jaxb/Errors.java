package com.bnpp.regliss.client.refog.jaxb;

// imports (lines 3-5) were folded in the IDE; reconstructed from usage
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "error"
})
@XmlRootElement(name = "errors")
public class Errors {

    @XmlElement(name = "Error", required = true)
    protected List<Error> error;

    public List<Error> getError() {
        if (error == null) {
            error = new ArrayList<>();
        }
        return this.error;
    }

}
