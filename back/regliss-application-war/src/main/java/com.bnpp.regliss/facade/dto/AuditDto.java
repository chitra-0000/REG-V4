package com.bnpp.regliss.facade.dto;

// [MISSING FROM SCREENSHOTS: lines 3-7 of the original file were not visible in the photos]

public class AuditDto {

    public ObjectType objectType;
    public String reference;
    public LocalDateTime logDate;
    public String username;
    public AuditedAction auditedAction;
    public AuditedFieldCode auditedField;
    public String oldValue;
    public String newValue;

}

