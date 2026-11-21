package com.bnpp.regliss.entity;

import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.Scope;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;

@Entity
@Table(name = "LIST_PERMISSION_MATRIX",
        indexes = { @Index(columnList = "PROFILE"), @Index(columnList = "CONFIDENTIALITY"),@Index(columnList = "SCOPE")})
/**@SequenceGenerator(name = "ListPermissionMatrixIdGenerator", initialValue = 100, sequenceName = "LIST_PERM_MAT_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "ListPermissionMatrixIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "LIST_PERM_MAT_SEQ")} )
public class ListPermissionMatrix extends AbstractSimpleEntity{

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "ListPermissionMatrixIdGenerator")
    protected Long id;
    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "PROFILE")
    private Profile profile;

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "CONFIDENTIALITY")
    private Confidentiality confidentiality;

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "SCOPE")
    private Scope scope;

    @Getter @Setter
    @Column(name = "SAME_COUNTRY")
    private Boolean sameCountry;

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "DEFAULT_PERM")
    private ListPermission defaultPerm;

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "ASSIGNABLE_PERM")
    private ListPermission assignablePerm;


    public ListPermissionMatrix() { super(); }

    public ListPermissionMatrix(Profile profile, Confidentiality confidentiality, boolean sameCountry, Scope scope){
        super();
        this.profile=profile;
        this.confidentiality = confidentiality;
        this.sameCountry = sameCountry;
        this.scope = scope;
    }

    public ListPermissionMatrix(ListPermissionMatrix other){
        this.profile=other.profile;
        this.confidentiality = other.confidentiality;
        this.sameCountry = other.sameCountry;
        this.scope = other.scope;
        this.assignablePerm=other.assignablePerm;
        this.defaultPerm=other.defaultPerm;
    }

    public ListPermissionMatrix merge(ListPermissionMatrix other){
        this.assignablePerm = this.assignablePerm != null ? this.assignablePerm : other.assignablePerm;
        this.defaultPerm = this.defaultPerm != null ? this.defaultPerm : other.defaultPerm;
        return this;
    }
}
