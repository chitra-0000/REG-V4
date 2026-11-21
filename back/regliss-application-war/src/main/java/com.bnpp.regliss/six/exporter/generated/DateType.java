// [MISSING FROM SCREENSHOTS: lines 1-6 of the original file were not visible in the photos]


package com.bnpp.regliss.six.exporter.generated;

import com.bnpp.regliss.six.exporter.generated.adapter.BooleanIntAdapter;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * <p>Java class for dateType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="dateType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="isPrimary" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="type" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="dateValue" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="placeValue" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="country" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="dateAdditionalInfo" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="id"&gt;
 *         &lt;simpleType&gt;
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *             &lt;maxLength value="100"/&gt;
 *           &lt;/restriction&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dateType", propOrder = {
    "isPrimary",
    "type",
    "dateValue",
    "placeValue",
    "country",
    "dateAdditionalInfo"
})
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DateType {

    @XmlJavaTypeAdapter(BooleanIntAdapter.class)
    protected Boolean isPrimary;
    @XmlElement(required = true)
    protected String type;
    @XmlElement(required = true)
    protected String dateValue;
    @XmlElement(required = true)
    protected String placeValue;
    @XmlElement(required = true)
    protected String country;
    @XmlElement(required = true)
    protected String dateAdditionalInfo;
    @XmlAttribute(name = "id")
    protected String id;

    /**
     * Gets the value of the isPrimary property.
     *
     * @return
     *     possible object is
     *     {@link Boolean }
     *
     */
    public Boolean isPrimary() {
        return isPrimary;
    }

    /**
     * Sets the value of the isPrimary property.
     *
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *
     */
    public void setIsPrimary(Boolean value) {
        this.isPrimary = value;
    }

    /**
     * Gets the value of the type property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the dateValue property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDateValue() {
        return dateValue;
    }

    /**
     * Sets the value of the dateValue property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDateValue(String value) {
        this.dateValue = value;
    }

    /**
     * Gets the value of the placeValue property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getPlaceValue() {
        return placeValue;
    }

    /**
     * Sets the value of the placeValue property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setPlaceValue(String value) {
        this.placeValue = value;
    }

    /**
     * Gets the value of the country property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the value of the country property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCountry(String value) {
        this.country = value;
    }

    /**
     * Gets the value of the dateAdditionalInfo property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDateAdditionalInfo() {
        return dateAdditionalInfo;
    }

    /**
     * Sets the value of the dateAdditionalInfo property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDateAdditionalInfo(String value) {
        this.dateAdditionalInfo = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setId(String value) {
        this.id = value;
    }

}

