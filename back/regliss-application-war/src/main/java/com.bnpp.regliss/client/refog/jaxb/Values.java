package com.bnpp.regliss.client.refog.jaxb;

// imports (lines 3-5) were folded in the IDE; reconstructed from usage
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "value"
})
@XmlRootElement(name = "Values")
public class Values {

    @XmlElement(name = "Value")
    protected List<Value> value;

    public List<Value> getValue() {
        if (value == null) {
            value = new ArrayList<>();
        }
        return this.value;
    }

}
