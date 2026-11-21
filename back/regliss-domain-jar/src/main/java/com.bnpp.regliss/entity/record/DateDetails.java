package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.DateType;
import com.bnpp.regliss.utils.FormatDateUtil;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "DATE_DETAILS")
/**@SequenceGenerator(name = "DateDetailsIdGenerator", initialValue = 100, sequenceName = "DATE_DET_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "DateDetailsIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "DATE_DET_SEQ") } )
public class DateDetails extends AbstractSimpleEntity implements RecordAwareChild, ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "DateDetailsIdGenerator")
    protected Long id;
    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_DATE_DETAILS_DATE_VALUE)
    @EqualityField
    @Column(name = "DATE_VALUE", length = 255)
    private String dateValueStr;

    @Setter
    @AuditedField(AuditedFieldCode.RECORD_DATE_DETAILS_DATE_TYPE)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "DATE_TYPE_ID")
    private DateType dateType;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_DATE_DETAILS_GIVEN_ID)
    @Column(name = "GIVEN_ID", length = 100)
    private String givenId;

    @ManyToMany(mappedBy = "datesDetails")
    private List<Record> records = new ArrayList<>();

    public DateDetails() {

    }

    public DateDetails(String dateValueStr, DateType dateType) {
        this.dateValueStr = dateValueStr;
        this.dateType = dateType;
    }

    public DateDetails(DateDetails other) {
        this.dateType = new DateType(other.dateType);
        this.dateValueStr = other.getDateValueStr();
    }

    void addRecord(Record record) { records.add(record); }
    public Optional<DateType> getDateTypeOpt() { return Optional.ofNullable(dateType); }

    @Override
    public List<Record> getRecords() { return Collections.unmodifiableList(records); }

    void removeRecord(Record record) { records.remove(record); }

    @Override
    public void removeFromParent(Record parentToUnlink) { parentToUnlink.removeDateDetails(this); }

    public boolean hasType(ReferentialBusinessConcept businessConcept) {
        return dateType != null && dateType.hasBusinessConcept(businessConcept);
    }
}
