package com.bnpp.regliss.client.refog.jaxb;

// imports (lines 3-5) were folded in the IDE; reconstructed from usage (javax.xml.bind.* on Boot 2, jakarta on Boot 3)
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{}RefogObject" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
// TODO verify: lines 28-33 not in the photos; standard xjc output assumed below
@XmlType(name = "", propOrder = {
    "refogObject"
})
@XmlRootElement(name = "Data")
public class Data {

    @XmlElement(name = "RefogObject")
    protected List<RefogObject> refogObject;

    /**
     * Gets the value of the refogObject property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the refogObject property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRefogObject().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RefogObject }
     *
     *
     */
    public List<RefogObject> getRefogObject() {
        if (refogObject == null) {
            refogObject = new ArrayList<>();
        }
        return this.refogObject;
    }

}
