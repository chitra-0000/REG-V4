package com.bnpp.regliss.validation;

import com.bnpp.regliss.annotation.AddressCityCountryConstraint;
import com.bnpp.regliss.entity.record.Address;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class AddressCityCountryValidator implements ConstraintValidator<AddressCityCountryConstraint, Address> {
    @Override
    public boolean isValid(Address address, ConstraintValidatorContext constraintValidatorContext) {
        return isNotBlank(address.getCity()) && address.getCountryOpt().isPresent();
    }
}
