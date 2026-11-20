package com.bnpp.regliss.importer;


import com.bnpp.regliss.annotation.AuditedCollection;
import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.Entity;
import com.bnpp.regliss.entity.record.Alias;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.record.RecordReferential;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.service.EntityCollectionDiff;
import com.bnpp.regliss.utils.EntityUtils;
import com.bnpp.regliss.utils.ReflectionCachedUtils;
import com.bnpp.regliss.utils.ReflectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@ReglissBatchProfile
public class RecordCopyOnWriteBatchService {

    private static final Logger logger = LoggerFactory.getLogger(RecordCopyOnWriteBatchService.class);

    @Autowired
    private EntityManager em;

    /unchecked/
    public <T extends AbstractSimpleEntity> Record linkOrCreateChildren(Record newRecord, Record oldRecord, boolean isManualImport) {
        logger.trace("Merge or clone children of record: {}", newRecord.getExternalReference());
        for (Field field : ReflectionCachedUtils.getAnnotatedFields(newRecord.getClass(), AuditedCollection.class)) {
            Method getter = ReflectionCachedUtils.getGetter(field);
            Set<T> oldChildren = (Set<T>) ReflectionUtils.invoke(oldRecord, getter);
            Set<T> newChildren = (Set<T>) ReflectionUtils.invoke(newRecord, getter);

            Set<T> mergedChildren = mergeUnsharedChildren(newRecord, newChildren, oldChildren, isManualImport);

            Set<AbstractSimpleEntity> realSet = ReflectionUtils.getFieldValue(field, newRecord);
            realSet.clear();
            realSet.addAll(mergedChildren.stream().map(this::reattachChild).collect(toSet()));
        }
        return newRecord;
    }

    private AbstractSimpleEntity reattachChild(AbstractSimpleEntity child) {
        if (child.getId() == null) {
            return child; // newly created child
        } else if (child instanceof RecordReferential) {
            return child; // getReference from TransformedHandler. We never link to 'previous version' of label
        } else {
            logger.trace("Reattaching unchanged child: {}", child);
            return em.getReference(child.getClass(), child.getId());
        }
    }

    private <T extends AbstractSimpleEntity> Set<T> mergeUnsharedChildren(Record newRecord, Set<T> newChildren, Set<T> oldChildren,
                                                                          boolean isManualImport) {
        EntityCollectionDiff<T> diff = new EntityCollectionDiff<>(oldChildren, newChildren,
                entity -> getCorrelationFieldValues(entity, isManualImport, oldChildren));

        Set<T> mergedChildren = new HashSet<>();

        logger.trace("Diff between old:{} and new:{} is {}", oldChildren, newChildren, diff);

        for (T toCreate : diff.getCreatedElements()) {
            logger.trace("Will persist child: {} ", toCreate);
            mergedChildren.add(toCreate);
        }

        for (Map.Entry<T, T> toModify : diff.getUpdatedElements().entrySet()) {
            T databaseChild = toModify.getKey();
            T newChild = toModify.getValue();
            if (getEqualityFieldValues(newChild).equals(getEqualityFieldValues(databaseChild))) {
                logger.trace("Child not changed, linking cloned record to previous child: {}", databaseChild);
                mergedChildren.add(databaseChild);
            } else {
                // At CSV Manual Import, Aliases can be UPDATED based on their external reference
                logger.trace("Old child changed ({}). Persisted cloned child: {}", databaseChild, newChild);
                ((Alias)databaseChild).removeFromParent(newRecord);
                ((Alias)newChild).copyUnmodifiableFieldsFrom(((Alias)databaseChild));
                mergedChildren.add(newChild);
            }
        }

        for (T toDelete : diff.getDeletedElements()) {
            logger.trace("Will unlink child: {}", toDelete);
            // NOTHING
        }
        return mergedChildren;
    }

    private List<Object> getCorrelationFieldValues(Entity<?> entity, boolean isManualImport, Set<?> oldChildren) {
        if (isManualImport && !oldChildren.isEmpty() && oldChildren.iterator().next() instanceof Alias) {
            return asList(((Alias)entity).getExternalReference());
        }
        return getEqualityFieldValues(entity);
    }

    private static final List<AuditedFieldCode> GENERATED_FIELDS = asList(
                    AuditedFieldCode.RECORD_EXTERNAL_REFERENCE,
                    AuditedFieldCode.RECORD_REGLISS_REFERENCE,
                    AuditedFieldCode.RECORD_ALIAS_EXTERNAL_REFERENCE,
                    AuditedFieldCode.RECORD_ALIAS_REGLISS_REFFERENCE,
                    AuditedFieldCode.RECORD_ADDRESS_REGLISS_REFERENCE,
                    AuditedFieldCode.RECORD_ID
    );
    private List<Object> getEqualityFieldValues(Entity<?> entity) {
        if (entity instanceof RecordReferential) {
            return asList(entity.getId());
        }
        List<Object> values = new ArrayList<>();
        List<Field> fields = ReflectionCachedUtils.getAnnotatedFields(entity.getClass(), AuditedField.class);
        for (Field field : fields) {
            if (!GENERATED_FIELDS.contains(field.getAnnotation(AuditedField.class).value())) {
                Object fieldValue = ReflectionUtils.getFieldValueForComparison(field, entity);
                if (fieldValue instanceof RecordReferential) {
                    fieldValue = ((RecordReferential)fieldValue).getId(); // fix for newly created Labels in XML Parser
                }
                values.add(fieldValue);
            }
        }

        if (logger.isTraceEnabled()) {
            List<String> decentValues = values.stream()
                    .map(ObjectUtils::toString)
                    .map(this::trimValue)
                    .collect(toList());
            logger.trace("Field values for {}: {}", entity, decentValues);
        }
        return values;
    }

    private String trimValue(String s) {
        if (s == null) return null;
        if (s.length() < 100) {
            return s;
        } else {
            return s.substring(0, 100) + " >>...<<";
        }
    }

    /unchecked/
    public <T extends AbstractSimpleEntity> boolean areEqual(Record newRecord, Record oldRecord) {
        EntityUtils.truncateStringFields(newRecord);

        List<Object> newRecordFields = getEqualityFieldValues(newRecord);
        List<Object> oldRecordFields = getEqualityFieldValues(oldRecord);
        if (!newRecordFields.equals(oldRecordFields)) {
            logger.trace("Direct record fields are different: \n{}\n{}",newRecordFields,oldRecordFields);
            return false;
        }
        for (Field field : ReflectionCachedUtils.getAnnotatedFields(newRecord.getClass(), AuditedCollection.class)) {
            Method getter = ReflectionCachedUtils.getGetter(field);
            Set<T> oldChildren = (Set<T>) ReflectionUtils.invoke(oldRecord, getter);
            Set<T> newChildren = (Set<T>) ReflectionUtils.invoke(newRecord, getter);
            if (!chidrenAreEqual(newChildren, oldChildren)) {
                return false;
            }
        }
        return true;
    }

    private <T extends AbstractSimpleEntity> boolean chidrenAreEqual(Set<T> newChildren, Set<T> oldChildren) {
        EntityCollectionDiff<T> diff = new EntityCollectionDiff<>(oldChildren, newChildren, this::getEqualityFieldValues);
        if (diff.getCreatedElements().isEmpty() && diff.getDeletedElements().isEmpty()) {
            return true;
        } else {
            logger.trace("Found differences between children: old={} and new={} ::: {}", oldChildren, newChildren, diff);
            return false;
        }
    }

}

