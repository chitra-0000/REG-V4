package com.bnpp.regliss.client.refog.jaxb;

// imports (lines 3-5) were folded in the IDE; reconstructed from usage
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "field"
})
@XmlRootElement(name = "fields")
public class Fields {

    @XmlElement(name = "Field", required = true)
    protected List<Field> field;

    public List<Field> getField() {
        if (field == null) {
            field = new ArrayList<>();
        }
        return this.field;
    }

}
