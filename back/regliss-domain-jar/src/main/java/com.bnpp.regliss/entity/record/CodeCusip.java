package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.validation.WarningConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "CODE_CUSIP")
/**@SequenceGenerator(name = "CodeCusipIdGenerator", initialValue = 100, sequenceName = "CODE_CUSIP_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "CodeCusipIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "CODE_CUSIP_SEQ") } )
public class CodeCusip extends AbstractSimpleEntity implements ToStringAuditable, RecordAwareChild {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "CodeCusipIdGenerator")
    protected Long id;

    @NotEmpty(message = "{entity.cusip.mandatory}", groups = WarningConstraint.class)
    @Size(min = 9, max = 9, message = "{entity.cusip.size}", groups = WarningConstraint.class)
    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.CUSIP_VALUE)
    @Column(name = "CODE", length = 9)
    private String code;

    @ManyToMany(mappedBy = "cusipCodes")
    private List<EntityRecord> entityRecords = new ArrayList<>();

    public CodeCusip() {
    }

    public CodeCusip(String code) { this.code = code; }

    public CodeCusip(Long id) { this.id = id; }

    public CodeCusip(Long id, String code) {
        this.id = id;
        this.code = code;
    }

    public CodeCusip(CodeCusip other) {
        this.id = other.id;
        this.code = other.code;
    }

    public List<EntityRecord> getEntityRecords() { return unmodifiableList(entityRecords); }
    void addEntityRecord(EntityRecord record) { entityRecords.add(record); }
    void removeEntityRecord(EntityRecord record) { entityRecords.remove(record); }

    @Override
    public List<Record> getRecords() { return unmodifiableList(entityRecords); }

    @Override
    public void removeFromParent(Record parentToUnlink) {
        if (parentToUnlink instanceof EntityRecord) {
            EntityRecord er = (EntityRecord) parentToUnlink;
            er.removeCusip(this);
        } else {
            throw new IllegalArgumentException();
        }
    }
}
