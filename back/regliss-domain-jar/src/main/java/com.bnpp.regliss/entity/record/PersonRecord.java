package com.bnpp.regliss.entity.record;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import com.bnpp.regliss.validation.WarningConstraint;
import org.apache.commons.lang.StringUtils;

import com.bnpp.regliss.entity.referential.record.Gender;
import com.bnpp.regliss.entity.referential.record.RecordType;

import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue(value = RecordType.DiscriminatorValues.PERSON)
public class PersonRecord extends Record implements AddressContainer {

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.PERSON_TITLE)
    @Column(name = "TITLE", length = 512 )
    private String title;

    @CaseInsensitive
    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.PERSON_FIRST_NAME)
    @Column(name = "FIRST_NAME", length = 300 )
    private String firstName;

    @CaseInsensitive
    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.PERSON_MIDDLE_NAME)
    @Column(name = "MIDDLE_NAME", length = 300 )
    private String middleName;

    @CaseInsensitive
    @NotEmpty(message="{record.lastName.mandatory}", groups = WarningConstraint.class)
    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.PERSON_LAST_NAME)
    @Column(name = "LAST_NAME", length = 300 )
    private String lastName;

    @CaseInsensitive
    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.PERSON_MAIDEN_NAME)
    @Column(name = "MAIDEN_NAME", length = 300 )
    private String maidenName;

    @Getter
    @Column(name = "MAIDEN_NAME_NORMALIZED", length = 450 )
    private String maidenNameNormalized;

    @CaseInsensitive
    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.PERSON_SUFFIX)
    @Column(name = "SUFFIX", length =  192 )
    private String suffix;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.PERSON_IS_DECEASED)
    @Column(name = "IS_DECEASED" )//, nullable = false)
    private Boolean deceased;

    @ImportableReferentialCollection(code = AuditedFieldCode.PERSON_GENDER)
    @AuditedCollection(createAction = AuditedAction.RECORD_GENDER_ADDITION,
            updateAction = AuditedAction.RECORD_GENDER_MODIFICATION,
            deleteAction = AuditedAction.RECORD_GENDER_DELETION)
    @ManyToMany(cascade = CascadeType.MERGE)
    @JoinTable(name = "RECORD_GENDER", joinColumns = @JoinColumn(name = "RECORD_ID"), inverseJoinColumns = @JoinColumn(name = "GENDER_ID"))
    private Set<Gender> genders = new HashSet<>();

    @Valid
    @ImportableChild(type = ChildType.PERSON_BIOGRAPHY)
    @AuditedCollection(createAction = AuditedAction.RECORD_PERSON_BIOGRAPHY_ADDITION,
            updateAction = AuditedAction.RECORD_PERSON_BIOGRAPHY_MODIFICATION,
            deleteAction = AuditedAction.RECORD_PERSON_BIOGRAPHY_DELETION)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RECORD_PERSON_BIOGRAPHY", joinColumns = @JoinColumn(name = "RECORD_ID"), inverseJoinColumns = @JoinColumn(name = "PERSON_BIOGRAPHY_ID"))
    private Set<PersonBiography> biographies = new HashSet<>();

    @ImportableChild(type = ChildType.FUNCTION)
    @AuditedCollection(createAction = AuditedAction.RECORD_PERSON_FUNCTION_ADDITION,
            updateAction = AuditedAction.RECORD_PERSON_FUNCTION_MODIFICATION,
            deleteAction = AuditedAction.RECORD_PERSON_FUNCTION_DELETION)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "RECORD_FUNCTION", joinColumns = @JoinColumn(name = "RECORD_ID"), inverseJoinColumns = @JoinColumn(name = "RECORD_FUNCTION_ID"))
    private Set<PersonFunction> personFunctions = new HashSet<>();

    @ImportableChild(type = ChildType.IMAGE)
    @ManyToMany(cascade = CascadeType.ALL)
    @AuditedCollection(createAction = AuditedAction.RECORD_PERSON_IMAGE_ADDITION,
            updateAction = AuditedAction.RECORD_PERSON_IMAGE_MODIFICATION,
            deleteAction = AuditedAction.RECORD_PERSON_IMAGE_DELETION)
    @JoinTable(name = "RECORD_IMAGE", joinColumns = @JoinColumn(name = "RECORD_ID"), inverseJoinColumns = @JoinColumn(name = "IMAGE_ID"))
    protected Set<PersonImage> personImages = new HashSet<>();

    public PersonRecord() { super(RecordType.PERSON); }

    // TODO: [GAP] lines 124-125 not captured in screenshots — do NOT fabricate missing logic.
    // (by analogy with sibling classes PersonFunction/PersonBiography, a constructor such as
    // "public PersonRecord(long id) { setId(id); }" likely sits here but was not visible in any photo.)

    public PersonRecord(PersonRecord other) {
        super(other);
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.middleName = other.middleName;
        this.lastName = other.lastName;
        this.maidenName = other.maidenName;
        this.maidenNameNormalized = other.maidenNameNormalized;
        this.suffix = other.suffix;
        this.deceased = other.deceased;
        this.title = other.title;

        for (Gender otherGender: other.genders) {
            addGender(new Gender(otherGender));
        }

        for (Address otherAddress: other.addresses) {
            addAddress(new Address(otherAddress));
        }

        for (IdentityDocument otherIdentityDocument: other.identityDocuments) {
            addIdentityDocument(new IdentityDocument(otherIdentityDocument));
        }

        for (PersonBiography otherBiography: other.biographies) {
            addBiography(new PersonBiography(otherBiography));
        }

        for (PersonFunction otherFunction: other.personFunctions) {
            addPersonFunction(new PersonFunction(otherFunction));
        }

        for (CodeIban otherCode: other.ibanCodes) {
            addIbanCode(new CodeIban(otherCode));
        }

        for (PersonImage otherPersonImage: other.personImages) {
            addPersonImage(new PersonImage(otherPersonImage));
        }
    }

    public void setGenders(Set<Gender> newGenders) {
        HashSet<Gender> oldGenders = new HashSet<>(genders);
        oldGenders.forEach(this::removeGender);
        newGenders.forEach(this::addGender);
    }

    public Set<Gender> getGenders() { return Collections.unmodifiableSet(this.genders); }

    public void addGender(final Gender gender) {
        genders.add(gender);
        gender.addPersonRecord(this);
    }

    public void removeGender(final Gender gender) {
        genders.remove(gender);
        gender.removePersonRecord(this);
    }

    public void setPersonFunctions(Set<PersonFunction> newPersonFunctions) {
        HashSet<PersonFunction> oldPersonFunctions = new HashSet<>(personFunctions);
        oldPersonFunctions.forEach(this::removePersonFunction);
        newPersonFunctions.forEach(this::addPersonFunction);
    }

    public void setBiographies(Set<PersonBiography> newBiographies) {
        HashSet<PersonBiography> oldBiography = new HashSet<>(biographies);
        oldBiography.forEach(this::removeBiography);
        biographies.clear();
        newBiographies.forEach(this::addBiography);
    }

    public void setPersonImages(Set<PersonImage> newPersonImages) {
        Set<PersonImage> oldPersonImages = new HashSet<>(personImages);
        oldPersonImages.forEach(this::removePersonImage);
        newPersonImages.forEach(this::addPersonImage);
    }

    public PersonRecord setMaidenName(String maidenName) {
        this.maidenName = maidenName;
        this.maidenNameNormalized = TextNormalizerQueryUtil.normalize(maidenName);
        return this;
    }

    @Override
    public String getFullName() {
        return Stream.of(title, firstName, middleName, lastName, suffix)
                .filter(StringUtils::isNotEmpty)
                .collect(joining(" "));
    }

    public Set<PersonBiography> getBiographies() { return Collections.unmodifiableSet(this.biographies); }

    public void addBiography(final PersonBiography biography) {
        biographies.add(biography);
        biography.addPersonRecord(this);
    }

    public void removeBiography(final PersonBiography biography) {
        biographies.remove(biography);
        biography.removePersonRecord(this);
    }

    public void addPersonFunction(final PersonFunction personFunction) {
        personFunctions.add(personFunction);
        personFunction.addPersonRecord(this);
    }

    public Set<PersonFunction> getPersonFunctions() { return Collections.unmodifiableSet(this.personFunctions); }

    public void removePersonFunction(PersonFunction personFunction) {
        this.personFunctions.remove(personFunction);
        personFunction.removePersonRecord(this);
    }

    public void addPersonImage(final PersonImage image) {
        this.personImages.add(image);
        image.addPersonRecord(this);
    }

    public void removePersonImage(final PersonImage personImage) {
        this.personImages.remove(personImage);
        personImage.removePersonRecord(this);
    }

    public Set<PersonImage> getPersonImages() { return Collections.unmodifiableSet(this.personImages); }

    @Size(min = 2, message="{personRecord.firstName.middleName.lastName.min}", groups = WarningConstraint.class)
    @Size(max = 300, message="{personRecord.firstName.middleName.lastName.max}", groups = WarningConstraint.class)
    private String getFirstNameLastNameMiddlename(){
        String firstNameValue = StringUtils.defaultString(firstName, EMPTY);
        String middleNameValue = StringUtils.defaultString(lastName, EMPTY);
        String lastNameValue = StringUtils.defaultString(middleName, EMPTY);

        return firstNameValue.trim() + middleNameValue.trim() + lastNameValue.trim();
    }
}
