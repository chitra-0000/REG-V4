package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "REF_PROGRAM_TYPE")
/**@SequenceGenerator(name = "ProgramSanctionTypeIdGenerator", initialValue = 100, sequenceName = "PROGRAM_TYPE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "ProgramSanctionTypeIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "PROGRAM_TYPE_SEQ")} )
public class ProgramSanctionType extends RecordReferential {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "ProgramSanctionTypeIdGenerator")
    protected Long id;

    public ProgramSanctionType() { }

    public ProgramSanctionType(String label) { super(label); }

    public ProgramSanctionType(ProgramSanctionType other) { super(other); }

    public ProgramSanctionType(String label, ReferentialBusinessConcept businessConcept) { super(label, businessConcept); }
}
