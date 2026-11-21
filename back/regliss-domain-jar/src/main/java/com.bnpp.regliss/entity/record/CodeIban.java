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
@Table(name = "CODE_IBAN")
/**@SequenceGenerator(name = "CodeIbanIdGenerator", initialValue = 100, sequenceName = "CODE_IBAN_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "CodeIbanIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "CODE_IBAN_SEQ") } )
public class CodeIban extends AbstractSimpleEntity implements RecordAwareChild, ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "CodeIbanIdGenerator")
    protected Long id;

    @NotEmpty(message = "{record.iban.mandatory}", groups = WarningConstraint.class)
    @Size(min = 14, max = 34, message = "{record.iban.size}", groups = WarningConstraint.class)
    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.IBAN_VALUE)
    @Column(name = "CODE", length = 34)
    private String code;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.IBAN_GIVEN_ID)
    @Column(name = "GIVEN_ID", length = 100)
    private String givenId;

    @ManyToMany(mappedBy = "ibanCodes")
    private List<Record> records = new ArrayList<>();

    public CodeIban() {
    }

    public CodeIban(String code) { this.code = code; }

    public CodeIban(Long id, String code) {
        this.id = id;
        this.code = code;
    }

    public CodeIban(Long id) { this.id = id; }

    public CodeIban(CodeIban other) {
        this.id = other.id;
        this.code = other.code;
    }

    void addRecord(Record record) { records.add(record); }

    void removeRecord(Record record) { records.remove(record); }

    public List<Record> getRecords() { return unmodifiableList(records); }

    @Override
    public void removeFromParent(Record parentToUnlink) { parentToUnlink.removeIbanCode(this); }
}
