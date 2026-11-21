package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.utils.FormatDateUtil;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "SANCTION_REFERENCE")
/**@SequenceGenerator(name = "SanctionReferenceIdGenerator", initialValue = 100, sequenceName = "SANCTION_REFERENCE_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "SanctionReferenceIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "SANCTION_REFERENCE_SEQ")} )
public class SanctionReference extends AbstractSimpleEntity implements RecordAwareChild, ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "SanctionReferenceIdGenerator")
    protected Long id;
    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.RECORD_SANCTION_REFERENCE_CODE)
    @Column(name = "CODE", length = 50)
    private String code;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_SANCTION_REFERENCE_LIST_NAME)
    @EqualityField
    @Column(name = "LIST_NAME", length = 300)
    private String listName;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_SANCTION_REFERENCE_SINCE_DATE)
    @EqualityField
    @Column(name = "SINCE_DATE", length = 255)
    private String sinceDateStr;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_SANCTION_REFERENCE_END_DATE)
    @EqualityField
    @Column(name = "END_DATE", length = 255)
    private String endDateStr;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_SANCTION_REFERENCE_REMARKS)
    @EqualityField
    @Column(name = "REMARKS", length = 2048)
    private String remarks;

    @Getter
    @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.RECORD_SANCTION_REFERENCE_GIVEN_ID)
    @Column(name = "GIVEN_ID", length = 100)
    private String givenId;

    @ManyToMany(mappedBy = "sanctionReferences")
    private List<Record> records = new ArrayList<>();

    public SanctionReference() {
    }

    public SanctionReference(SanctionReference other) {
        this.id = other.id;
        this.code = other.code;
        this.listName = other.listName;
        this.sinceDateStr = other.sinceDateStr;
        this.endDateStr = other.endDateStr;
        this.remarks = other.remarks;
    }

    void addRecord(Record record) { records.add(record); }

    @Override
    public List<Record> getRecords() { return unmodifiableList(records); }

    @Override
    public void removeFromParent(Record parentToUnlink) { parentToUnlink.removeSanctionReference(this); }

    public void setSinceDate(LocalDate date) { sinceDateStr = FormatDateUtil.getDateStringFromLocalDate(date); }

    public void setEndDate(LocalDate date) { endDateStr = FormatDateUtil.getDateStringFromLocalDate(date); }
}
