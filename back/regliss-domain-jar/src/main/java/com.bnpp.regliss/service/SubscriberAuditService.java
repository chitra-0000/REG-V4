package com.bnpp.regliss.service;

import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bnpp.regliss.search.FileDeliverySearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.FileDelivery;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.Log;
import com.bnpp.regliss.entity.LogDetail;
import com.bnpp.regliss.entity.ObjectType;
import com.bnpp.regliss.entity.Subscriber;
import com.bnpp.regliss.entity.Subscription;
import com.bnpp.regliss.repository.AuditRepository;
import com.bnpp.regliss.repository.LogRepository;
import com.bnpp.regliss.search.SubscriberSearchCriteria;
import com.bnpp.regliss.utils.AuditUtil;

@Service
public class SubscriberAuditService {

    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private LogRepository logRepo;

    @Autowired
    private ReglissRequestContext requestContext;

    public void auditCreate(Subscriber subscriber) {
        auditRepo.save(fill(new Audit(AuditedAction.SUBSCRIBER_CREATION), subscriber));
        if (subscriber.isActive()) {
            auditRepo.save(fill(new Audit(AuditedAction.SUBSCRIBER_ACTIVATION), subscriber));
        }
    }

    public void auditUpdate(Subscriber oldSubscriber, Subscriber newSubscriber) {
        auditActivation(oldSubscriber, newSubscriber);
        auditFieldsChanged(oldSubscriber, newSubscriber);
    }

    public void auditDeliveryReportGeneration(GeneratedFile generatedFile) {
        Audit audit = fill(new Audit(AuditedAction.SUBSCRIBER_DELIVERY_REPORT_GENERATION), generatedFile.getSubscriberOpt()
                .orElseThrow(() -> new IllegalArgumentException("Subscriber cannot be null")));
        audit.setChildId(generatedFile.getId());
        audit.setNewValue(generatedFile.getGenerationStatus().toString());
        auditRepo.save(audit);
    }

    public void auditDeliveryReportDelivery(FileDelivery fileDelivery) {
        Audit audit = fill(new Audit(AuditedAction.SUBSCRIBER_DELIVERY_REPORT_DELIVERY), fileDelivery.getGeneratedFile()
                .getSubscriberOpt().orElseThrow(() -> new IllegalArgumentException("Subscriber cannot be null")));
        audit.setChildId(fileDelivery.getId());
        audit.setNewValue(fileDelivery.getStatus().toString());
        auditRepo.save(audit);
    }

    private void auditFieldsChanged(Subscriber oldSubscriber, Subscriber newSubscriber) {
        List<Audit> audits = ReflectionComparator.compare(oldSubscriber, newSubscriber);
        audits.addAll(getAuditForSubscriptionsDelete(oldSubscriber, newSubscriber));
        for (Audit audit : audits) {
            if(audit.getAuditedAction() == null) {
                audit.setAuditedAction(AuditedAction.SUBSCRIBER_MODIFICATION);
            }
            auditRepo.save(fill(audit, oldSubscriber));
        }
    }

    private List<Audit> getAuditForSubscriptionsDelete(Subscriber oldSubscriber, Subscriber newSubscriber) {
        List<Audit> audits = new ArrayList<>();
        List<Subscription> deletedSubscriptions = newSubscriber.getSubscriptions().stream()
            .filter(newS -> oldSubscriber.getSubscriptions().stream()
                .anyMatch(old -> old.getId().equals(newS.getId()) && old.isNotDeleted() && newS.isDeleted()))
            .collect(toList());
        for (Subscription deletedSubscription : deletedSubscriptions) {
            audits.add(ReflectionComparator.createDeleteFromCollectionAudit(deletedSubscription, AuditedAction.SUBSCRIPTION_DELETION));
        }
        return audits;
    }

    private void auditActivation(Subscriber oldSubscriber, Subscriber newSubscriber) {
        if (newSubscriber.isActive() == oldSubscriber.isActive()) {
            return; // nothing to do
        }

        if (newSubscriber.isActive()) {
            auditRepo.save(fill(new Audit(AuditedAction.SUBSCRIBER_ACTIVATION), oldSubscriber));
        }
        if (!newSubscriber.isActive()) {
            auditRepo.save(fill(new Audit(AuditedAction.SUBSCRIBER_DEACTIVATION), oldSubscriber));
        }
    }

    private <T extends LogDetail> T fill(T audit, Subscriber subscriber) {
        audit.setUser(requestContext.getCurrentUserRef());
    audit.setLogDate(LocalDateTime.now());
    audit.setObjectType(ObjectType.SUBSCRIBER);
    audit.setReference(subscriber.getName());
    audit.setObjectId(subscriber.getId());
    return audit;
}


public void logSubscriberSearch(SubscriberSearchCriteria criteriaObject) {
    Log log = new Log(AuditedAction.SUBSCRIBER_SEARCH);
    log.setLogDate(requestContext.getRequestTime());
    log.setUser(requestContext.getCurrentUserRef());
    log.setObjectType(ObjectType.SUBSCRIBER);
    log.setNewValue(AuditUtil.criteriaToAuditString(criteriaObject));
    logRepo.save(log);
}


public void logDiffusionLoad() {
    Log log = new Log(AuditedAction.DIFFUSION_SEARCH);
    log.setLogDate(requestContext.getRequestTime());
    log.setUser(requestContext.getCurrentUserRef());
    log.setObjectType(ObjectType.DIFFUSION);
    logRepo.save(log);
}

public void logDiffusionSearch(FileDeliverySearchCriteria criteria) {
    Log log = new Log(AuditedAction.DIFFUSION_SEARCH);
    log.setLogDate(requestContext.getRequestTime());
    log.setUser(requestContext.getCurrentUserRef());
    log.setObjectType(ObjectType.DIFFUSION);
    log.setNewValue(AuditUtil.criteriaToAuditString(criteria));
    logRepo.save(log);
}

}
