package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;

import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "MAPPING_COLUMN")
/**@SequenceGenerator(name = "MappingColumnIdGenerator", initialValue = 100, sequenceName = "MAP_COL_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "MappingColumnIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "MAP_COL_SEQ")} )
public class MappingColumn extends AbstractSimpleEntity {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "MappingColumnIdGenerator")
    protected Long id;
    @Getter @Setter
    @EqualityField
    @Column(name = "COLUMN_LABEL")
    private String columnLabel;

    @Getter @Setter
    @Column(name = "FIELD_CODE")
    @Enumerated(EnumType.STRING)
    @EqualityField
    private AuditedFieldCode fieldCode;

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name = "MAPPING_FILE_ID")
    private MappingFile mappingFile;

    @Getter @Setter
    @EqualityField
    @Column(name = "CHILD_INDEX")
    private Integer childIndex;

    public MappingColumn() {}

    public MappingColumn(String columnLabel, AuditedFieldCode fieldCode) {
        this.columnLabel = columnLabel;
        this.fieldCode = fieldCode;
    }

    public MappingColumn(AuditedFieldCode fieldCode) {
        this.fieldCode = fieldCode;
        this.columnLabel = fieldCode.name();
    }

    public MappingColumn(AuditedFieldCode fieldCode, int childIndex) {
        this.fieldCode = fieldCode;
        this.columnLabel = fieldCode.name();
        this.childIndex = childIndex;
    }

    public MappingColumn(MappingColumn other) {
        this.id = other.id;
        this.childIndex = other.childIndex;
        this.columnLabel = other.columnLabel;
        this.fieldCode = other.fieldCode;
    }

    public int getColumnIndex() {
        if (mappingFile == null) {return -1;}
        return mappingFile.getColumns().stream().map(MappingColumn::getId).collect(toList())
                .indexOf(this.id);
    }

    @Override
    public String toString() {
        return "Column#"+getColumnIndex()+"-"+fieldCode+
                (childIndex!=null? "#"+childIndex:"");
    }

}
