package com.bnpp.regliss.six.exporter.generated;

import javax.xml.bind.annotation.*;
import lombok.*;

@XmlRootElement(name = "extraEntityRecordDetail") // optional, but handy
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "extraEntityRecordDetailType", propOrder = {
        "addresses",
        "identityDocuments",
        "countryDetails",
        "datesDetails",
        "aliases",
        "codes",
        "relations"
})
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExtraEntityRecordDetailType {

    @XmlElement(required = true) protected AddressesType addresses;
    @XmlElement(required = true) protected IdentityDocumentsType identityDocuments;
    @XmlElement(required = true) protected CountryDetailsType countryDetails;
    @XmlElement(required = true) protected DatesDetailsType datesDetails;
    @XmlElement(required = true) protected AliasesType aliases;
    @XmlElement(required = true) protected CodesType codes;
    @XmlElement(required = true) protected RelationsType relations;

    @XmlTransient
    public static class ExtraEntityRecordDetailTypeBuilder { }
}
