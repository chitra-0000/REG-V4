package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Embeddable
@EqualsAndHashCode
public class DJSubfilterDescription1 implements ToStringAuditable{

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_DESCRIPTIONS1)
    @ManyToOne
    @JoinColumn(name = "CATEGORY_ID")
    private RecordCategory recordCategory;

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_DESCRIPTIONS1)
    @Column(name = "INCLUDE_RCA")
    private Boolean includeRca;

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_DESCRIPTIONS1)
    @Column(name = "INCLUDE_RCAA")
    private Boolean includeRcaa;


    protected DJSubfilterDescription1() {}

    public DJSubfilterDescription1(RecordCategory recordCategory, Boolean includeRca, Boolean includeRcaa) {
        super();
        this.recordCategory = recordCategory;
        this.includeRca = includeRca;
        this.includeRcaa = includeRcaa;
    }

    public DJSubfilterDescription1(DJSubfilterDescription1 other) {
        this.recordCategory = other.recordCategory;
        this.includeRca = other.includeRca;
        this.includeRcaa = other.includeRcaa;
    }

    public boolean isIncludeRca() { return Boolean.TRUE.equals(includeRca); }

    public boolean isIncludeRcaa() { return Boolean.TRUE.equals(includeRcaa); }

    @Override
    public String toStringAudit() {
        StringBuilder auditValue = new StringBuilder(recordCategory.getLabel());
        if (includeRca) {
            auditValue.append("+RCA");
        }

        if (includeRcaa) {
            auditValue.append("+RCAA");
        }
        return auditValue.toString();
    }
}
