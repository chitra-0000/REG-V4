package com.bnpp.regliss.validation;

import com.bnpp.regliss.annotation.PersonBiographyBirthDeathCountryPlaceConstraint;
import com.bnpp.regliss.entity.record.PersonBiography;
import com.bnpp.regliss.entity.referential.record.DateType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Optional;
import java.util.function.Predicate;

import static com.bnpp.regliss.entity.referential.ReferentialBusinessConcept.DATE_TYPE_DATE_OF_BIRTH;
import static com.bnpp.regliss.entity.referential.ReferentialBusinessConcept.DATE_TYPE_DATE_OF_DEATH;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class PersonBiographyBirthDeathCountryPlaceValidator implements ConstraintValidator<PersonBiographyBirthDeathCountryPlaceConstraint, PersonBiography> {

    @Override
    public boolean isValid(PersonBiography personBiography, ConstraintValidatorContext constraintValidatorContext) {

        return ((isNotBlank(personBiography.getDateValueStr()) && isDateBirthOrDeath(personBiography.getDateTypeOpt())) ||
                isNotBlank(personBiography.getBirthPlace()) ||
                personBiography.getBirthCountryOpt().isPresent());
    }

    private boolean isDateBirthOrDeath(Optional<DateType> dateTypeOpt) {
        Predicate<DateType> isBirthDate = d -> DATE_TYPE_DATE_OF_BIRTH.equals(d.getBusinessConcept());
        Predicate<DateType> isDeathDate = d -> DATE_TYPE_DATE_OF_DEATH.equals(d.getBusinessConcept());
        return dateTypeOpt
                .filter(isBirthDate.or(isDeathDate))
                .isPresent();
    }

}
