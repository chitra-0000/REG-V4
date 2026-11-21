package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.CaseInsensitive;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = RecordType.DiscriminatorValues.AIRPORT)
public class AirportRecord extends Record {

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.AIRPORT_SEARCH_CODE)
    @Column(name = "SEARCH_CODE", length = 36)
    private String searchCode;

    @Column(name = "SEARCH_CODE_NORMALIZED", length = 54)
    private String searchCodeNormalized;

    public AirportRecord() { super(RecordType.AIRPORT); }

    public AirportRecord(AirportRecord other) {
        super(other);
        this.searchCode = other.searchCode;
        this.searchCodeNormalized = other.searchCodeNormalized;
    }

    public void setSearchCode(String searchCode) {
        this.searchCode = searchCode;
        this.searchCodeNormalized = TextNormalizerQueryUtil.normalize(searchCode);
    }
}
