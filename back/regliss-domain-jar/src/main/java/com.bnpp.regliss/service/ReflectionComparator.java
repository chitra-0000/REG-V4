package com.bnpp.regliss.service;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.AuditedCollection;
import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.Audit;
import com.bnpp.regliss.entity.record.Address;
import com.bnpp.regliss.entity.record.Alias;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import com.bnpp.regliss.utils.ReflectionCachedUtils;
import com.bnpp.regliss.utils.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;

public class ReflectionComparator {

    private static final String ADDRESSES_FIELD = "addresses";

    private ReflectionComparator() {}

    private static final Logger log = LoggerFactory.getLogger(ReflectionComparator.class);

    private final static int fieldLength = 4000; //NOSONAR

    public static List<Audit> compare(Object object1, Object object2) {
        List<Audit> audits = new ArrayList<>();
        audits.addAll(getAuditsForAnnotatedFields(object1, object2));
        audits.addAll(getAuditsForAnnotatedCollections(object1, object2, false, false));
        return audits;
    }

    // TODO: [GAP - line 42 (method signature) not captured in screenshots]
        List<Audit> audits = new ArrayList<>();
        audits.addAll(getAuditsForAnnotatedFields(object1, object2));
        audits.addAll(getAuditsForAnnotatedCollections(object1, object2, isManualImport, isCustomImport));
        return audits;
    }

    private static List<Audit> getAuditsForAnnotatedFields(Object object1, Object object2) {
        List<Audit> audits = new ArrayList<>();
        List<Field> annotatedFields = ReflectionCachedUtils.getAnnotatedFields(object1.getClass(), AuditedField.class);
        for (Field field : annotatedFields) {
            Object v1 = ReflectionUtils.getFieldValueForComparison(field, object1);
            Object v2 = ReflectionUtils.getFieldValueForComparison(field, object2);
            if (different(v1, v2)) {
                Audit audit = new Audit();
                audit.setAuditedField(field.getAnnotation(AuditedField.class).value());
                audit.setOldValue(getStringValue(ReflectionUtils.getFieldValue(field, object1)));
                audit.setNewValue(getStringValue(ReflectionUtils.getFieldValue(field, object2)));
                audits.add(audit);
            }
        }

        return audits;
    }

    /*unchecked*/
    private static List<Audit> getAuditsForAnnotatedCollections(Object object1, Object object2, boolean isManualImport, boolean isCustomImport) {
        List<Audit> audits = new ArrayList<>();
        List<Field> annotatedCollections = ReflectionCachedUtils.getAnnotatedFields(object1.getClass(),
                AuditedCollection.class);

        for (Field field : annotatedCollections) {
            Collection<AbstractSimpleEntity> oldCollection = ReflectionUtils.getFieldValue(field, object1);
            Collection<AbstractSimpleEntity> newCollection = ReflectionUtils.getFieldValue(field, object2);

            if (field.getName().equals(ADDRESSES_FIELD)) {
                EntityCollectionDiff<AbstractSimpleEntity> diff = new EntityCollectionDiff<>(oldCollection, newCollection, equalityCriteriaExtractor(field, isManualImport,
                        true, isCustomImport));
                audits.addAll(buildAuditsFromDiff(field, diff, true));
            }
            EntityCollectionDiff<AbstractSimpleEntity> diff = new EntityCollectionDiff<>(oldCollection, newCollection, equalityCriteriaExtractor(field, isManualImport,
                    false, isCustomImport));
            audits.addAll(buildAuditsFromDiff(field, diff, false));
        }

        return audits;
    }

    private static List<Audit> buildAuditsFromDiff(Field field, EntityCollectionDiff<AbstractSimpleEntity> diff, boolean minorUpdates) {
        List<Audit> audits = new ArrayList<>();
        log.trace("Comparing collections {}: {}", field.getDeclaringClass().getSimpleName()+'.'+field.getName(), diff);
        AuditedAction createAction = field.getAnnotation(AuditedCollection.class).createAction();
        AuditedAction updateAction = field.getAnnotation(AuditedCollection.class).updateAction();
        AuditedAction deleteAction = field.getAnnotation(AuditedCollection.class).deleteAction();
        AuditedFieldCode zipFieldCode = (minorUpdates) ? AuditedFieldCode.RECORD_ADDRESS_ZIP_CODE : null;

        List<AbstractSimpleEntity> createdElements = diff.getCreatedElements();
        List<AbstractSimpleEntity> deletedElements = diff.getDeletedElements();

        for (AbstractSimpleEntity toCreate : createdElements) {
            audits.add(createAddInCollectionAudit(toCreate, createAction, zipFieldCode));
        }

        if (!field.getName().equals(ADDRESSES_FIELD)) {
            for (Map.Entry<AbstractSimpleEntity, AbstractSimpleEntity> toModify : diff.getUpdatedElements().entrySet()) {
                audits.addAll(createModifyInCollectionAudit(toModify.getKey(), toModify.getValue(), updateAction));
            }
        }

        for (AbstractSimpleEntity toDelete : deletedElements) {
            audits.add(createDeleteFromCollectionAudit(toDelete, deleteAction, zipFieldCode));
        }

        return audits;
    }

    private static Function<AbstractSimpleEntity, List<Object>> equalityCriteriaExtractor(Field field, boolean isManualImport, boolean minorUpdates, boolean isCustomImport) {
        Function<AbstractSimpleEntity, List<Object>> equalityCriteriaExtractor = e -> asList((isCustomImport) ? e : e.getId());
        if(isManualImport && field.getName().equals("aliases")){
            equalityCriteriaExtractor = e -> asList(((Alias) e).getExternalReference());
        }
        if (field.getName().equals(ADDRESSES_FIELD)) {
            equalityCriteriaExtractor = e -> {

                Address a = Address.class.cast(e);
                if (minorUpdates) {
                    return asList(Arrays.asList(a.getZipCode(), a.getAddress()));
                } else {
                    return asList(Arrays.asList(a.getCountryOpt().orElse( null), a.getCity(),a.getState()));
                }
            };
        }
        return equalityCriteriaExtractor;
    }

    public static Audit createAddInCollectionAudit(AbstractSimpleEntity toCreate, AuditedAction createAction) {
        Audit createAudit = new Audit(createAction);
        createAudit.setNewValue(getStringValue(toCreate));
        createAudit.setChildId(toCreate.getId());
        createAudit.setChild(toCreate);
        return createAudit;
    }

    public static List<Audit> createModifyInCollectionAudit(AbstractSimpleEntity oldValue, AbstractSimpleEntity newValue, AuditedAction updateAction) {
        List<Audit> audits = getAuditsForAnnotatedFields(oldValue, newValue);
        for (Audit audit : audits) {
            audit.setAuditedAction(updateAction);
            audit.setChildId(newValue.getId());
            audit.setChild(newValue);
        }
        return audits;
    }

    public static Audit createDeleteFromCollectionAudit(AbstractSimpleEntity toDelete, AuditedAction deleteAction) {
        Audit deleteAudit = new Audit(deleteAction);
        deleteAudit.setOldValue(getStringValue(toDelete));
        deleteAudit.setChildId(toDelete.getId());
        deleteAudit.setChild(toDelete);
        return deleteAudit;
    }

    private static Audit createAddInCollectionAudit(AbstractSimpleEntity entity, AuditedAction action, AuditedFieldCode auditedFieldCode) {
        Audit audit = new Audit(action);
        audit.setNewValue(getStringValue(entity));
        audit.setChildId(entity.getId());
        audit.setChild(entity);
        audit.setAuditedField(auditedFieldCode);
        return audit;
    }

    private static Audit createDeleteFromCollectionAudit(AbstractSimpleEntity entity, AuditedAction action, AuditedFieldCode auditedFieldCode) {
        Audit audit = new Audit(action);
        audit.setOldValue(getStringValue(entity));
        audit.setChildId(entity.getId());
        audit.setChild(entity);
        audit.setAuditedField(auditedFieldCode);
        return audit;
    }

    private static boolean different(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return false;
        }
        if (value1 == null || value2 == null) {
            return true;
        }
        if (value1 instanceof LocalDateTime) {
            return ((LocalDateTime) value1).toLocalDate().isEqual(((LocalDateTime) value2).toLocalDate());
        }
        return !value1.equals(value2);
    }

    private static String getStringValue(Object fieldValue) {
        String stringValue = null;
        if (fieldValue != null) {
            if (fieldValue instanceof String || fieldValue instanceof Enum<?>) {
                stringValue = fieldValue.toString();
            } else if (fieldValue instanceof ToStringAuditable) {
                stringValue = ((ToStringAuditable) fieldValue).toStringAudit();
            } else if (fieldValue instanceof LocalDateTime) {
                stringValue = ((LocalDateTime) fieldValue).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else{
                stringValue = String.valueOf(fieldValue);
            }
        }

        if(stringValue != null && stringValue.length() > fieldLength){
            stringValue = stringValue.substring(0, fieldLength);
        }
        return stringValue;
    }
}
