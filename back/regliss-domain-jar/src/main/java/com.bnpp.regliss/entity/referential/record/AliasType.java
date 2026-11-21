package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "REF_ALIAS_TYPE")
/**@SequenceGenerator(name = "AliasTypeIdGenerator", initialValue = 100, sequenceName = "REF_ALIAS_TYPE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "AliasTypeIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "REF_ALIAS_TYPE_SEQ") } )
public class AliasType extends RecordReferential {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "AliasTypeIdGenerator")
    protected Long id;

    public AliasType() {
    }

    public AliasType(String label) { super(label); }

    public AliasType(String label, ReferentialBusinessConcept businessConcept) { super(label, businessConcept); }

    public AliasType(AliasType other) { super(other); }
}
