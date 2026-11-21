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
@DiscriminatorValue(value = RecordType.DiscriminatorValues.COUNTRY)
public class CountryRecord extends Record {

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.COUNTRY_SEARCH_CODE)
    @Column(name = "SEARCH_CODE", length = 36)
    private String searchCode;

    @Column(name = "SEARCH_CODE_NORMALIZED", length = 54)
    private String searchCodeNormalized;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.COUNTRY_ISO_NUM)
    @Column(name = "ISO_NUM", length = 3)
    private String isoNum;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.COUNTRY_ISO_ALPHA)
    @Column(name = "ISO_ALPHA",  length = 6)
    private String isoAlpha;

    public CountryRecord() { super(RecordType.COUNTRY); }

    public CountryRecord(CountryRecord other) {
        super(other);
        this.searchCode = other.searchCode;
        this.isoNum =  other.isoNum;
        this.isoAlpha= other.isoAlpha;
        this.searchCodeNormalized = other.searchCodeNormalized;
    }

    public void setSearchCode(String searchCode) {
        this.searchCode = searchCode;
        this.searchCodeNormalized = TextNormalizerQueryUtil.normalize(searchCode);
    }
}
