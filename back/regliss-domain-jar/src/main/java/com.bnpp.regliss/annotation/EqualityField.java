package com.bnpp.regliss.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bnpp.regliss.entity.Entity;

/**
 * Field-level annotation used for calculating equality between entity objects derived from
 * {@link Entity}. Applying this annotation to a <i>non-transient</i> and <i>non-static</i>
 * field hints the framework that its value should be included during equals() and hashCode()
 * processing.
 *
 * Using this annotation saves you from reimplementing the equals() and hashCode() methods in
 * derived types of {@link Entity} for the common cases of object equality. This comprises
 * the comparison of fields <i>declared by your entity class</i> itself. More complex cases of
 * equality checking such as comparing fields in composite entities are not covered by this
 * mechanism and still require a custom override of equals() and hashCode() from your types.
 */
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EqualityField {
}
