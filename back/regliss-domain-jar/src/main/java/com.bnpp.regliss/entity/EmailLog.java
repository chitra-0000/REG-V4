package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "EMAIL_LOG")
/**@SequenceGenerator(name = "EmailLogIdGenerator", initialValue = 100, sequenceName = "EMAIL_LOG_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "EmailLogIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "EMAIL_LOG_SEQ")} )
public class EmailLog extends AbstractSimpleEntity {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "EmailLogIdGenerator")
    protected Long id;

    @Getter
    @EqualityField
    @Column(name = "EMAIL_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailType type;

    @Getter
    @Setter
    @EqualityField
    @Column(name = "PARAMS", length = 2048)
    private String parameters;

    @Getter
    @Setter
    @EqualityField
    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;

    @Getter
    @Setter
    @EqualityField
    @Column(name = "TO_ADRESS")
    private String toAddress;

    @Getter
    @Setter
    @EqualityField
    @Column(name = "CC_ADDRESS")
    private String ccAddress;

    @Getter
    @Setter
    @EqualityField
    @Column(name = "SENDING_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private SendingStatus sendingStatus;


    public EmailLog() {
    }

    public EmailLog(EmailType type) {
        this.type = type;
        this.sendingStatus = SendingStatus.SENDING;
        this.creationDate = LocalDateTime.now();
    }

    public enum SendingStatus {
        SENDING,
        SENT,
        SENDING_FAILED
    }

}
