package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.entity.referential.record.DateType;
import com.bnpp.regliss.utils.FormatDateUtil;
import com.bnpp.regliss.validation.WarningConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "PERSON_BIOGRAPHY")
@CaseInsensitive
/**@SequenceGenerator(name = "PersonBiographyIdGenerator", initialValue = 100, sequenceName = "PERS_BIO_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "PersonBiographyIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "PERS_BIO_SEQ")} )
@PersonBiographyBirthDeathCountryPlaceConstraint(message="{personBiography.birthDate.birthPlace.birthCountry.deathDate}", groups = WarningConstraint.class)
@PersonBiographyBirthDateBirthPlaceConstraint(message="{personBiography.date.birthPlace}", groups = WarningConstraint.class)
public class PersonBiography extends AbstractSimpleEntity implements RecordAwareChild, ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "PersonBiographyIdGenerator")
    protected Long id;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_DATE_VALUE)
    @EqualityField
    @Column(name = "DATE_VALUE", length = 255)
    private String dateValueStr;

    @Setter
    @Getter
    @AuditedField(AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_DATE_TYPE)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "DATE_TYPE_ID")
    private DateType dateType;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_BIRTH_PLACE)
    @EqualityField
    @Column(name = "BIRTH_PLACE", length = 128)
    private String birthPlace;

    @Setter
    @AuditedField(AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_BIRTH_COUNTRY)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "BIRTH_COUNTRY")
    private Country birthCountry;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_IS_PRIMARY)
    @EqualityField
    @Column(name = "IS_PRIMARY")
    private Boolean primary;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_ADDITIONAL_INFO)
    @EqualityField
    @Column(name = "ADDITIONAL_INFO", length = 512)
    private String additionalInfo;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_GIVEN_ID)
    @Column(name = "GIVEN_ID", length = 100)
    private String givenId;

    @ManyToMany(mappedBy = "biographies")
    private List<PersonRecord> personRecords = new ArrayList<>();

    public PersonBiography() {
    }

    public PersonBiography(long id) { setId(id); }

    public PersonBiography(PersonBiography other) {
        this.id = other.id;
        this.additionalInfo = other.additionalInfo;
        if (other.birthCountry != null) {
            this.birthCountry = new Country(other.birthCountry);
        }
        if (other.dateType != null) {
            this.dateType = new DateType(other.dateType);
        }
        this.dateValueStr = other.dateValueStr;
        this.birthPlace = other.birthPlace;
        this.primary = other.primary;
    }

    void addPersonRecord(PersonRecord record) { personRecords.add(record); }

    void removePersonRecord(PersonRecord record) { personRecords.remove(record); }

    public Optional<Country> getBirthCountryOpt() { return Optional.ofNullable(birthCountry); }

    @Override
    public List<Record> getRecords() { return Collections.unmodifiableList(personRecords); }

    @Override
    public void removeFromParent(Record parentToUnlink) {
        PersonRecord pr = (PersonRecord) parentToUnlink;
        pr.removeBiography(this);
    }

    public Optional<String> getBirthPlaceOpt() { return Optional.ofNullable(birthPlace); }

    public Optional<String> getDateValueStrOpt() { return Optional.ofNullable((dateType == null) ? null : dateValueStr); }

    public void setDateValue(LocalDate date) { dateValueStr = FormatDateUtil.getDateStringFromLocalDate(date); }

    public Optional<DateType> getDateTypeOpt() { return Optional.ofNullable(dateType); }

    public boolean isPrimary() { return Boolean.TRUE.equals(primary); }

    public boolean hasDateType(ReferentialBusinessConcept businessConcept) {
        return dateType != null && dateType.hasBusinessConcept(businessConcept);
    }
}
