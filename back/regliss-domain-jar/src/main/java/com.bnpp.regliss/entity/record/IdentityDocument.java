package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.CaseInsensitive;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.entity.referential.record.IdentityDocumentType;
import com.bnpp.regliss.utils.FormatDateUtil;
import com.bnpp.regliss.validation.WarningConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "IDENTITY_DOCUMENT")
@CaseInsensitive
/**@SequenceGenerator(name = "IdentityDocumentIdGenerator", initialValue = 100, sequenceName = "IDENT_DOC_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "IdentityDocumentIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "IDENT_DOC_SEQ") } )
public class IdentityDocument extends AbstractSimpleEntity implements ToStringAuditable, RecordAwareChild{

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "IdentityDocumentIdGenerator")
    protected Long id;

    @NotNull(message="{identityDocument.type.mandatory}", groups = WarningConstraint.class)
    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_TYPE)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "IDENTITY_DOCUMENT_TYPE_ID")
    private IdentityDocumentType type;

    @NotEmpty(message="{identityDocument.number.mandatory}", groups = WarningConstraint.class)
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_NUMBER)
    @EqualityField
    @Column(name = "IDENTITY_DOCUMENT_NUMBER", length = 128)
    private String number;

    @Setter
    @AuditedField(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_COUNTRY)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "COUNTRY")
    private Country country;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_ISSUING_DATE)
    @EqualityField
    @Column(name = "ISSUING_DATE", length = 255)
    private String issuingDateStr;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_EXPIRATION_DATE)
    @EqualityField
    @Column(name = "EXPIRATION_DATE", length = 255)
    private String expirationDateStr;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_ADDITIONAL_INFO)
    @EqualityField
    @Column(name = "ADDITIONAL_INFO")
    private String additionalInfo;

    // TODO: [GAP] lines 83-?? not captured in screenshots — do NOT fabricate missing logic.
    // (one further @Setter-annotated element appeared at the bottom edge of the last photo in
    // this series, cut off before its target field/annotation could be read; it is presumed to
    // be part of the transition into the fields below, e.g. refferenceGroup.)

    @AuditedField(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_REFFERENCE_GROUP)
    @EqualityField
    @Column(name = "REFFERENCE_GROUP", length = 240)
    private String refferenceGroup;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_GIVEN_ID)
    @Column(name = "GIVEN_ID", length = 100)
    private String givenId;

    @ManyToMany(mappedBy = "identityDocuments")
    private List<Record> records = new ArrayList<>();

    public IdentityDocument() { }

    public IdentityDocument(IdentityDocument other) {
        this.id = other.id;
        this.additionalInfo = other.additionalInfo;
        this.country = other.country;
        this.expirationDateStr = other.getExpirationDateStr();
        this.issuingDateStr = other.getIssuingDateStr();
        this.number = other.number;
        this.type = other.type;
    }

    void addRecord(Record record) { records.add(record); }

    void removeRecord(Record record) { records.remove(record); }

    public Optional<IdentityDocumentType> getTypeOpt() { return Optional.ofNullable(type); }

    public Optional<Country> getCountryOpt() { return Optional.ofNullable(country); }

    public Optional<String> getRefferenceGroupOpt() { return Optional.ofNullable(refferenceGroup); }

    public List<Record> getRecords() { return unmodifiableList(records); }

    @Override
    public void removeFromParent(Record parentToUnlink) { parentToUnlink.removeIdentityDocument(this); }

    public boolean hasType(ReferentialBusinessConcept businessConcept) {
        return type != null && type.hasBusinessConcept(businessConcept);
    }

    public Optional<LocalDate> getIssuingDateOpt() {
        return FormatDateUtil.getLocalDateOptFromDateString(getIssuingDateStr());
    }

    public Optional<LocalDate> getExpirationDateOpt() {
        return FormatDateUtil.getLocalDateOptFromDateString(getExpirationDateStr());
    }

    public void setIssuingDate(LocalDate date) { issuingDateStr = FormatDateUtil.getDateStringFromLocalDate(date); }
    public void setExpirationDate(LocalDate date) { expirationDateStr = FormatDateUtil.getDateStringFromLocalDate(date); }
    public Optional<String> getNumberOpt() { return Optional.ofNullable(number); }
    public boolean hasOneOfTypes(List<ReferentialBusinessConcept> businessConcepts) {
        return type != null && businessConcepts.stream().anyMatch(bc -> type.hasBusinessConcept(bc));
    }
}
