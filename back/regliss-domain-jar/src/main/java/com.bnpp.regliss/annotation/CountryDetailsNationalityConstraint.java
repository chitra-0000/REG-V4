package com.bnpp.regliss.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.bnpp.regliss.validation.CountryDetailsNationalityValidator;

@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = CountryDetailsNationalityValidator.class)
public @interface CountryDetailsNationalityConstraint {
    String message() default "";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
