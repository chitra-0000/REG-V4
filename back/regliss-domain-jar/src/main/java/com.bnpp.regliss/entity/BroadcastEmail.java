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
import java.util.Comparator;

@Entity
@Table(name = "BROADCAST_EMAIL")
/**@SequenceGenerator(name = "BroadcastEmailIdGenerator", initialValue = 100, sequenceName = "BROAD_EMAIL_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "BroadcastEmailIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "BROAD_EMAIL_SEQ")} )
public class BroadcastEmail extends AbstractSimpleEntity implements Comparable<BroadcastEmail>, ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "BroadcastEmailIdGenerator")
    protected Long id;
    @Getter @Setter
    @AuditedField(AuditedFieldCode.LIST_EMAIL)
    @EqualityField
    @Column(name = "EMAIL", length = 256, nullable = false)
    private String email;

    @Getter
    @ManyToOne
    @JoinColumn(name = "LIST_ID")
    private ReglissList list;

    public BroadcastEmail() {
    }

    public BroadcastEmail(String email) { this.email = email; }

    public BroadcastEmail(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    public BroadcastEmail(BroadcastEmail other) {
        this.id = other.id;
        this.email = other.email;
    }

    void setList(ReglissList list) { this.list = list; }


    @Override
    public int compareTo(BroadcastEmail other) {
        return Comparator.comparing(BroadcastEmail::getEmail).compare(this, other);
    }
}
