package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.validation.WarningConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "ADDRESS")
/**@SequenceGenerator(name = "AddressIdGenerator", initialValue = 100, sequenceName = "ADDRESS_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "AddressIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "ADDRESS_SEQ")} )
@AddressCityCountryConstraint(message="{addresse.city.country.mandatory}", groups = WarningConstraint.class)
public class Address extends AbstractSimpleEntity implements ToStringAuditable, RecordAwareChild {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "AddressIdGenerator")
    protected Long id;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_ADDRESS_REGLISS_REFERENCE)
    @Column(name = "REGLISS_REFERENCE")
    private String reglissReference;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_ADDRESS_ADDRESS)
    @EqualityField
    // TODO: [GAP] lines ~42-82 of Address.java were photographed but are too motion-blurred to transcribe reliably.
    // Do NOT fabricate missing logic/annotation details here. From context (constructor body below, which copies
    // other.address, other.city, other.country, other.state, other.zipCode, other.reglissReference) this section
    // declares, in some order, fields: address, city, state, country (ManyToOne to Country), zipCode, and possibly
    // additional fields (e.g. cityCode, additionalAddress) each following the @Getter @Setter / @AuditedField /
    // @EqualityField / @Column (or @ManyToOne/@JoinColumn for country) pattern used throughout this class.
    // Re-check the original source file / IDE for the exact annotations, @Column names, lengths and field order.
    private String address;

    private String city;

    private String state;

    private Country country;

    private String zipCode;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_ADDRESS_GIVEN_ID)
    @Column(name = "GIVEN_ID", length = 100)
    private String givenId;

    @ManyToMany(mappedBy = "addresses")
    private List<Record> records = new ArrayList<>();

    @Transient
    @Getter @Setter
    private int indexOnRecord;

    public Address() {
    }

    public Address(Long id) { this.id = id; }

    public Address(Address other) {
        this.id = other.id;
        this.address = other.address;
        this.city = other.city;
        this.country = other.country;
        this.state = other.state;
        this.zipCode = other.zipCode;
        this.reglissReference = other.reglissReference;
    }

    void addRecord(Record record) { records.add(record); }

    void removeRecord(Record record) { records.remove(record); }

    public List<Record> getRecords() { return unmodifiableList(records); }

    public Optional<Country> getCountryOpt() { return Optional.ofNullable(country); }

    @Override
    public void removeFromParent(Record parentToUnlink) { parentToUnlink.removeAddress(this); }

    public Record getSomeParentRecord() { return getRecords().iterator().next(); }
}
