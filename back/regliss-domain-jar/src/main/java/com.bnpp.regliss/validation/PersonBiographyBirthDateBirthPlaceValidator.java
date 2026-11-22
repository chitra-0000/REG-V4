package com.bnpp.regliss.validation;

import com.bnpp.regliss.annotation.PersonBiographyBirthDateBirthPlaceConstraint;
import com.bnpp.regliss.entity.record.PersonBiography;
import com.bnpp.regliss.entity.referential.record.DateType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Optional;

import static com.bnpp.regliss.entity.referential.ReferentialBusinessConcept.DATE_TYPE_DATE_OF_BIRTH;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class PersonBiographyBirthDateBirthPlaceValidator implements ConstraintValidator<PersonBiographyBirthDateBirthPlaceConstraint, PersonBiography> {

    @Override
    public boolean isValid(PersonBiography personBiography, ConstraintValidatorContext constraintValidatorContext) {
        return ((isNotBlank(personBiography.getDateValueStr()) && isDateBirth(personBiography.getDateTypeOpt())) ||
                isNotBlank(personBiography.getBirthPlace()));
    }

    private boolean isDateBirth(Optional<DateType> dateTypeOpt) {
        return dateTypeOpt
                .filter(d -> DATE_TYPE_DATE_OF_BIRTH.equals(d.getBusinessConcept()))
                .isPresent();
    }
}
