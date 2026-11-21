package com.bnpp.regliss.annotation;

import com.bnpp.regliss.validation.CustomLengthValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CustomLength.List.class)
@Documented
@Constraint(
        validatedBy = {CustomLengthValidator.class}
)
public @interface CustomLength {

    String message() default "{javax.validation.constraints.CustomLength.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int[] filterByCustomLength() default {};

    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface List {
        CustomLength[] value();
    }
}
