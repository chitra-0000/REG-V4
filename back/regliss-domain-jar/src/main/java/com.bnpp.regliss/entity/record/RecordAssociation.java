package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.CaseInsensitive;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
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
@Table(name = "ASSOCIATION")
@CaseInsensitive
/**@SequenceGenerator(name = "RecordAssociationIdGenerator", initialValue = 100, sequenceName = "RECORD_ASSOCIATION_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "RecordAssociationIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "RECORD_ASSOCIATION_SEQ") } )
public class RecordAssociation extends AbstractSimpleEntity implements ToStringAuditable, RecordAwareChild {
    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "RecordAssociationIdGenerator")
    protected Long id;
    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_ASSOCIATION_EXT_REF)
    @EqualityField
    @Column(name = "EXTERNAL_REFERENCE", length = 20)
    private String externalReference;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_ASSOCIATION_TYPE)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "RELATION_TYPE")
    private RelationType relationType;

    @Setter
    @AuditedField(AuditedFieldCode.RECORD_ASSOCIATION_EX)
    @EqualityField
    @Column(name = "EX")
    private Boolean ex;

    @ManyToMany(mappedBy = "associations")
    private List<Record> records = new ArrayList<>();

    public RecordAssociation() {
    }


    public RecordAssociation(RecordAssociation other) {
        this.id = other.id;
        this.externalReference = other.externalReference;
        this.relationType = other.relationType;
        this.ex =  other.ex;
    }

    void addRecord(Record record) { records.add(record); }

    @Override
    public List<Record> getRecords() { return unmodifiableList(records); }

    @Override
    public void removeFromParent(Record parentToUnlink) { parentToUnlink.removeAssociation(this); }

    public Optional<RelationType> getRelationTypeOpt() { return Optional.ofNullable(relationType); }

    public RecordAssociation(String externalReference, RelationType relationType, boolean ex) {
        super();
        this.externalReference = externalReference;
        this.relationType = relationType;
        this.ex = ex;
    }

    public boolean isEx() { return Boolean.TRUE.equals(ex); }
}
