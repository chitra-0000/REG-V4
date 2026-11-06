package com.bnpp.regliss.client.refog.jaxb;

// imports (lines 3-5) were folded in the IDE; reconstructed from usage
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "content"
})
@XmlRootElement(name = "Value")
public class Value {

    @XmlElementRef(name = "InnerField", type = InnerField.class)
    @XmlMixed
    protected List<Object> content;
    public List<Object> getContent() {
        if (content == null) {
            content = new ArrayList<>();
        }
        return this.content;
    }

}
