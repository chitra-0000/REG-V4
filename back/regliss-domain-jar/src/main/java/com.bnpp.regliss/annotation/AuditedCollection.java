package com.bnpp.regliss.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface AuditedCollection {

    AuditedAction createAction();
    AuditedAction updateAction() default AuditedAction.NOT_APPLICABLE;
    AuditedAction deleteAction();

}
