package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.record.Record;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

import static java.util.Collections.emptyList;

@Entity
@Table(name = "REF_DESCRIPTION3")
/**@SequenceGenerator(name = "Description3IdGenerator", initialValue = 100, sequenceName = "DESCRIPTION_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "Description3IdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "DESCRIPTION_SEQ") } )
public class Description3 extends Description {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "Description3IdGenerator")
    protected Long id;
    @Getter
    @ManyToOne
    @JoinColumn(name = "SANCTION_TYPE_ID")
    @EqualityField
    private SanctionType sanctionType;

    public Description3() {
    }

    public Description3(Description3 other) {
        super(other);
        this.sanctionType = new SanctionType(other.sanctionType);
    }

    public Description3(String label) { super(label); }

    public void setSanctionType(SanctionType sanctionType) {
        this.sanctionType = sanctionType;
        sanctionType.getDescriptions3().add(this);
    }

    @Override
    public Description getParent() { return sanctionType; }

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
