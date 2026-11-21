// [MISSING FROM SCREENSHOTS: lines 1-6 of the original file were not visible in the photos]


package com.bnpp.regliss.six.exporter.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * <p>Java class for extraOtherRecordDetailType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="extraOtherRecordDetailType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="aliases" type="{}aliasesType"/&gt;
 *         &lt;element name="category" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "extraOtherRecordDetailType", propOrder = {
    "aliases",
    "category"
})
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExtraOtherRecordDetailType {

    @XmlElement(required = true)
    protected AliasesType aliases;
    @XmlElement(required = true)
    protected String category;

    /**
     * Gets the value of the aliases property.
     *
     * @return
     *     possible object is
     *     {@link AliasesType }
     *
     */
    public AliasesType getAliases() {
        return aliases;
    }

    /**
     * Sets the value of the aliases property.
     *
     * @param value
     *     allowed object is
     *     {@link AliasesType }
     *
     */
    public void setAliases(AliasesType value) {
        this.aliases = value;
    }

    /**
     * Gets the value of the category property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the value of the category property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCategory(String value) {
        this.category = value;
    }

}

