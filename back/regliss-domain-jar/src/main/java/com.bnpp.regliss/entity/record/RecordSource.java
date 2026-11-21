package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.CaseInsensitive;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "SOURCE")
/**@SequenceGenerator(name = "RecordSourceIdGenerator", initialValue = 100, sequenceName = "SOURCE_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "RecordSourceIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "SOURCE_SEQ") } )
public class RecordSource extends AbstractSimpleEntity implements RecordAwareChild, ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "RecordSourceIdGenerator")
    protected Long id;
    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_SOURCE_NAME)
    @EqualityField
    @Column(name = "NAME", length = 300)
    private String name;

    @ManyToMany(mappedBy = "sources")
    private List<Record> records = new ArrayList<>();

    public RecordSource() {}

    public RecordSource(RecordSource other) {
        this.id = other.id;
        this.name=other.name;
    }

    public RecordSource(String name) { this.name = name; }

    void addRecord(Record record) { records.add(record); }

    @Override
    public List<Record> getRecords() { return unmodifiableList(records); }

    @Override
    public void removeFromParent(Record parentToUnlink) { parentToUnlink.removeSource(this); }
}
