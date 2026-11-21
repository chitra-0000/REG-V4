package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

@Entity
@Table(name = "REF_SANCTION_TYPE")
/**@SequenceGenerator(name = "SanctionTypeIdGenerator", initialValue = 100, sequenceName = "SANC_TYPE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "SanctionTypeIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "SANC_TYPE_SEQ")} )
public class SanctionType extends Description {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "SanctionTypeIdGenerator")
    protected Long id;
    @Getter
    @ManyToOne
    @JoinColumn(name = "CATEGORY_ID")
    @EqualityField
    private RecordCategory recordCategory;

    @Getter
    @OneToMany(mappedBy = "sanctionType")
    private Set<Description3> descriptions3 = new HashSet<>();

    public SanctionType() {
    }

    public SanctionType(String label) { super(label); }

    public SanctionType(String label, ReferentialBusinessConcept businessConcept) { super(label, businessConcept); }

    public SanctionType(SanctionType other) {
        super(other);
        this.recordCategory = other.recordCategory;
        this.descriptions3 = new HashSet<>(other.descriptions3);
    }

    public void setRecordCategory(RecordCategory recordCategory) {
        this.recordCategory = recordCategory;
        recordCategory.getSanctionTypes().add(this);
    }

    @Override
    public Description getParent() { return recordCategory; }

    @Override
    public String computeBusinessKey(){
        String parentId = getParent() != null ? getParent().getId() + "" : "";
        return getLabel().trim() +":"+ parentId;
    }

    @Override
    public List<Record> getRecords() { return emptyList(); }

    @Override
    public void removeFromParent(Record parentToUnlink) {
        //intentionally left empty
    }
}
