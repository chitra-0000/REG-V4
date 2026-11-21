package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.CaseInsensitive;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.validation.WarningConstraint;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.validation.constraints.Size;

@Entity
@DiscriminatorValue(value = RecordType.DiscriminatorValues.VESSEL)
public class VesselRecord extends Record implements AddressContainer{

    @Size(min = 7, max = 7, message = "{vessel.imo.size}", groups = WarningConstraint.class)
    @EqualityField
    @Getter @Setter
    @AuditedField(AuditedFieldCode.VESSEL_IMO)
    @Column(name = "IMO", length = 36)
    private String imo;

    @EqualityField
    @Getter @Setter
    @AuditedField(AuditedFieldCode.VESSEL_FLAG)
    @Column(name = "FLAG", length = 64)
    private String flag;

    @EqualityField
    @Getter @Setter
    @AuditedField(AuditedFieldCode.VESSEL_OWNER)
    @Column(name = "OWNER", length = 300)
    private String owner;

    @EqualityField
    @Getter @Setter
    @AuditedField(AuditedFieldCode.VESSEL_TYPE)
    @Column(name = "VESSEL_TYPE", length = 250)
    private String vesselType;

    @EqualityField
    @Getter @Setter
    @AuditedField(AuditedFieldCode.VESSEL_TONNAGE)
    @Column(name = "TONNAGE", length = 64)
    private String tonnage;

    @EqualityField
    @Getter @Setter
    @AuditedField(AuditedFieldCode.VESSEL_GROSS_TONNAGE)
    @Column(name = "GROSS_TONNAGE", length = 64)
    private String grossTonnage;

    @EqualityField
    @Getter @Setter
    @AuditedField(AuditedFieldCode.VESSEL_CALL_SIGN)
    @Column(name = "VESSEL_CALL_SIGN", length = 64)
    private String callSign;

    public VesselRecord() { super(RecordType.VESSEL); }

    public VesselRecord(VesselRecord other) {
        super(other);
        this.flag = other.flag;
        this.grossTonnage = other.grossTonnage;
        this.imo = other.imo;
        this.owner = other.owner;
        this.tonnage = other.tonnage;
        this.vesselType = other.vesselType;

        for (IdentityDocument otherIdentityDocument: other.identityDocuments) {
            addIdentityDocument(new IdentityDocument(otherIdentityDocument));
        }

        for (Address otherAddress: other.addresses) {
            addAddress(new Address(otherAddress));
        }
    }
}
