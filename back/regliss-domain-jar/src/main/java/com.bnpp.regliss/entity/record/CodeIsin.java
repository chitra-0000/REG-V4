package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.CaseInsensitive;
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
@Table(name = "CODE_ISIN")
@CaseInsensitive
/**@SequenceGenerator(name = "CodeIsinIdGenerator", initialValue = 100, sequenceName = "CODE_ISIN_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "CodeIsinIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "CODE_ISIN_SEQ") } )
public class CodeIsin extends AbstractSimpleEntity implements ToStringAuditable, RecordAwareChild {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "CodeIsinIdGenerator")
    protected Long id;

    @NotEmpty(message = "{entity.isin.mandatory}", groups = WarningConstraint.class)
    @Size(min = 12, max = 12, message = "{entity.isin.size}", groups = WarningConstraint.class)
    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.ISIN_VALUE)
    @Column(name = "CODE", length = 12)
    private String code;

    @ManyToMany(mappedBy = "isinCodes")
    private List<EntityRecord> entityRecords = new ArrayList<>();

    public CodeIsin() {
    }

    public CodeIsin(Long id) { this.id = id; }

    public CodeIsin(String code) { this.code = code; }

    public CodeIsin(CodeIsin other) {
        this.id = other.id;
        this.code = other.code;
    }

    public CodeIsin(Long id, String code) {
        this.id = id;
        this.code = code;
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
            er.removeIsinCode(this);
        } else {
            throw new IllegalArgumentException();
        }
    }
}
