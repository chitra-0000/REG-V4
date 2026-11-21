package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.CaseInsensitive;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.ProgramSanctionType;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import com.bnpp.regliss.validation.SemiAutoErrorConstraint;
import com.bnpp.regliss.validation.WarningConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "PROGRAM")
/**@SequenceGenerator(name = "ProgramIdGenerator", initialValue = 100, sequenceName = "PROGRAM_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "ProgramIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "PROGRAM_SEQ")} )
public class Program extends AbstractSimpleEntity implements Comparable<Program>, ToStringAuditable, RecordAwareChild {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "ProgramIdGenerator")
    protected Long id;

    @NotEmpty(message="{program.label.mandatory}", groups = {WarningConstraint.class, SemiAutoErrorConstraint.class})
    @AuditedField(AuditedFieldCode.RECORD_PROGRAM_LABEL)
    @EqualityField
    @Column(name = "LABEL")
    private String label;

    @Column(name = "LABEL_NORMALIZED")
    private String labelNormalized;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_PROGRAM_DESCRIPTION)
    @EqualityField
    @Column(name = "DESCRIPTION", length=2048)
    private String description;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_PROGRAM_SANCTION_TYPE)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "PROGRAM_TYPE_ID")
    private ProgramSanctionType sanctionType;

    @Setter
    @AuditedField(AuditedFieldCode.RECORD_PROGRAM_REFFERENCE_GROUP)
    @EqualityField
    @Column(name = "REFFERENCE_GROUP", length = 240)
    private String refferenceGroup;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_PROGRAM_GIVEN_ID)
    @EqualityField
    @Column(name = "GIVEN_ID", length = 100)//, nullable = false)
    private String givenId;

    @ManyToMany(mappedBy = "programs")
    private List<Record> records = new ArrayList<>();

    @Override
    public List<Record> getRecords() { return unmodifiableList(records); }

    public Program() {
    }

    public Program(String label) { this.setLabel(label); }

    public Program(Program other) {
        this.id = other.id;
        this.description = other.description;
        this.label = other.label;
        this.labelNormalized = other.labelNormalized;
        this.refferenceGroup = other.refferenceGroup;
        if (other.sanctionType != null) {
            this.sanctionType = new ProgramSanctionType(other.sanctionType);
        }
    }

    void addRecord(Record record) { records.add(record); }
    public Optional<ProgramSanctionType> getSanctionTypeOpt() { return Optional.ofNullable(sanctionType); }

    public Optional<String> getLabelOpt() { return Optional.ofNullable(label); }

    public Optional<String> getRefferenceGroupOpt() { return Optional.ofNullable(refferenceGroup); }

    @Override
    public int compareTo(Program other) {
        String otherLabelSafe = other.label != null ? other.label : "";
        String myLabelSafe = this.label != null ? this.label : "";
        return myLabelSafe.toUpperCase().compareTo(otherLabelSafe.toUpperCase());
    }

    @Override
    public void removeFromParent(Record parentToUnlink) { parentToUnlink.removeProgram(this); }

    public void setLabel(String label) {
        this.label = label;
        this.labelNormalized = TextNormalizerQueryUtil.normalize(label);
    }

    public boolean hasType(ReferentialBusinessConcept businessConcept) {
        return sanctionType != null && sanctionType.hasBusinessConcept(businessConcept);
    }

    public boolean hasOneOfTypes(List<ReferentialBusinessConcept> businessConcepts) {
        return sanctionType != null && businessConcepts.stream().anyMatch(bc -> sanctionType.hasBusinessConcept(bc));
    }
}
