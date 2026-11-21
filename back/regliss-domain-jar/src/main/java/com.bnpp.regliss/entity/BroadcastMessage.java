package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.persistence.Entity;
import java.util.Optional;

@Entity
@Table(name = "BROADCAST_MESSAGE")
/**@SequenceGenerator(name = "BroadcastMessageIdGenerator", initialValue = 100, sequenceName = "BROADCAST_MESSAGE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "BroadcastMessageIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "BROADCAST_MESSAGE_SEQ")} )
public class BroadcastMessage extends AbstractSimpleEntity{

    public enum Status {
        DRAFT, PUBLISHED
    }

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "BroadcastMessageIdGenerator")
    protected Long id;

    @Getter @Setter
    @EqualityField
    @Column(name = "MESSAGE", unique = true)
    private String message;

    @Getter @Setter
    @EqualityField
    @Column(name = "TITLE")
    private String title;

    @Getter @Setter
    @EqualityField
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private Status status;

    public BroadcastMessage(){super();};
    public BroadcastMessage(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public boolean isPublished(){ return status == Status.PUBLISHED; }

    public Optional<String> getMessageOpt() { return Optional.ofNullable(message); }

}
