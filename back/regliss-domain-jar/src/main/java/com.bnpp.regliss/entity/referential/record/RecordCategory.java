package com.bnpp.regliss.entity.referential.record;

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
@Table(name = "REF_RECORD_CATEGORY")
/**@SequenceGenerator(name = "RecordCategoryIdGenerator", initialValue = 100, sequenceName = "CATEGORY_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "RecordCategoryIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "CATEGORY_SEQ")} )
public class
RecordCategory extends Description {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "RecordCategoryIdGenerator")
    protected Long id;
    @Getter
    @OneToMany(mappedBy = "recordCategory")
    private Set<SanctionType> sanctionTypes = new HashSet<>();


    public RecordCategory(String label) { super(label); }

    public RecordCategory(RecordCategory other) {
        super(other);
        this.sanctionTypes = new HashSet<>(other.sanctionTypes);
    }

    public RecordCategory(String label, ReferentialBusinessConcept businessConcept) { super(label, businessConcept); }

    public RecordCategory() { }

    @Override
    public Description getParent() { return null; }

    @Override
    public String computeBusinessKey() { return getLabel().trim(); }

    @Override
    public List<Record> getRecords() { return emptyList(); }

    @Override
    public void removeFromParent(Record parentToUnlink) {
        //intentionally left empty
    }
}
