package com.bnpp.regliss.entity.referential.record;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "REF_RELATION_TYPE")
/**@SequenceGenerator(name = "RelationTypeIdGenerator", initialValue = 100, sequenceName = "RELATION_TYPE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "RelationTypeIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "RELATION_TYPE_SEQ")} )
public class RelationType extends RecordReferential {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "RelationTypeIdGenerator")
    protected Long id;

    public RelationType() { }

    public RelationType(String label) { super(label); }

    public RelationType(RelationType other) { super(other); }

}
