package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.ChildType;
import com.bnpp.regliss.annotation.AuditedFieldCode.SemiAutoImportFieldCategory;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "MAPPING_FILE")
/**@SequenceGenerator(name = "MappingFileIdGenerator", initialValue = 100, sequenceName = "MAP_FILE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "MappingFileIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "MAP_FILE_SEQ")} )
public class MappingFile extends AbstractSimpleEntity implements ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "MappingFileIdGenerator")
    protected Long id;
    @Getter @Setter
    @AuditedField(AuditedFieldCode.LIST_MAPPING_FILE_NAME)
    @EqualityField
    @Column(name = "FILE_NAME")
    private String fileName;

    @Getter @Setter
    @EqualityField
    @Column(name = "IS_DELETED")
    private boolean deleted;

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name = "LIST_ID")
    private ReglissList list;

    @OneToMany(mappedBy = "mappingFile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "COLUMN_INDEX")
    private List<MappingColumn> columns = new ArrayList<>();

    public MappingFile() {}

    public MappingFile(MappingFile other) {
        this.id = other.id;
        this.fileName = other.fileName;
        this.deleted = other.deleted;

        for (MappingColumn otherColumn : other.columns) {
            addMappingColumn(new MappingColumn(otherColumn));
        }
    }

    public MappingFile removeAllColumns(AuditedFieldCode fieldCode) {
        columns.removeIf(c -> c.getFieldCode() == fieldCode);
        return this;
    }

    public List<MappingColumn> getColumnsForField(AuditedFieldCode fieldCode) {
        return columns.stream()
                .filter(c -> c.getFieldCode() == fieldCode)
                .collect(toList());
    }

    public List<List<MappingColumn>> getAllChildrenColumns() {
        Function<MappingColumn, String> childIdExtractor =
                col -> col.getFieldCode().getCategory() + "-:)" + col.getChildIndex();
        Map<String, List<MappingColumn>> columnsByChild = columns.stream()
                .filter(col -> col.getChildIndex() != null)
                .collect(groupingBy(childIdExtractor));
        return new ArrayList<>(columnsByChild.values());
    }


    public List<List<MappingColumn>> getChildrenColumns(ChildType childType) {
        Map<Integer, List<MappingColumn>> columnsByChild = columns.stream()
                .filter(col -> col.getFieldCode().getChildType() == childType)
                .collect(groupingBy(MappingColumn::getChildIndex));
        return new ArrayList<>(columnsByChild.values());
    }

    public boolean isActive() { return !deleted; }

    public List<MappingColumn> getColumns() { return Collections.unmodifiableList(this.columns); }

    public MappingFile addMappingColumn(MappingColumn mappingColumn) {
        mappingColumn.setMappingFile(this);
        this.columns.add(mappingColumn);
        return this;
    }

    public void removeMappingColumn(final MappingColumn mappingColumn) { this.columns.remove(mappingColumn); }

    public List<MappingColumn> getAllFieldsOfCategories(List<SemiAutoImportFieldCategory> categories) {
        List<MappingColumn> recordColumns = new ArrayList<>();
        for (SemiAutoImportFieldCategory semiAutoImportFieldCategory : categories) {
            recordColumns.addAll(getAllColumnsOfCategory(semiAutoImportFieldCategory));
        }
        return recordColumns;
    }

    private List<MappingColumn> getAllColumnsOfCategory(SemiAutoImportFieldCategory category) {
        return columns.stream()
                .filter(col -> col.getFieldCode().getCategory() == category)
                .collect(toList());
    }
}
