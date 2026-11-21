package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.AuditedAction;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;

@Entity
@Table(name = "AUDITS")
/**@SequenceGenerator(name = "AuditIdGenerator", initialValue = 100, sequenceName = "AUDIT_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "AuditIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "AUDIT_SEQ") } )
public class Audit extends LogDetail {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "AuditIdGenerator")
    protected Long id;
    public Audit() {
    }

    public Audit(AuditedAction auditedAction) { super(auditedAction); }

    public Audit(Audit audit) { super(audit); }

}
