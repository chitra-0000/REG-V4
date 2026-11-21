package com.bnpp.regliss.entity.referential;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "REF_ISSUER")
/**@SequenceGenerator(name = "IssuerIdGenerator", initialValue = 100, sequenceName = "ISSUER_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "IssuerIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "ISSUER_SEQ")} )
public class Issuer extends ListReferential {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "IssuerIdGenerator")
    protected Long id;

    public Issuer() {}

    public Issuer(String label) { super(label); }

    public Issuer(Issuer other) { super(other); }

}
