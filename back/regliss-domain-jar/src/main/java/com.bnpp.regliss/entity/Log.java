package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.AuditedAction;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;

@Entity
@Table(name = "LOGS")
/**@SequenceGenerator(name = "LogIdGenerator", initialValue = 100, sequenceName = "LOG_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "LogIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "LOG_SEQ")} )
public class Log extends LogDetail {

    @Id
    @Getter
    @Setter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LogIdGenerator")
    protected Long id;

    public Log() { super(); }

    public Log(AuditedAction auditedAction) { super(auditedAction); }

    public Log(Log log) { super(log); }

}
