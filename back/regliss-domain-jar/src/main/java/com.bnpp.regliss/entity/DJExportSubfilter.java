package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import com.bnpp.regliss.entity.referential.record.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "DJ_SUBFILTER")
/**@SequenceGenerator(name = "DJExportSubfilterIdGenerator", initialValue = 100, sequenceName = "DJ_SUBFILTER_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "DJExportSubfilterIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "DJ_SUBFILTER_SEQ") } )
public class DJExportSubfilter extends AbstractSimpleEntity implements ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "DJExportSubfilterIdGenerator")
    protected Long id;
    @Getter @Setter
    @Column(name = "SUBFILTER_INDEX")
    private Integer index;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_SINCE_DEATH_PEP_SIP)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "operator", column = @Column(name = "OPERATOR_SINCE_DEATH_PEP_SIP")),
            @AttributeOverride(name = "years", column = @Column(name = "YEARS_SINCE_DEATH_PEP_SIP"))
    })
    private DJSubfilterDateCriteria deathPepSipDateCriteria;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_SINCE_DEATH_RCA)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "operator", column = @Column(name = "OPERATOR_SINCE_DEATH_RCA")),
            @AttributeOverride(name = "years", column = @Column(name = "YEARS_SINCE_DEATH_RCA"))
    })
    private DJSubfilterDateCriteria deathRCADateCriteria;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_INACTIVITY_PEP)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "operator", column = @Column(name = "OPERATOR_INACTIVITY_PEP")),
            @AttributeOverride(name = "years", column = @Column(name = "YEARS_INACTIVITY_PEP"))
    })
    private DJSubfilterDateCriteria inactivityPepDateCriteria;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_DESCRIPTIONS1)
    @Embedded
    private DJSubfilterDescription1 description1;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_INACTIVITY_RCA)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "operator", column = @Column(name = "OPERATOR_INACTIVITY_RCA")),
            @AttributeOverride(name = "years", column = @Column(name = "YEARS_INACTIVITY_RCA"))
    })
    private DJSubfilterDateCriteria inactivityRcaDateCriteria;

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_DESCRIPTIONS2)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "DJ_SUBFILTER_SANCTION_TYPE", joinColumns = @JoinColumn(name = "DJ_SUBFILTER_ID"), inverseJoinColumns = @JoinColumn(name = "SANCTION_TYPE_ID"))
    private Set<SanctionType> sanctionTypes = new HashSet<>();

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_DESCRIPTIONS3)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "DJ_SUBFILTER_DESCRIPTION3", joinColumns = @JoinColumn(name = "DJ_SUBFILTER_ID"), inverseJoinColumns = @JoinColumn(name = "DESCRIPTION3_ID"))
    private Set<Description3> descriptions3 = new HashSet<>();

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_PEP_OCCUPATIONS)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "DJ_SUBFILTER_PEP_OCC_CAT", joinColumns = @JoinColumn(name = "DJ_SUBFILTER_ID"), inverseJoinColumns = @JoinColumn(name = "PEP_OCCUPATION_CATEGORY_ID"))
    private Set<PEPOccupationCategory> pepOccupationCategories = new HashSet<>();

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_RESIDENCE_COUNTRIES)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "DJ_SUBFILTER_RES_COUNTRY", joinColumns = @JoinColumn(name = "DJ_SUBFILTER_ID"), inverseJoinColumns = @JoinColumn(name = "COUNTRY_ID"))
    private Set<Country> residenceCountries = new HashSet<>();

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_JURISDICTION_COUNTRIES)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "DJ_SUBFILTER_JUR_COUNTRY", joinColumns = @JoinColumn(name = "DJ_SUBFILTER_ID"), inverseJoinColumns = @JoinColumn(name = "COUNTRY_ID"))
    private Set<Country> jurisdictionCountries = new HashSet<>();

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_RELATION_TYPES)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "DJ_SUBFILTER_RELATION_TYPE", joinColumns = @JoinColumn(name = "DJ_SUBFILTER_ID"), inverseJoinColumns = @JoinColumn(name = "RELATION_TYPE_ID"))
    private Set<RelationType> relationTypes = new HashSet<>();

    @Getter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_GENDERS)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "DJ_SUBFILTER_GENDERS", joinColumns = @JoinColumn(name = "DJ_SUBFILTER_ID"), inverseJoinColumns = @JoinColumn(name = "GENDER_ID"))
    private Set<Gender> genders = new HashSet<>();

    @Getter @Setter
    @EqualityField
    @Column(name = "IS_DELETED")
    private boolean deleted;

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name = "FILTER_ID")
    private DJExportFilter filter;

    @Getter @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.SUBFILTER_NAME)
    @Column(name = "NAME")
    private String subfilterName;

    public DJExportSubfilter() {
    }

    public DJExportSubfilter(DJExportSubfilter other) {
        this.id = other.id;
        this.subfilterName = other.subfilterName;
        if(other.deathRCADateCriteria != null) {
            this.deathRCADateCriteria = new DJSubfilterDateCriteria(other.deathRCADateCriteria);
        }
        if(other.deathPepSipDateCriteria != null) {
            this.deathPepSipDateCriteria = new DJSubfilterDateCriteria(other.deathPepSipDateCriteria);
        }
        if(other.inactivityPepDateCriteria != null) {
            this.inactivityPepDateCriteria = new DJSubfilterDateCriteria(other.inactivityPepDateCriteria);
        }
        if(other.inactivityRcaDateCriteria != null) {
            this.inactivityRcaDateCriteria = new DJSubfilterDateCriteria(other.inactivityRcaDateCriteria);
        }
        if(other.description1 != null) {
            this.description1 = new DJSubfilterDescription1(other.description1);
        }

        this.deleted = other.deleted;
        this.genders = new HashSet<>(other.genders);
        this.sanctionTypes = new HashSet<>(other.sanctionTypes);
        this.descriptions3 = new HashSet<>(other.descriptions3);
        this.pepOccupationCategories = new HashSet<>(other.pepOccupationCategories);
        this.residenceCountries = new HashSet<>(other.jurisdictionCountries);
        this.jurisdictionCountries = new HashSet<>(other.relationTypes);
        this.relationTypes = new HashSet<>(other.relationTypes);
        this.index = other.index;
    }

    public void setDescription1(RecordCategory recordCategory, Boolean includeRca, Boolean includeRcaa) {
        description1 = new DJSubfilterDescription1(recordCategory, includeRca, includeRcaa);
    }

    public boolean isNotDeleted() { return !deleted; }

    public boolean isAll() {
        return description1 == null &&
                sanctionTypes.isEmpty() &&
                descriptions3.isEmpty() &&
                pepOccupationCategories.isEmpty();
    }

    public boolean includeRca() { return description1 != null && description1.isIncludeRca(); }

    public boolean includeRcaa() { return description1 != null && description1.isIncludeRcaa(); }

    @Override
    public String toStringAudit() { return this.index + " / "+ super.toStringAudit(); }
    public boolean hasPepSipDeathDateCriteria() {
        return deathPepSipDateCriteria != null &&
                deathPepSipDateCriteria.getYears() != null &&
                deathPepSipDateCriteria.getOperator() != null;
    }
    public boolean hasRCADeathDateCriteria() {
        return deathRCADateCriteria != null &&
                deathRCADateCriteria.getYears() != null &&
                deathRCADateCriteria.getOperator() != null;
    }
    public boolean hasInactivityPepDateCriteria() {
        return inactivityPepDateCriteria != null &&
                inactivityPepDateCriteria.getYears() != null &&
                inactivityPepDateCriteria.getOperator() != null;
    }
    public boolean hasInactivityRcaDateCriteria() {
        return inactivityRcaDateCriteria != null &&
                inactivityRcaDateCriteria.getYears() != null &&
                inactivityRcaDateCriteria.getOperator() != null;
    }
}
