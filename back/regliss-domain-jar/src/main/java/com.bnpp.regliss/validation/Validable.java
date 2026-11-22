package com.bnpp.regliss.validation;

import javax.validation.ConstraintViolation;
import java.util.Set;

public interface Validable {


    String getBusinessId();

    default Set<ConstraintViolation<Validable>> getValidationErrors() {
        Set<ConstraintViolation<Validable>> violations = ConstraintsValidator.validate(this, false, ErrorConstraint.class);
        ConstraintsValidator.logViolations(violations, this);
        return violations;
    }

    default Set<ConstraintViolation<Validable>> getSemiAutoValidationErrors() {

        Set<ConstraintViolation<Validable>> violations =ConstraintsValidator.validate(this, true, SemiAutoErrorConstraint.class);
        ConstraintsValidator.logViolations(violations, this);
        return violations;
    }

    default Set<ConstraintViolation<Validable>> getValidationWarnings() {
        Set<ConstraintViolation<Validable>> violations = ConstraintsValidator.validate(this, false, WarningConstraint.class);
        ConstraintsValidator.logViolations(violations, this);
        return violations;
    }

    default <T> void validate(T o, Class<?>... validatorGroups) {
        Set<ConstraintViolation<T>> violations = ConstraintsValidator.validate(o, false, validatorGroups);
        ConstraintsValidator.logViolations(violations, this);
    }
}
