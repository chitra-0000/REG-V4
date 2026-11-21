package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/serial/
@Entity
@Table(name = "REF_COUNTRY_TYPE")
/**@SequenceGenerator(name = "CountryTypeIdGenerator", initialValue = 100, sequenceName = "COUNTRY_TYPE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "CountryTypeIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "COUNTRY_TYPE_SEQ") } )
public class CountryType extends RecordReferential implements Serializable{

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "CountryTypeIdGenerator")
    protected Long id;

    public CountryType() { super(); }

    public CountryType(String label) { super(label); }

    public CountryType(CountryType other) { super(other); }

    public CountryType(String label, ReferentialBusinessConcept businessConcept) { super(label, businessConcept); }
}
