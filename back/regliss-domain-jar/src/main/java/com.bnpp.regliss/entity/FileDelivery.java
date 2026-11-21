package com.bnpp.regliss.entity;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.persistence.Entity;
import java.time.LocalDateTime;

@Entity
@Table(name = "FILE_DELIVERY")
/**@SequenceGenerator(name = "FileDeliveryIdGenerator", initialValue = 100, sequenceName = "FILE_DELIVERY_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "FileDeliveryIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "FILE_DELIVERY_SEQ")} )
public class FileDelivery extends AbstractSimpleEntity{

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "FileDeliveryIdGenerator")
    protected Long id;

    public enum Status {
        SCHEDULED, IN_PROGRESS, OK, KO
    }

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name="GENERATED_FILE_ID")
    private GeneratedFile generatedFile;

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name="SUBSCRIPTION_ID")
    private Subscription subscription;

    @Getter @Setter
    @Column(name = "SCHEDULED_RUNTIME")
    private LocalDateTime scheduledRunTime;

    @Getter @Setter
    @Column(name = "ACTUAL_RUNTIME")
    private LocalDateTime actualRunTime;

    @Getter @Setter
    @Column(name="ACK_FILE")
    @Lob
    private String ackFile;

    @Getter @Setter
    @Column(name="CFT_LOG_TIME_ID")
    private String cftLogTimeId;

    @Getter @Setter
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private Status status = Status.SCHEDULED;

    @Getter @Setter
    @Column(name = "RETRIES_LEFT")
    private int retriesLeft;

    @Getter @Setter
    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Getter @Setter
    @Column(name = "RETURN_CODE")
    private Integer returnCode;

    @Getter @Setter
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "USER_ID")
    private User deliveryUser;

    public FileDelivery() {
    }

    public FileDelivery(GeneratedFile generatedFile, Subscription subscription, LocalDateTime scheduledRunTime,
            User deliveryUser, int retriesLeft) {
        this.generatedFile = generatedFile;
        this.subscription = subscription;
        this.scheduledRunTime = scheduledRunTime;
        this.deliveryUser = deliveryUser;
        this.retriesLeft = retriesLeft;
    }

    public boolean isInProgress() { return status == Status.IN_PROGRESS; }

    public boolean isScheduled() { return status == Status.SCHEDULED; }

    public boolean isKo() { return status == Status.KO; }

    public boolean isOk() { return status == Status.OK; }

    public boolean isDeliveryReportFile() { return generatedFile.getSubscriberOpt().isPresent(); }

    public Subscriber getSubscriber() {
        if (generatedFile.isDeliveryReport()) {
            return generatedFile.getSubscriberOpt()
                    .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        } else {
            return subscription.getSubscriber();
        }
    }

}
