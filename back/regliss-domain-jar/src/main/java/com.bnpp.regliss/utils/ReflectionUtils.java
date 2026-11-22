package com.bnpp.regliss.utils;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.annotation.CaseInsensitive;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unchecked")
public final class ReflectionUtils {
    private static final String CHECK_SECURITY_POLICY = " check security policy";
    public static final String THE_FIELD_ARGUMENT_SHOULD_NOT_BE_NULL = "The field argument should not be null";
    public static final String UNABLE_TO_ACCESS_FIELD = "Unable to access field: ";
    public static final String COULDN_T_SWITCH_ACCESSIBLE_PROPERTY_FOR_FIELD = "Couldn't switch acessible property for field: ";

    /**
     * Retrieve all fields annotated with a given annotation class within a given class throughout
     * its class hierarchy.
     *
     * @param <T> The annotation type
     * @param clazz to search for annotated field
     * @param annotation the annotation object
     * @return all fields who are annotated
     */
    public static <T extends Annotation> List<Field> getAnnotatedFields(Class<?> clazz, Class<T> annotation) {
        List<Field> result = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getAnnotation(annotation) != null) {
                result.add(field);
            }
        }
        if (clazz.getSuperclass() != Object.class) {
            result.addAll(getAnnotatedFields(clazz.getSuperclass(), annotation));
        }
        return result;
    }




    /**
     * Retrieve field by its name within a given class throughout the class hierarchy.
     *
     * @param clazz the class to search for the field
     * @param fieldName the name of the field to search for
     * @return Field a field with the corresponding fieldName or null if no field was found
     */
    public static Field getField(final Class<?> clazz, final String fieldName) {
        for (Field field : clazz.getDeclaredFields()) {
            if (fieldName.equals(field.getName())) {
                return field;
            }
        }

        if (clazz.getSuperclass() != Object.class) {
            return getField(clazz.getSuperclass(), fieldName);
            // 18:10
        }
        return null;
    }

    /**
     * Retrieve all fields of a given type within the given class throughout the class hierarchy.
     *
     * @param <T> The annotation type
     * @param clazz to search in
     * @param type the type of the field to search for
     * @return all fields of the given type
     */
    public static <T extends Object> List<Field> getFields(final Class<T> clazz, final Class<?> type) {
        final List<Field> result = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType().isAssignableFrom(type)) {
                result.add(field);
            }
        }
        if (clazz.getSuperclass() != Object.class) {
            result.addAll(getFields(clazz.getSuperclass(), type));
        }
        return result;

    }

    /**
     * Obtain the value of a given field for a given instance in which the field is defined. Remark:
     * in order to obtain this value, the field's accessibility may need to be compromised due to
     * visibility constraints. This of course will only work in case that the actual security
     * manager does not put any restriction on altering the field's accessibility.
     *
     * @param field the field to get the value for
     * @param source the object containing the value
     * @return the field value
     */
    public static <T> T getFieldValue(final Field field, final Object source) {
        try {
            if (field == null) {
                throw new IllegalArgumentException(THE_FIELD_ARGUMENT_SHOULD_NOT_BE_NULL);
            }
            if (field.isAccessible()) {
                return (T) field.get(source);
            } else {
                field.setAccessible(true);
                final Object v = field.get(source);
                return (T)v;
            }
        } catch (IllegalAccessException e) {
            throw new ReglissException(e, UNABLE_TO_ACCESS_FIELD + field.getName() + CHECK_SECURITY_POLICY);
        } catch (SecurityException e) {
            throw new ReglissException(e, COULDN_T_SWITCH_ACCESSIBLE_PROPERTY_FOR_FIELD + field.getName() // NOSONAR Exception thrown above if null
                    + CHECK_SECURITY_POLICY);
        }
    }

    public static <T> T getFieldValueForComparison(final Field field, final Object source) {
        try {
            if (field == null) {
                throw new IllegalArgumentException(THE_FIELD_ARGUMENT_SHOULD_NOT_BE_NULL);
            }
            boolean caseInsensitiveFields = source.getClass().getAnnotation(CaseInsensitive.class) != null || EntityUtils.isCaseInsensitiveField(field);
            if (field.isAccessible()) {
                return getFieldValue(field, source, caseInsensitiveFields);
            } else {
                field.setAccessible(true);
                final Object v = getFieldValue(field, source, caseInsensitiveFields);
                return (T)v;
            }
        } catch (IllegalAccessException e) {
            throw new ReglissException(e, UNABLE_TO_ACCESS_FIELD + field.getName() + CHECK_SECURITY_POLICY);
        } catch (SecurityException e) {
            throw new ReglissException(e, COULDN_T_SWITCH_ACCESSIBLE_PROPERTY_FOR_FIELD + field.getName() // NOSONAR Exception thrown above if null
                    + CHECK_SECURITY_POLICY);
        }
    }

    public static <T> T getFieldValue(Field field, Object source, boolean caseInsensitiveFields) throws IllegalAccessException {
        T fieldValue = (T) field.get(source);
        return (field.getType().equals(String.class) && caseInsensitiveFields && fieldValue != null)
                ? (T) String.class.cast(fieldValue).toUpperCase()
                : fieldValue;
    }

    public static void setFieldValue(final Field field, final Object source, final Object newValue) {
        try {
            if (field == null) {
                throw new IllegalArgumentException(THE_FIELD_ARGUMENT_SHOULD_NOT_BE_NULL);
            }
            if (field.isAccessible()) {
                field.set(source, newValue);
            } else {
                field.setAccessible(true);
                field.set(source, newValue);
            }
        } catch (IllegalAccessException e) {
            throw new ReglissException(e, UNABLE_TO_ACCESS_FIELD + field.getName() + CHECK_SECURITY_POLICY);
        } catch (SecurityException e) {
            throw new ReglissException(e, COULDN_T_SWITCH_ACCESSIBLE_PROPERTY_FOR_FIELD + field.getName() // NOSONAR Exception thrown above if null
                    + CHECK_SECURITY_POLICY);
        }
    }

    /**
     * Test whether a field has been declared as transient.
     *
     * @param field the field to inspect
     * @return true if the field is transient
     */
    public static boolean isTransientField(final Field field) {
        return Modifier.isTransient(field.getModifiers());
    }

    /**
     * Test whether a field has been declared as static.
     *
     * @param field the field to inspect
     * @return true if the field is static
     */
    public static boolean isStaticField(final Field field) {
        return Modifier.isStatic(field.getModifiers());
    }

    /**
     * Find class through overall java type information (Type can be a raw type, parameterized type,
     * array type, type variable or a primitive type.
     *
     * @param type
     * @return Class definition found
     */
    @SuppressWarnings("rawtypes")
    public static Class<?> getClass(final Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            final Type componentType = ((GenericArrayType) type).getGenericComponentType();
            final Class<?> componentClass = getClass(componentType);
            if (componentClass != null) {
                return Array.newInstance(componentClass, 0).getClass();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Retrieve all type arguments for a generic class.
     *
     * @param <T>
     * @param clazz The class for which to retrieve the generic type information
     * @return List of all generic type arguments found
     */
      public static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (IllegalAccessException|InstantiationException e) {
            throw new ReglissException(e);
        }
    }

    /**
     * Variant of Class.getMethod that doesn't throw any Checked exceptions
     *
     * @param clazz
     * @param methodName
     * @param parameterTypes
     * @return the Method
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (SecurityException | NoSuchMethodException e) {
            throw new ReglissException(e);
        }
    }

    /**
     * Variant of Method.invoke, that doesn't throw any checked exceptions.
     *
     * @param obj
     * @param method
     * @param arguments
     * @return
     */
    public static Object invoke(Object obj, Method method, Object... arguments) {
        if(method == null) {
            throw new  IllegalArgumentException("The method argument should not be null");
        }
        try {
            return method.invoke(obj, arguments);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw new ReglissException(e, "Could not invoke " + method.getName() + " with args " + Arrays.toString(arguments));
        }
    }

    public static String getCallerMethodName() {
        return new RuntimeException().getStackTrace()[2].getMethodName();
    }

    public static void trimAllStringFields(Object o) {
        for (Field field : o.getClass().getDeclaredFields()) {
            Object fieldValue = ReflectionUtils.getFieldValue(field, o);
            if (!field.getType().equals(String.class) || fieldValue == null) {
                continue;
            }
            ReflectionUtils.setFieldValue(field, o, String.class.cast(fieldValue).trim());
        }
    }

    public static Method getGetter(Field field) {
        String getterName = "get" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        return getMethod(field.getDeclaringClass(), getterName);
    }

    public static Method getSetter(Field field) {
        String setterName = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        return getMethod(field.getDeclaringClass(), setterName, field.getType());
    }




}
