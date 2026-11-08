package com.bnpp.regliss.facade.dto.list;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class MappingColumnDto {

    public String columnLabel;
    public AuditedFieldCode recordFieldCode;

    public MappingColumnDto(){}

    public MappingColumnDto(MappingColumn columnEntity){
        columnLabel = columnEntity.getColumnLabel();
        recordFieldCode = columnEntity.getFieldCode();
    }

    public MappingColumnDto(String columnLabel) { this.columnLabel = columnLabel; }

    public MappingColumnDto(String columnLabel, AuditedFieldCode recordFieldCode) {
        super();
        this.columnLabel = columnLabel;
        this.recordFieldCode = recordFieldCode;
    }
}

