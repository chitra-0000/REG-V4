package com.bnpp.regliss.validation;

import com.bnpp.regliss.annotation.CountryDetailsNationalityConstraint;
import com.bnpp.regliss.entity.record.CountryDetails;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CountryDetailsNationalityValidator implements ConstraintValidator<CountryDetailsNationalityConstraint, CountryDetails> {
    @Override
    public boolean isValid(CountryDetails countryDetails, ConstraintValidatorContext constraintValidatorContext) {

        return countryDetails.isCitizenshipType() && countryDetails.getCountryOpt().isPresent();
    }


}