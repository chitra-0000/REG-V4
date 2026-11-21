package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;

@Entity
@Table(name = "SUBST_MATRIX_ELEM")
/**@SequenceGenerator(name = "SubstitutionMatrixElementIdGenerator", initialValue = 100, sequenceName = "SUBST_MATRIX_ELEM_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "SubstitutionMatrixElementIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "SUBST_MATRIX_ELEM_SEQ") } )
public class SubstitutionMatrixElement extends AbstractSimpleEntity implements ToStringAuditable{

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "SubstitutionMatrixElementIdGenerator")
    protected Long id;
    @Getter @Setter
    @EqualityField
    @Column(name = "FILE_FORMAT")
    @Enumerated(EnumType.STRING)
    @AuditedField(AuditedFieldCode.SUBSTITUTION_MATRIX_FILE_FORMAT)
    private FileExportFormat fileFormat;

    @Getter @Setter
    @EqualityField
    @Column(name = "ORIGINAL_VALUE", length=256)
    @AuditedField(AuditedFieldCode.SUBSTITUTION_MATRIX_ORIGINAL_VALUE)
    private String originalValue;

    @Getter @Setter
    @EqualityField
    @Column(name = "SUBSTITUTION_VALUE", length=256)
    @AuditedField(AuditedFieldCode.SUBSTITUTION_MATRIX_SUBSTITUTED_VALUE)
    private String substitutionValue;

    public SubstitutionMatrixElement() {

    }

    public SubstitutionMatrixElement(SubstitutionMatrixElement other){
        fileFormat = other.fileFormat;
        originalValue = other.originalValue;
        substitutionValue = other.substitutionValue;
    }

    public SubstitutionMatrixElement(FileExportFormat fileFormat, String originalValue, String substitutionValue) {
        this.fileFormat = fileFormat;
        this.originalValue = originalValue;
        this.substitutionValue = substitutionValue;
    }

}
