package com.bnpp.regliss.importer;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.ImportableChild;
import com.bnpp.regliss.annotation.ImportableReferentialCollection;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.record.RecordReferential;
import com.bnpp.regliss.utils.ReflectionCachedUtils;
import com.bnpp.regliss.utils.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

@Service
public class ReattachRecordReferentials {

    @Autowired
    private EntityManager em;

    public void reattachRecordReferentials(Record record) {
        reattachReferentialFields(record);
        reattachReferentialCollectionFields(record);
        reattachReferentialsOnChildren(record);
    }

    private void reattachReferentialsOnChildren(Object child) {
        for (Field collectionField : ReflectionCachedUtils.getAnnotatedFields(child.getClass(), ImportableChild.class)) {
            Set<Object> collectionFieldValues = ReflectionUtils.getFieldValue(collectionField, child);
            for (Object collectionFieldValue : collectionFieldValues) {
                reattachReferentialFields(collectionFieldValue);
                reattachReferentialCollectionFields(collectionFieldValue);
            }
        }
    }

    private void reattachReferentialFields(Object object) {
        for (Field field : ReflectionCachedUtils.getAnnotatedFields(object.getClass(), AuditedField.class)) {
            if(RecordReferential.class.isAssignableFrom(field.getType())){
                Object fieldValue = ReflectionUtils.getFieldValue(field, object);
                if (fieldValue != null) {
                    ReflectionUtils.setFieldValue(field, object,
                            em.getReference(field.getType(), ((RecordReferential) fieldValue).getId()));
                }
            }
        }
    }

    private void reattachReferentialCollectionFields(Object object) {
        for (Field collectionField : ReflectionCachedUtils.getAnnotatedFields(object.getClass(), ImportableReferentialCollection.class)) {
            Class<?> referentialClass = ReflectionCachedUtils.getElementType(collectionField);
            Set<Object> referentialCollection = ReflectionUtils.getFieldValue(collectionField, object);
            Set<Object> referentialReferences = new HashSet<>();
            for (Object referential : referentialCollection) {
                if (referential != null) {
                    referentialReferences.add(em.getReference(referentialClass, ((RecordReferential) referential).getId()));
                } else {
                    referentialReferences.add(referential);
                }
            }
            ReflectionUtils.setFieldValue(collectionField, object, referentialReferences);
        }
    }
}
