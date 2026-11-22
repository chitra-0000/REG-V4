package com.bnpp.regliss.validation;

import com.bnpp.regliss.annotation.CustomLength;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CustomLengthValidator implements ConstraintValidator<CustomLength, String> {

    private int[] customLength;

    @Override
    public void initialize(CustomLength constraint) {
        this.customLength = constraint.filterByCustomLength();
    }

    @Override
    public boolean isValid(String codeBic, ConstraintValidatorContext constraintValidatorContext) {
        for (int length : customLength) {
            if (length == codeBic.length()) {
                return true;
            }
        }
        return false;
    }

}
