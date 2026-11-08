package com.bnpp.regliss.facade.dto.reference;

import com.bnpp.regliss.annotation.AuditedFieldCode.SemiAutoImportFieldCategory;

public class FieldRef implements ReferentialValue {
    public String id;
    public SemiAutoImportFieldCategory category;
    public String label;
    public int index;

    @Override
    public Object getId() { return id; }

    @Override
    public String getLabel() { return label; }
}

