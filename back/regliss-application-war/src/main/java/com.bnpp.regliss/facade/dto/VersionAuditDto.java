package com.bnpp.regliss.facade.dto;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import org.apache.commons.lang.builder.CompareToBuilder;

import java.time.LocalDateTime;

public class VersionAuditDto implements Comparable<VersionAuditDto> {

    public LocalDateTime logDate;
    public String username;
    public AuditedAction auditedAction;
    public String returnComment;
    public String prevValue;
    public String newValue;
    public AuditedFieldCode auditedField;

    @Override
    public int compareTo(VersionAuditDto o) {
        return new CompareToBuilder()
                .append(o.logDate, logDate)
                .append(auditedAction, o.auditedAction)
                .append(username, o.username)
                .append(prevValue, o.prevValue)
                .append(newValue, o.newValue)
                .append(auditedField, o.auditedField)
                .toComparison();
    }
}

