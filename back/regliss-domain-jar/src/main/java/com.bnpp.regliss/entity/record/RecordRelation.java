package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.CaseInsensitive;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.entity.referential.record.RelationType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "RELATION")
/**@SequenceGenerator(name = "RecordRelationIdGenerator", initialValue = 100, sequenceName = "RECORD_RELATION_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "RecordRelationIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "RECORD_RELATION_SEQ") } )
public class RecordRelation extends AbstractSimpleEntity implements ToStringAuditable, RecordAwareChild {
    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "RecordRelationIdGenerator")
    protected Long id;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_RELATION_NAME)
    @EqualityField
    @Column(name = "NAME", length = 256)
    private String name;

    @Getter
    @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.RECORD_RELATION_CATEGORY)
    @JoinColumn(name = "CATEGORY_ID")
    @ManyToOne
    private RecordCategory category;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_RELATION_TYPE)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "RELATION_TYPE")
    private RelationType relationType;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_RELATION_RECORD_TYPE)
    @Column(name = "RECORD_TYPE")
    @Enumerated(EnumType.STRING)
    private RecordType recordType;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_RELATION_EX)
    @EqualityField
    @Column(name = "EX")
    private Boolean ex;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_RELATION_GIVEN_ID)
    @Column(name = "GIVEN_ID", length = 100)
    private String givenId;

    @ManyToMany(mappedBy = "relations")
    private List<Record> records = new ArrayList<>();

    public RecordRelation() {
    }


    public RecordRelation(RecordRelation other) {
        this.id = other.id;
        this.name = other.name;
        this.category = other.category;
        this.relationType = other.relationType;
        this.recordType = other.recordType;
        this.ex = other.ex;
    }


    void addRecord(Record record) { records.add(record); }

    @Override
    public List<Record> getRecords() { return unmodifiableList(records); }


    @Override
    public void removeFromParent(Record parentToUnlink) { parentToUnlink.removeRelation(this); }

    public Optional<RelationType> getRelationTypeOpt() { return Optional.ofNullable(relationType); }

    public Optional<RecordCategory> getCategoryOpt() { return Optional.ofNullable(category); }
}
