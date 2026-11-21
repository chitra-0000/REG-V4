package com.bnpp.regliss.entity.referential;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "REF_REGULATOR_OR_INT_POLICY")
/**@SequenceGenerator(name = "RegulatorOrInternalPolicyIdGenerator", initialValue = 100, sequenceName = "REGULATOR_IP_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "RegulatorOrInternalPolicyIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "REGULATOR_IP_SEQ")} )
public class RegulatorOrInternalPolicy extends ListReferential {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "RegulatorOrInternalPolicyIdGenerator")
    protected Long id;

    public RegulatorOrInternalPolicy() {
    }

    public RegulatorOrInternalPolicy(String label) { super(label); }

    public RegulatorOrInternalPolicy(RegulatorOrInternalPolicy other) { super(other); }
}
