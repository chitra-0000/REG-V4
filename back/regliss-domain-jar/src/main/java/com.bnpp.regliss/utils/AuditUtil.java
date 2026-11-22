package com.bnpp.regliss.utils;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import com.bnpp.regliss.repository.pagination.PaginationOptions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static com.bnpp.regliss.utils.ReflectionUtils.getAnnotatedFields;
import static com.bnpp.regliss.utils.ReflectionUtils.getFieldValue;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.StringUtils.isEmpty;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class AuditUtil {

    private static final String SUBFILTER_DESCRIPTION_ALL_VALUE = "ALL";

    // TODO: [ILLEGIBLE - the line(s) just above the copy() method are unclear in the screenshot, shown as "/unchecked/" then "@" alone on the next line; likely a @SuppressWarnings("unchecked") annotation, not transcribed verbatim]
    public static <T extends Record> T copy(T other) {
        try {
            return (T) other.getClass().getConstructor(other.getClass()).newInstance(other);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static String criteriaToAuditString(Object criteriaObject) {
        return Stream.of(criteriaObject.getClass().getDeclaredFields()).sorted(comparing(Field::getName))
                .filter(field -> !field.getType().equals(PaginationOptions.class))
                .map(field -> fieldToLabelAndValue(criteriaObject, field)).filter(StringUtils::isNotBlank)
                .collect(joining(", "));
    }

    public static String auditedFieldsToString(Object object) {
        List<Field> fields = getAnnotatedFields(object.getClass(), AuditedField.class);
        if (fields.size() == 1) {
            final Field field = fields.get(0);
            return fieldValueToString(getFieldValue(field, object), field);
        } else {
            return fields.stream().sorted(comparing(Field::getName)).map(field -> fieldToLabelAndValue(object, field))
                    .filter(StringUtils::isNotEmpty).collect(joining(", "));
        }
    }

    private static String fieldToLabelAndValue(Object object, Field field) {
        String fieldValueStr = fieldToValue(object, field);
        if (isEmpty(fieldValueStr)) {
            return "";
        } else {
            return field.getName() + "=" + fieldValueStr;
        }
    }

    private static String fieldToValue(Object object, Field field) {
        Object fieldValue = getFieldValue(field, object);
        String fieldValueStr = fieldValueToString(fieldValue, field);
        return fieldValueStr;
    }

    private static String fieldValueToString(Object fieldValue, Field field) {
        if (fieldValue == null) {
            if (field.isAnnotationPresent(AuditedField.class) &&
                    field.getAnnotation(AuditedField.class).value() == AuditedFieldCode.SUBFILTER_DESCRIPTIONS1) {
                return SUBFILTER_DESCRIPTION_ALL_VALUE;
            }
            return "";
        }
        if (fieldValue instanceof ToStringAuditable) {
            return ((ToStringAuditable) fieldValue).toStringAudit();
        } else if (fieldValue instanceof LocalDateTime) {
            return DateTimeFormatter.ofPattern("dd/MM/yyyy").format((LocalDateTime) fieldValue);
        } else if (fieldValue instanceof LocalDate) {
            return DateTimeFormatter.ofPattern("dd/MM/yyyy").format((LocalDate) fieldValue);
        } else if (fieldValue instanceof Collection<?>) {
            Collection<?> collectionField = (Collection<?>) fieldValue;

            if (collectionField.isEmpty()) {
                return "";
            } else if (collectionField.iterator().next() instanceof ToStringAuditable) {
                String values = collectionField.stream().map(element -> ((ToStringAuditable) element).toStringAudit())
                        .collect(joining(","));
                return "[" + values + "]";
            }
            return fieldValue.toString();
        } else {
            return fieldValue.toString();
        }
    }
}
