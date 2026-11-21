package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "SUBSCRIPTION")
/**@SequenceGenerator(name = "SubscriptionIdGenerator", initialValue = 100, sequenceName = "SUBSCRIPTION_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "SubscriptionIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "SUBSCRIPTION_SEQ") } )
public class Subscription extends AbstractSimpleEntity implements ToStringAuditable{

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "SubscriptionIdGenerator")
    protected Long id;
    @Embedded
    @EqualityField
    private GeneratedFileFormat generatedFileFormat;

    @Getter @Setter
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "LIST_ID")
    private ReglissList list;

    @EqualityField
    @ManyToOne
    @JoinColumn(name = "SUBSCRIBER_ID")
    private Subscriber subscriber;

    @EqualityField
    @Column(name = "IS_DELETED")
    private boolean deleted;

    @Getter
    @EqualityField
    @Column(name = "CREATION_DATE")
    @CreationTimestamp
    private LocalDateTime creationDate;

    @Getter @Setter
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "CONCATENATED_LIST_ID")
    private ConcatenatedList concatenatedList;

    public Subscription() {
    }

    public Subscription(GeneratedFileFormat generatedFileFormat, ReglissList list) {
        this.generatedFileFormat = generatedFileFormat;
        this.list = list;
    }

    public Subscription(GeneratedFileFormat generatedFileFormat, ReglissList list, Subscriber subscriber, LocalDateTime creationDate) {
        this.generatedFileFormat = generatedFileFormat;
        this.list = list;
        this.subscriber = subscriber;
        this.creationDate = creationDate;
    }

    public Subscription(GeneratedFileFormat generatedFileFormat, ConcatenatedList concatenatedList) {
        this.generatedFileFormat = generatedFileFormat;
        this.concatenatedList = concatenatedList;
    }

    public Subscription(GeneratedFileFormat generatedFileFormat, ConcatenatedList concatenatedList, Subscriber subscriber, LocalDateTime creationDate) {
        this.generatedFileFormat = generatedFileFormat;
        this.concatenatedList = concatenatedList;
        this.subscriber = subscriber;
        this.creationDate = creationDate;
    }

    public Subscription(Subscription other) {
        this.id = other.id;
        this.generatedFileFormat = new GeneratedFileFormat(other.generatedFileFormat);
        this.list = other.list;
        this.deleted = other.deleted;
        this.creationDate = other.creationDate;
        this.concatenatedList = other.concatenatedList;
    }

    public boolean isNotOfac() { return list != null; }

    public Optional<ReglissList> getListOpt() { return Optional.ofNullable(list); }

    public Subscriber getSubscriber() { return subscriber; }

    public void setSubscriber(Subscriber subscriber) { this.subscriber = subscriber; }

    public GeneratedFileFormat getGeneratedFileFormat() { return this.generatedFileFormat; }

    public void setDeleted(boolean isDeleted) { this.deleted = isDeleted; }

    public boolean isNotDeleted() { return !deleted; }

    public boolean isDeleted() { return deleted; }

    @Override
    public String toStringAudit() {
        return "list= " + (list != null ? list.getShortName() : "Global plus OFAC") + ", "  + generatedFileFormat.toStringAudit();
    }

    public boolean hasDeliveryReport() { return (subscriber != null && subscriber.hasDeliveryReport()); }

    public boolean isValidSubscriptionToList(Long listId) {
        return isNotDeleted() && list != null && list.getId().equals(listId) && creationDate != null;
    }
}
