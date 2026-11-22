package com.bnpp.regliss.validation;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.AggregateResourceBundleLocator;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
class ConstraintsValidator {
    /*To instantiate a validator takes a lot of time so instantiate only once in a constant*/
    public static final String CUSTOM_VALIDATOR_MESSAGES = "CustomValidationMessages";
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final Validator CUSTOM_VALIDATOR = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator((new ResourceBundleMessageInterpolator(new AggregateResourceBundleLocator(Arrays.asList(CUSTOM_VALIDATOR_MESSAGES)))))
            .buildValidatorFactory()
            .getValidator();

    private ConstraintsValidator() {
        throw new UnsupportedOperationException("Only static method class");
    }

    static <T> Set<ConstraintViolation<T>> validate(T toValidate) {
        return VALIDATOR.validate(toValidate);
    }

    static <T> Set<ConstraintViolation<T>> validate(T toValidate, boolean isCustomValidation, Class<?>... validatorGroups) {
        if(isCustomValidation){
            return CUSTOM_VALIDATOR.validate(toValidate, validatorGroups);
        }
        return VALIDATOR.validate(toValidate, validatorGroups);
    }

    static <T> void logViolations(Set<ConstraintViolation<T>> violations, Validable validatedObject) {
        if (violations.isEmpty()) return;
        String formattedViolations = violations.stream().map(ConstraintsValidator::printConstraint).collect(Collectors.joining(" | "));
        log.debug("Reference {}  {}, violations: {}", validatedObject.getBusinessId(), validatedObject, formattedViolations);
    }

    private static <T> String printConstraint(ConstraintViolation<T> v) {
        return v.getPropertyPath().toString() + " " + v.getMessage();
    }
}
