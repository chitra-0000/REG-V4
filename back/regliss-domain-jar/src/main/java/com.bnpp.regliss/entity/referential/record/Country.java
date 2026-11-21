package com.bnpp.regliss.entity.referential.record;

import java.io.*;

import javax.persistence.*;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.referential.*;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

/serial/
@Entity
@Table(name = "REF_COUNTRY")
/**@SequenceGenerator(name = "CountryIdGenerator", initialValue = 100, sequenceName = "COUNTRY_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "CountryIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "COUNTRY_SEQ") } )
public class Country extends RecordReferential implements Serializable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "CountryIdGenerator")
    protected Long id;

    @Getter @Setter
    @EqualityField
    @Column(name = "ISO2_CODE", length = 6)
    private String ISO2Code;

    public Country() { super(); }

    public Country(String label) { super(label); }

    public Country(String name, ReferentialBusinessConcept businessConcept) { super(name, businessConcept); }

    public Country(Country other) { super(other); }
}
