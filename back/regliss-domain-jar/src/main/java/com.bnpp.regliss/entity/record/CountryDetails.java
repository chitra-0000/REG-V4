package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.entity.referential.record.CountryType;
import com.bnpp.regliss.validation.WarningConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.bnpp.regliss.entity.referential.ReferentialBusinessConcept.COUNTRY_TYPE_CITIZENSHIP;
// TODO: the project imports were not visible in the screenshots for the static import enabling
// the bare "unmodifiableList(...)" calls below (e.g. "import static java.util.Collections.unmodifiableList;").
// Re-add it in the IDE (Alt+Enter / Optimize Imports).

@Entity
@Table(name = "COUNTRY_DETAILS")
@CaseInsensitive
/**@SequenceGenerator(name = "CountryDetailsIdGenerator", initialValue = 100, sequenceName = "NATION_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "CountryDetailsIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "NATION_SEQ")} )
@CountryDetailsNationalityConstraint(message="{countryDetails.nationality.mandatory}", groups = WarningConstraint.class)
public class CountryDetails extends AbstractSimpleEntity implements ToStringAuditable, RecordAwareChild {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "CountryDetailsIdGenerator")
    protected Long id;
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_COUNTRY_DETAILS_TYPE)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "COUNTRY_TYPE_ID")
    private CountryType type;

    @Setter @Getter
    @AuditedField(AuditedFieldCode.RECORD_COUNTRY_DETAILS_IS_PRIMARY)
    @EqualityField
    @Column(name = "IS_PRIMARY")
    private Boolean primary;

    @Setter
    @AuditedField(AuditedFieldCode.RECORD_COUNTRY_DETAILS_ADDITIONAL_INFO)
    @EqualityField
    @Column(name = "ADDITIONAL_INFO", length = 2048)
    private String additionalInfo;

    @Setter
    @AuditedField(AuditedFieldCode.RECORD_COUNTRY_DETAILS_BIRTH_COUNTRY)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "COUNTRY_ID")
    private Country country;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_COUNTRY_DETAILS_GIVEN_ID)
    @Column(name = "GIVEN_ID", length = 100)
    private String givenId;

    @ManyToMany(mappedBy = "countriesDetails")
    private final List<Record> records = new ArrayList<>();

    public CountryDetails() {
    }

    public CountryDetails(Long id) { this.id = id; }

    public CountryDetails(CountryDetails other) {
        this.id = other.id;
        this.additionalInfo = other.additionalInfo;
        this.country = other.country;
        this.primary = other.primary;
        this.type = other.type;
    }

    public CountryDetails(CountryType type, Country country) {
        this.type = type;
        this.country = country;
    }

    @Override
    public List<Record> getRecords() { return unmodifiableList(records); }

    void addRecord(Record record) { records.add(record); }

    void removeRecord(Record record) { records.remove(record); }

    public Optional<Country> getCountryOpt() { return Optional.ofNullable(country); }
    public Optional<CountryType> getTypeOpt() { return Optional.ofNullable(type); }
    public Optional<String> getAdditionalInfoOpt() { return Optional.ofNullable(additionalInfo); }

    @Override
    public void removeFromParent(Record parentToUnlink) { parentToUnlink.removeCountryDetails(this); }

    public boolean hasType(ReferentialBusinessConcept businessConcept) {
        return type != null && type.hasBusinessConcept(businessConcept);
    }

    public boolean isCitizenshipType() {
        return getTypeOpt().filter(to -> to.hasBusinessConcept(COUNTRY_TYPE_CITIZENSHIP)).isPresent();
    }
}
