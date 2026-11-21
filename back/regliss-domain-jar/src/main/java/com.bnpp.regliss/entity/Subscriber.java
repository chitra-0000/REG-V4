package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import com.bnpp.regliss.entity.referential.WatchlistBroadcastFrequency;
import com.bnpp.regliss.entity.referential.record.Country;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Entity;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "SUBSCRIBER")
/**@SequenceGenerator(name = "SubscriberIdGenerator", initialValue = 100, sequenceName = "SUBSCRIBER_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "SubscriberIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "SUBSCRIBER_SEQ")} )
public class Subscriber extends AbstractSimpleEntity implements ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "SubscriberIdGenerator")
    protected Long id;
    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBSCRIBER_NAME)
    @Column(name = "NAME", length = 64)
    private String name;

    @Column(name = "NAME_NORMALIZED", length = 128)
    private String nameNormalized;

    @Getter @Setter
    @EqualityField
    // TODO: [GAP] lines 42-? not captured in screenshots — annotation/field preceding DESCRIPTION not fully visible
    @AuditedField(AuditedFieldCode.SUBSCRIBER_DESCRIPTION)
    @Column(name = "DESCRIPTION", length = 512)
    private String description;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBSCRIBER_CONTACT)
    @Column(name = "CONTACT", length = 128)
    private String contact;

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBSCRIBER_ENTITY_OR_SECTOR)
    @Column(name = "ENTITY_OR_SECTOR", length = 64)
    private String entityOrSector;

    @Column(name = "ENTITY_OR_SECTOR_NORMALIZED", length = 128)
    private String entityOrSectorNormalized;

    @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBSCRIBER_COUNTRY)
    @ManyToOne
    @JoinColumn(name = "COUNTRY_ID")
    private Country country;

    @UpdateTimestamp
    @Getter @Setter
    @EqualityField
    @Column(name = "MODIFICATION_DATE")
    private LocalDateTime modificationDate;

    @Getter @Setter
    @Column(name = "IS_ACTIVE")
    private boolean active;

    @AuditedField(AuditedFieldCode.SUBSCRIBER_HAS_DELIVERY_REPORT)
    @Column(name = "HAS_DELIVERY_REPORT")
    private boolean deliveryReport;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBSCRIBER_HOUR_OF_DIFFUSION)
    @Column(name = "DIFFUSION_TIME")
    private String diffusionTime;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBSCRIBER_PARTNER)
    @Column(name = "CFT_PARTNER", length = 24)
    private String cftPartner;

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBSCRIBER_FLUX_NAME)
    @Column(name = "CFT_FLUX_NAME", length = 8)
    private String cftFluxName;

    @Column(name = "CFT_FLUX_NAME_NORMALIZED", length = 16)
    private String cftFluxNameNormalized;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBSCRIBER_WL_FULL_BROADCAST_FREQUENCY)
    @Column(name = "WL_FULL_BROADCAST_FREQUENCY")
    @Enumerated(EnumType.STRING)
    private WatchlistBroadcastFrequency wlFullBroadcastFrequency;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBSCRIBER_WL_DELTA_BROADCAST_FREQUENCY)
    @Column(name = "WL_DELTA_BROADCAST_FREQUENCY")
    @Enumerated(EnumType.STRING)
    private WatchlistBroadcastFrequency wlDeltaBroadcastFrequency;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBSCRIBER_ENCRYPTION)
    @Column
    private boolean encryption = false;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBSCRIBER_RECEIVER_KEY_ID)
    @Column(name = "RECEIVER_KEY_ID", length = 10)
    private String receiverKeyId;

    @AuditedCollection(createAction = AuditedAction.SUBSCRIPTION_ADDITION,
            updateAction = AuditedAction.SUBSCRIPTION_MODIFICATION,
            deleteAction = AuditedAction.SUBSCRIPTION_DELETION)
    @OneToMany(mappedBy = "subscriber", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subscription> subscriptions = new ArrayList<>();

    public Subscriber() {
    }

    public Subscriber(Subscriber other) {
        this.id = other.id;
        this.name = other.name;
        this.nameNormalized = other.nameNormalized;
        this.cftFluxName = other.cftFluxName;
        this.cftFluxNameNormalized = other.cftFluxNameNormalized;
        this.description = other.description;
        this.active = other.active;
        this.deliveryReport =  other.deliveryReport;
        this.contact = other.contact;
        this.entityOrSector = other.entityOrSector;
        this.entityOrSectorNormalized = other.entityOrSectorNormalized;
        this.modificationDate = other.modificationDate;
        this.diffusionTime = other.diffusionTime;
        this.country = other.country;
        this.cftPartner = other.cftPartner;
        this.wlFullBroadcastFrequency = other.wlFullBroadcastFrequency;
        this.wlDeltaBroadcastFrequency = other.wlDeltaBroadcastFrequency;
        this.encryption = other.encryption;
        this.receiverKeyId = other.receiverKeyId;

        for (Subscription otherSubscription : other.subscriptions) {
            addSubscription(new Subscription(otherSubscription));
        }
    }

    public List<Subscription> getSubscriptions() { return Collections.unmodifiableList(this.subscriptions); }

    public void removeSubscription(final Subscription subscription) { subscription.setDeleted(true); }

    public void removeSubscription(final Long subscriptionId) {
        this.subscriptions.stream().filter(s->s.id.equals(subscriptionId)).forEach(s->s.setDeleted(true));
    }

    public void addSubscription(Subscription subscription) {
        subscription.setSubscriber(this);
        this.subscriptions.add(subscription);
    }

    public Optional<Country> getCountryOpt() { return Optional.ofNullable(country); }

    public boolean hasDeliveryReport() { return deliveryReport; }

    public void setDeliveryReport(boolean deliveryReportParam) { deliveryReport = deliveryReportParam; }

    public Optional<LocalDateTime> getListSubscriptionCreationDate(Long listId) {
        return getSubscriptions().stream()
                .filter(s -> s.isValidSubscriptionToList(listId))
                .map(Subscription::getCreationDate)
                .sorted()
                .findFirst();
    }

    public void setName(String name) {
        this.name = name;
        this.nameNormalized = TextNormalizerQueryUtil.normalize(name);
    }

    public void setEntityOrSector(String entityOrSector) {
        this.entityOrSector = entityOrSector;
        this.entityOrSectorNormalized = TextNormalizerQueryUtil.normalize(entityOrSector);
    }

    public void setCftFluxName(String cftFluxName) {
        this.cftFluxName = cftFluxName;
        this.cftFluxNameNormalized = TextNormalizerQueryUtil.normalize(cftFluxName);
    }
}
