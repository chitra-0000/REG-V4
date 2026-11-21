package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;

/serial/ /* TODO: illegible, verify - appears to be a collapsed/folded comment in the IDE */
@Entity
@Table(name = "USER_LIST_PERMISSION")
/**@SequenceGenerator(name = "UserListPermissionIdGenerator", initialValue = 100, sequenceName = "USER_LIST_PEMISSION_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "UserListPermissionIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "USER_LIST_PEMISSION_SEQ")} )
public class UserListPermission extends AbstractSimpleEntity implements ToStringAuditable, Serializable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "UserListPermissionIdGenerator")
    protected Long id;
    @Getter
    @JoinColumn(name = "USER_ID")
    @ManyToOne
    private User user;

    @Getter @Setter
    @EqualityField
    @Column(name="REGLISS_LIST_ID")
    private Long reglissListId;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.USER_PERMISSION)
    @Enumerated(EnumType.STRING)
    @Column(name = "PERMISSION")
    private ListPermission permission;

    public UserListPermission() {
    }

    public UserListPermission(Long listId, User user, ListPermission permission) {
        this.reglissListId = listId;
        this.user = user;
        this.permission = permission;
    }

    public UserListPermission(Long listId, ListPermission permission) {
        this.reglissListId = listId;
        this.permission = permission;
    }

    void setUser(User user) { this.user = user; }

    @Override
    public String toStringAudit() { return "reglissListId= " + reglissListId + ", " + super.toStringAudit(); }

    @Override
    public String toString() {
        return "UserListPermission [user=" + user + ", reglissListId=" + reglissListId + ", permission=" + permission
                + ", id=" + id + "]";
    }
}
