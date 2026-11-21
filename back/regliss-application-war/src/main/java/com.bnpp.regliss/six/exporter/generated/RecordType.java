// [MISSING FROM SCREENSHOTS: lines 1-6 of the original file were not visible in the photos]


package com.bnpp.regliss.six.exporter.generated;

import javax.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * <p>Java class for recordType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="recordType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="generalInformation" type="{}generalInformationType"/&gt;
 *         &lt;element name="extraMigrationIDRef" type="{}extraMigrationIDRefType" minOccurs="0"/&gt;
 *         &lt;element name="extraIndividualRecordDetail" type="{}extraIndividualRecordDetailType" minOccurs="0"/&gt;
 *         &lt;element name="extraEntityRecordDetail" type="{}extraEntityRecordDetailType" minOccurs="0"/&gt;
 *         &lt;element name="extraVesselRecordDetail" type="{}extraVesselRecordDetailType" minOccurs="0"/&gt;
 *         &lt;element name="extraOtherRecordDetail" type="{}extraOtherRecordDetailType" minOccurs="0"/&gt;
 *         &lt;element name="extraCountryRecordDetail" type="{}extraCountryRecordDetailType" minOccurs="0"/&gt;
 *         &lt;element name="extraCityRecordDetail" type="{}extraCityRecordDetailType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="applicationDate" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="deletionDate" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="externalReference" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "recordType", propOrder = {
    "generalInformation",
    "extraEntityRecordDetail",
    "extraOtherRecordDetail"
})
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordType {

    @XmlElement(required = true)
    protected GeneralInformationType generalInformation;
    protected ExtraEntityRecordDetailType extraEntityRecordDetail;
    protected ExtraOtherRecordDetailType extraOtherRecordDetail;
    @XmlAttribute(name = "applicationDate")
    protected String applicationDate;
    @XmlAttribute(name = "deletionDate")
    protected String deletionDate;
    @XmlAttribute(name = "externalReference")
    protected String externalReference;

    /**
     * Gets the value of the generalInformation property.
     *
     * @return
     *     possible object is
     *     {@link GeneralInformationType }
     *
     */
    public GeneralInformationType getGeneralInformation() {
        return generalInformation;
    }

    /**
     * Sets the value of the generalInformation property.
     *
     * @param value
     *     allowed object is
     *     {@link GeneralInformationType }
     *
     */
    public void setGeneralInformation(GeneralInformationType value) {
        this.generalInformation = value;
    }


    /**
     * Gets the value of the extraEntityRecordDetail property.
     *
     * @return
     *     possible object is
     *     {@link ExtraEntityRecordDetailType }
     *
     */
    public ExtraEntityRecordDetailType getExtraEntityRecordDetail() {
        return extraEntityRecordDetail;
    }

    /**
     * Sets the value of the extraEntityRecordDetail property.
     *
     * @param value
     *     allowed object is
     *     {@link ExtraEntityRecordDetailType }
     *
     */
    public void setExtraEntityRecordDetail(ExtraEntityRecordDetailType value) {
        this.extraEntityRecordDetail = value;
    }


    /**
     * Gets the value of the extraOtherRecordDetail property.
     *
     * @return
     *     possible object is
     *     {@link ExtraOtherRecordDetailType }
     *
     */
    public ExtraOtherRecordDetailType getExtraOtherRecordDetail() {
        return extraOtherRecordDetail;
    }

    /**
     * Sets the value of the extraOtherRecordDetail property.
     *
     * @param value
     *     allowed object is
     *     {@link ExtraOtherRecordDetailType }
     *
     */

    /**
     * Gets the value of the applicationDate property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getApplicationDate() {
        return applicationDate;
    }

    /**
     * Sets the value of the applicationDate property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setApplicationDate(String value) {
        this.applicationDate = value;
    }

    /**
     * Gets the value of the deletionDate property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDeletionDate() {
        return deletionDate;
    }

    /**
     * Sets the value of the deletionDate property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDeletionDate(String value) {
        this.deletionDate = value;
    }

    /**
     * Gets the value of the externalReference property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getExternalReference() {
        return externalReference;
    }

    /**
     * Sets the value of the externalReference property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setExternalReference(String value) {
        this.externalReference = value;
    }

}

