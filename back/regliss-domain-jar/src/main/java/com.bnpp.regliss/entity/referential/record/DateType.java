package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/serial/
@Entity
@Table(name = "REF_DATE_TYPE")
/**@SequenceGenerator(name = "DateTypeIdGenerator", initialValue = 100, sequenceName = "DATE_TYPE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "DateTypeIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "DATE_TYPE_SEQ")} )
public class DateType extends RecordReferential implements Serializable{

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "DateTypeIdGenerator")
    protected Long id;

    public DateType() { super(); }

    public DateType(String label) { super(label); }

    public DateType(DateType other) { super(other); }

    public DateType(String name, ReferentialBusinessConcept businessConcept) { super(name, businessConcept); }
}
