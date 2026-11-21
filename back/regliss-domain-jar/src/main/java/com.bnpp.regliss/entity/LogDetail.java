package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.record.AuditableRecord;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class LogDetail extends AbstractSimpleEntity {

    /**
     * Type of object or relation modified
     */
    @Getter @Setter
    @EqualityField
    @Column(name = "OBJECT_TYPE") // , nullable = false)
    @Enumerated(EnumType.STRING)
    private ObjectType objectType;

    @Getter @Setter
    @EqualityField
    @Column(name = "REFERENCE")
    private String reference;

    @Getter @Setter
    @EqualityField
    @Column(name = "LOG_DATE")
    private LocalDateTime logDate;

    @Getter @Setter
    @EqualityField
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "USER_ID") // , nullable = false)
    private User user;

    @Getter @Setter
    @EqualityField
    @Column(name = "AUDITED_ACTION") // , nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditedAction auditedAction;

    @Getter @Setter
    @EqualityField
    @Column(name = "AUDITED_FIELD")
    @Enumerated(EnumType.STRING)
    private AuditedFieldCode auditedField;

    @Getter @Setter
    @EqualityField
    @Column(name = "OLD_VALUE", length = 1997)
    private String oldValue;

    @Getter @Setter
    @EqualityField
    @Column(name = "NEW_VALUE", length = 1997)
    private String newValue;

    @Getter @Setter
    @EqualityField
    @Column(name = "OBJECT_ID")
    private Long objectId;

    @Getter @Setter
    @EqualityField
    @Column(name = "CHILD_ID")
    private Long childId;

    @Getter @Setter
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "VERSION_ID")
    private Version version;

    @Getter @Setter
    @EqualityField
    @Column(name = "RECORD_NAME", length = 300)
    private String recordName;

    @Getter @Setter
    private transient AuditableRecord object;

    @Getter @Setter
    private transient AbstractSimpleEntity child;

    public LogDetail() {}

    public LogDetail(AuditedAction auditedAction) { this.auditedAction = auditedAction; }

    public LogDetail(LogDetail logDetail) {
        super();
        this.objectType = logDetail.objectType;
        this.logDate = logDetail.logDate;
        this.user = logDetail.user;
        this.auditedAction = logDetail.auditedAction;
        this.auditedField = logDetail.auditedField;
        this.oldValue = logDetail.oldValue;
        this.newValue = logDetail.newValue;
        this.object = logDetail.object;
        this.objectId = logDetail.objectId;
        this.child = logDetail.child;
        this.childId = logDetail.childId;
        this.version = logDetail.version;
        this.reference = logDetail.reference;
        this.recordName = logDetail.recordName;
    }

    public String exportForComparison() {
        return "LogDetail [objectType=" + objectType + ", auditedAction=" + auditedAction + ", auditedField=" + auditedField + ", oldValue=" + oldValue
                + ", newValue=" + newValue + ", version=" + version + ", recordName=" + recordName + "]";
    }

    @Override
    public String toString() {
        return "LogDetail [objectType=" + objectType + ", reference=" + reference + ", logDate=" + logDate + ", user="
                + user + ", auditedAction=" + auditedAction + ", auditedField=" + auditedField + ", oldValue="
                + oldValue + ", newValue=" + newValue + ", objectId=" + objectId + ", childId=" + childId + ", child="+child+"]";
    }

    // TODO: [GAP] lines 126-end not captured in screenshots — the class closing brace was not visible.
}
