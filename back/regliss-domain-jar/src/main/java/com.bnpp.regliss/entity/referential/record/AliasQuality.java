package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;

@Entity
@Table(name = "REF_ALIAS_QUALITY")
/**@SequenceGenerator(name = "AliasQualityIdGenerator", initialValue = 100, sequenceName = "REF_ALIAS_QUALITY_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "AliasQualityIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "REF_ALIAS_QUALITY_SEQ")} )
public class AliasQuality extends RecordReferential {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "AliasQualityIdGenerator")
    protected Long id;

    public AliasQuality() {
    }

    public AliasQuality(ReferentialBusinessConcept businessConcept) { super(businessConcept); }

    public AliasQuality(String label) { super(label); }

    public AliasQuality(String label, ReferentialBusinessConcept businessConcept) { super(label, businessConcept); }

    public AliasQuality(AliasQuality other) { super(other); }
}
