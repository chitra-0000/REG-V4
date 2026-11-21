package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.referential.record.PEPOccupationCategory;
import com.bnpp.regliss.utils.FormatDateUtil;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "FUNCTION")
/**@SequenceGenerator(name = "PersonFunctionIdGenerator", initialValue = 100, sequenceName = "RECORD_FUNCTION_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "PersonFunctionIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "RECORD_FUNCTION_SEQ")} )
public class PersonFunction extends AbstractSimpleEntity implements RecordAwareChild, ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "PersonFunctionIdGenerator")
    protected Long id;
    @Getter @Setter
    @AuditedField(AuditedFieldCode.PERSON_FUNCTION_CATEGORY)
    @ManyToOne(cascade = CascadeType.MERGE)
    @EqualityField
    @JoinColumn(name = "PERSON_CATEGORY_ID")
    private PEPOccupationCategory pepOccupationCategory;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.PERSON_FUNCTION_NAME)
    @EqualityField
    @Column(name = "FUNCTION_NAME", length = 512, nullable = false)
    private String functionName;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.PERSON_FUNCTION_FROM_DATE)
    @EqualityField
    @Column(name = "FROM_DATE", length = 255)
    private String fromStr;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.PERSON_FUNCTION_TO_DATE)
    @EqualityField
    @Column(name = "TO_DATE", length = 255)
    private String toStr;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.PERSON_FUNCTION_GIVEN_ID)
    @Column(name = "GIVEN_ID", length = 100)
    private String givenId;

    @ManyToMany(mappedBy = "personFunctions")
    private List<PersonRecord> personRecords = new ArrayList<>();

    public PersonFunction() {
    }

    public PersonFunction(long id) { setId(id); }

    public PersonFunction(long id, String fname) {
        setId(id);
        this.functionName = fname;
    }

    public PersonFunction(PersonFunction other) {
        this.id = other.id;
        this.fromStr = other.getFromStr();
        this.toStr = other.getToStr();
        this.functionName= other.functionName;
        this.pepOccupationCategory = other.pepOccupationCategory;
    }

    void addPersonRecord(PersonRecord personRecord) { personRecords.add(personRecord); }

    @Override
    public List<Record> getRecords() { return unmodifiableList (personRecords); }

    @Override
    public void removeFromParent(Record parentToUnlink) { ((PersonRecord)parentToUnlink).removePersonFunction(this); }

    public void removePersonRecord(PersonRecord personRecord) { personRecords.remove(personRecord); }

    public Optional<LocalDate> getFromOpt() { return FormatDateUtil.getLocalDateOptFromDateString(getFromStr()); }

    public void setFrom(LocalDate date) { fromStr = FormatDateUtil.getDateStringFromLocalDate(date); }

    public Optional<LocalDate> getToOpt() { return FormatDateUtil.getLocalDateOptFromDateString(getToStr()); }

    public void setTo(LocalDate date) { toStr = FormatDateUtil.getDateStringFromLocalDate(date); }
}
