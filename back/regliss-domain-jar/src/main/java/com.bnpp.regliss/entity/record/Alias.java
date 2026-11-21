package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.AliasQuality;
import com.bnpp.regliss.entity.referential.record.AliasType;
import com.bnpp.regliss.utils.TextNormalizerQueryUtil;
import com.bnpp.regliss.validation.WarningConstraint;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

@Entity
@Table(name = "ALIAS")
@CaseInsensitive
/**@SequenceGenerator(name = "AliasIdGenerator", initialValue = 100, sequenceName = "ALIAS_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "AliasIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "ALIAS_SEQ") } )
public class Alias extends AbstractSimpleEntity implements RecordAwareChild, ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "AliasIdGenerator")
    protected Long id;

    @AuditedField(AuditedFieldCode.RECORD_ALIAS_REGLISS_REFFERENCE)
    @Embedded
    private AliasReglissReference reglissReference;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_ALIAS_TITLE)
    @EqualityField
    @Column(name = "TITLE", length = 512)
    private String title;

    @NotEmpty(message="{alias.externalReference.mandatory}", groups = WarningConstraint.class)
    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_ALIAS_EXTERNAL_REFERENCE)
    @Column(name = "EXTERNAL_REFFERENCE")
    private String externalReference;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_ALIAS_FIRST_NAME)
    @EqualityField
    @Column(name = "FIRST_NAME", length = 300)
    private String firstName;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_ALIAS_MIDDLE_NAME)
    @EqualityField
    @Column(name = "MIDDLE_NAME", length = 300)
    private String middleName;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.RECORD_ALIAS_LAST_NAME)
    @EqualityField
    @Column(name = "LAST_NAME", length = 300)
    private String lastName;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_ALIAS_SUFFIX)
    @EqualityField
    @Column(name = "SUFFIX", length = 300)
    private String suffix;

    @NotEmpty(message="{alias.fullName.mandatory}", groups = WarningConstraint.class)
    @Getter
    @AuditedField(AuditedFieldCode.RECORD_ALIAS_FULL_NAME)
    @Column(name = "FULL_NAME", length = 1200)
    @EqualityField
    private String fullName;

    @Column(name = "FULL_NAME_NORMALIZED", length = 1500)
    private String fullNameNormalized;

    @Setter
    @AuditedField(AuditedFieldCode.RECORD_ALIAS_TYPE)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "ALIAS_TYPE_ID")
    private AliasType type;

    @Setter
    @AuditedField(AuditedFieldCode.RECORD_ALIAS_QUALITY)
    @EqualityField
    @ManyToOne
    @JoinColumn(name = "ALIAS_QUALITY_ID")
    private AliasQuality quality;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.RECORD_ALIAS_LANGUAGE)
    @EqualityField
    @Column(name="LANGUAGE", length = 24)
    private String language;

    @ManyToMany(mappedBy = "aliases")
    private List<Record> records = new ArrayList<>();

    public Alias() {
    }

    public Alias(long id) { setId(id); }

    public Alias(Alias other) {
        this.id = other.id;
        this.firstName = other.firstName;
        this.title = other.title;
        this.middleName = other.middleName;
        this.lastName = other.lastName;
        this.suffix = other.suffix;
        this.fullName = other.fullName;
        this.fullNameNormalized = other.fullNameNormalized;
        this.externalReference = other.externalReference;
        this.type = other.type;
        this.quality = other.quality;
        this.language = other.language;
        copyUnmodifiableFieldsFrom(other);
    }

    public void copyUnmodifiableFieldsFrom(Alias a) {
        this.reglissReference = new AliasReglissReference(a.reglissReference);
    }

    public Optional<AliasType> getTypeOpt() { return Optional.ofNullable(type); }

    public Optional<AliasQuality> getQualityOpt() { return Optional.ofNullable(quality); }

    public boolean hasQuality(ReferentialBusinessConcept businessConcept) {
        return quality != null && quality.hasBusinessConcept(businessConcept);
    }

    public boolean hasType(ReferentialBusinessConcept businessConcept) {
        return type != null && type.hasBusinessConcept(businessConcept);
    }

    void addRecord(Record record) { records.add(record); }

    @Override
    public List<Record> getRecords() { return unmodifiableList(records); }

    public Record getSomeParentRecord() { return records.iterator().next(); }

    public String getComputedFullName() {
        return Stream.of(title, firstName, middleName, lastName)
                .filter(StringUtils::isNotEmpty)
                .collect(joining(" "));
    }

    @Override
    public void removeFromParent(Record parentToUnlink) { parentToUnlink.removeAlias(this); }

    public static Predicate<Alias> byExternalRef(String extRef) {
        return alias -> extRef.equals(alias.getExternalReference());
    }

    public String getReglissReference() { return reglissReference != null ? reglissReference.getFullReference() : ""; }

    public String getReglissReferenceSuffix() { return reglissReference != null ? reglissReference.getSuffix() : ""; }

    public void setReglissReference(String fullReglissReference) {
        this.reglissReference = new AliasReglissReference(fullReglissReference);
    }

    public void setReglissReference(AliasReglissReference reglissReference) { this.reglissReference = reglissReference; }

    public static String generateNextAliasReference(String lastReference) {
        StringBuilder builder = new StringBuilder();
        while (isNotEmpty(lastReference) && lastReference.endsWith("Z")) {
            lastReference = lastReference.substring(0, lastReference.length() - 1);
            builder.append("A");
        }
        if (isEmpty(lastReference)) {
            return "A" + builder.toString();
        } else {
            char characterToBeIncremented = lastReference.charAt(lastReference.length() - 1);
            lastReference = lastReference.substring(0, lastReference.length() - 1);
            return lastReference + (char) (characterToBeIncremented + 1) + builder.toString();
        }
    }

    public Alias setFullName(String fullName) {
        this.fullName = fullName;
        this.fullNameNormalized = TextNormalizerQueryUtil.normalize(fullName);
        return this;
    }
}
