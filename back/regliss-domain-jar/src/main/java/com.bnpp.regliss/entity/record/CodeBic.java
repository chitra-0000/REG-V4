package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.validation.ErrorConstraint;
import com.bnpp.regliss.validation.SemiAutoErrorConstraint;
import com.bnpp.regliss.validation.WarningConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "CODE_BIC")
@CaseInsensitive
/**@SequenceGenerator(name = "CodeBicIdGenerator", initialValue = 100, sequenceName = "CODE_BIC_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "CodeBicIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "CODE_BIC_SEQ") } )
public class CodeBic extends AbstractSimpleEntity implements ToStringAuditable, RecordAwareChild {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "CodeBicIdGenerator")
    protected Long id;

    @NotEmpty(message = "{entity.bic.mandatory}", groups = WarningConstraint.class)
    @CustomLength(filterByCustomLength = {8,11}, message = "{entity.bic.length}", groups = {WarningConstraint.class, ErrorConstraint.class, SemiAutoErrorConstraint.class})
    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.BIC_VALUE)
    @Column(name = "CODE", length = 12)
    private String code;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.BIC_GIVEN_ID)
    @Column(name = "GIVEN_ID", length = 100)
    private String givenId;

    @ManyToMany(mappedBy = "bicCodes")
    private List<EntityRecord> entityRecords = new ArrayList<>();

    public CodeBic() {
    }

    public CodeBic(String code) { this.code = code; }

    public CodeBic(Long id) { this.id = id; }

    public CodeBic(Long id, String code) {
        this.id = id;
        this.code = code;
    }

    public CodeBic(CodeBic other) {
        this.id = other.id;
        this.code = other.code;
    }

    @Override
    public List<Record> getRecords() { return Collections.unmodifiableList(entityRecords); }

    public List<EntityRecord> getEntityRecords() { return unmodifiableList(entityRecords); }
    void addEntityRecord(EntityRecord record) { entityRecords.add(record); }
    void removeEntityRecord(EntityRecord record) { entityRecords.remove(record); }

    @Override
    public void removeFromParent(Record parentToUnlink) {
        if (parentToUnlink instanceof EntityRecord) {
            EntityRecord er = (EntityRecord) parentToUnlink;
            er.removeBicCode(this);
        } else {
            throw new IllegalArgumentException();
        }
    }
}
