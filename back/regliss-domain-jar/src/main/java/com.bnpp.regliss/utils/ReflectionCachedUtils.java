package com.bnpp.regliss.utils;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.record.Record;

import javax.persistence.ManyToMany;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;

@SuppressWarnings("unchecked")
public class ReflectionCachedUtils {

    private static Map<List<?>, Object> cache = new ConcurrentHashMap<>();

    // with @Cacheable
//  +FILL_ATTRIBUTES: 1,225 ms (8,096 calls)
//  +FILL_CHILDREN: 4,280 ms (8,096 calls)
//  +FILL_LABEL_COLLECTION: 667 ms (8,096 calls)

// cu static map
//  +FILL_ATTRIBUTES: 1,249 ms (8,096 calls)
//  +FILL_CHILDREN: 3,265 ms (8,096 calls)
//  +FILL_LABEL_COLLECTION: 875 ms (8,096 calls)

    public static <T extends Annotation> List<Field> getAnnotatedFields(Class<?> clazz, Class<T> annotation) {
        return (List<Field>) cache.computeIfAbsent(asList("getAnnotatedFields", clazz, annotation),
                k -> ReflectionUtils.getAnnotatedFields(clazz, annotation));
    }

    public static Method getSetter(Field field) {
        return (Method) cache.computeIfAbsent(asList("getSetter", field),
                k -> ReflectionUtils.getSetter(field));
    }
    public static Method getGetter(Field field) {
        return (Method) cache.computeIfAbsent(asList("getGetter", field),
                k -> ReflectionUtils.getGetter(field));
    }
    public static Class<?> getElementType(Field collectionField) {
        return (Class<?>) cache.computeIfAbsent(asList("getElementType", collectionField),
                k -> EntityUtils.getElementType(collectionField));
    }
    public static Method getChildAddMethod(Class<?> recordClass, Class<?> childClass) {
          return (Method) cache.computeIfAbsent(asList("getChildAddMethod", recordClass, childClass),
                  k -> EntityUtils.getChildAddMethod(recordClass, childClass));
      }
    public static Method getChildRemoveMethod(Class<?> recordClass, Class<?> childClass) {
          return (Method) cache.computeIfAbsent(asList("getChildRemoveMethod", recordClass, childClass),
                  k -> EntityUtils.getChildRemoveMethod(recordClass, childClass));
      }

    public static Field getAuditedFieldByCode(Class<?> clazz, String fieldCodeStr) {
        return (Field) cache.computeIfAbsent(asList("getAuditedFieldByCode", clazz, fieldCodeStr),
                k -> getAuditedFieldByCodeInternal(clazz, fieldCodeStr));
    }

    private static Field getAuditedFieldByCodeInternal(Class<?> clazz, String fieldCodeStr) {
        AuditedFieldCode fieldCode = null;
        for (AuditedFieldCode afc : AuditedFieldCode.values()) {
            if (afc.name().equals(fieldCodeStr)) {
                fieldCode = afc;
                break;
            }
        }
        if (fieldCode == null) {
            return null; // NON-mapped elements in intermediary XML: for example ACTION
        }
        for (Field field : ReflectionUtils.getAnnotatedFields(clazz, AuditedField.class)) {
            if (field.getAnnotation(AuditedField.class).value() == fieldCode) {
                return field;
            }
        }
        return null; // for example: if the codeStr is for a child
    }


    public static Field getImportableChildByType(Class<?> clazz, String childTypeStr) {
        return (Field) cache.computeIfAbsent(asList("getImportableChildByType", clazz, childTypeStr),
                k -> getImportableChildByTypeInternal(clazz, childTypeStr));
    }

    private static Field getImportableChildByTypeInternal(Class<?> clazz, String childTypeStr) {
        ChildType childType = null;
        for (ChildType type : ChildType.values()) {
            if (type.name().equals(childTypeStr)) {
                childType = type;
                break;
            }
        }
        if (childType == null) {
            return null; // not an importable child (eg. perhaps a direct attribute)
        }
        for (Field field : ReflectionUtils.getAnnotatedFields(clazz, ImportableChild.class)) {
            if (field.getAnnotation(ImportableChild.class).type() == childType) {
                return field;
            }
        }
        return null; // for example: if the codeStr is for a child
    }

    public static Field getImportableReferentialCollections(Class<? extends Record> recordClass, String fieldCodeStr) {
        return (Field) cache.computeIfAbsent(asList("getImportableReferentialCollections", recordClass, fieldCodeStr),
                k -> getImportableReferentialCollectionsInternal(recordClass, fieldCodeStr));
    }

    private static Object getImportableReferentialCollectionsInternal(Class<? extends Record> recordClass, String fieldCodeStr) {
        AuditedFieldCode fieldCode = null;
        for (AuditedFieldCode afc : AuditedFieldCode.values()) {
            if (afc.name().equals(fieldCodeStr)) {
                fieldCode = afc;
                break;
            }
        }
        if (fieldCode == null) {
            return null; // not an importable child (eg. perhaps a direct attribute)
        }
        for (Field field : ReflectionUtils.getAnnotatedFields(recordClass, ImportableReferentialCollection.class)) {
            if (field.getAnnotation(ImportableReferentialCollection.class).code() == fieldCode) {
                return field;
            }
        }
        return null; // for example: if the codeStr is for a child
    }


    public static Class<? extends AbstractSimpleEntity> getFieldOrCollectionElementType(Field field) {
        return (Class<? extends AbstractSimpleEntity>) cache.computeIfAbsent(asList("getFieldOrCollectionElementType", field),
                k -> getFieldOrCollectionElementTypeReal(field));
    }

    private static Class<? extends AbstractSimpleEntity> getFieldOrCollectionElementTypeReal(Field field) {
        if (field.getAnnotation(ManyToMany.class) != null) {
            ParameterizedType type = (ParameterizedType) field.getGenericType();
            return (Class<? extends AbstractSimpleEntity>) type.getActualTypeArguments()[0];
        } else {
            return (Class<? extends AbstractSimpleEntity>) field.getType();
        }
    }

    private ReflectionCachedUtils(){}

}
