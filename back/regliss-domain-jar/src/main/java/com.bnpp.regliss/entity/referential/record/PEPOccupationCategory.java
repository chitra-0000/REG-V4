package com.bnpp.regliss.entity.referential.record;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "REF_PEP_OCCUPATION_CATEGORY")
/**@SequenceGenerator(name = "PEPOccupationCategoryIdGenerator", initialValue = 100, sequenceName = "PEP_OCCUPATION_CATEG_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "PEPOccupationCategoryIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "PEP_OCCUPATION_CATEG_SEQ")} )
public class PEPOccupationCategory extends RecordReferential {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "PEPOccupationCategoryIdGenerator")
    protected Long id;

    public PEPOccupationCategory() {
    }

    public PEPOccupationCategory(String label) { super(label); }

    public PEPOccupationCategory(PEPOccupationCategory other) { super(other); }
}
