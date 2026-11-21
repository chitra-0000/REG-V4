package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.persistence.Entity;

@Entity
@Table(name = "ADD_MOD_SUP_AUDIT_TRAIL_FILE")
/**@SequenceGenerator(name = "AddModSupAuditTrailFileIdGenerator", initialValue = 100, sequenceName = "ADD_MOD_SUP_AUDIT_TRAIL_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "AddModSupAuditTrailFileIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "ADD_MOD_SUP_AUDIT_TRAIL_SEQ") } )
public class AddModSupAuditTrailFile extends ExportFile{

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "AddModSupAuditTrailFileIdGenerator")
    protected Long id;

    @Getter @Setter
    @EqualityField
    @Column(name = "IS_DEPRECATED")
    private boolean deprecated;

    public AddModSupAuditTrailFile(){super();}

    public boolean isDeprecated() { return deprecated; }
}
