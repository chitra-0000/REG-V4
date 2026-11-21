package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.CaseInsensitive;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = RecordType.DiscriminatorValues.OTHER)
public class OtherRecord extends Record {

    @CaseInsensitive
    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.OTHER_CATEGORY)
    @Column(name = "OTHER_CATEGORY", length = 64)
    private String otherCategory;

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.OTHER_SEARCH_CODE)
    @Column(name = "SEARCH_CODE",  length = 36)
    private String searchCode;

    @Column(name = "SEARCH_CODE_NORMALIZED", length = 54)
    private String searchCodeNormalized;

    public OtherRecord() { super(RecordType.OTHER); }

    public OtherRecord(OtherRecord other) {
        super(other);
        this.searchCode = other.searchCode;
        this.searchCodeNormalized = other.searchCode;
        this.otherCategory = other.otherCategory;
    }

    public void setSearchCode(String searchCode) {
        this.searchCode = searchCode;
        this.searchCodeNormalized = TextNormalizerQueryUtil.normalize(searchCode);
    }
}
