package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.*;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import com.bnpp.regliss.entity.referential.record.SanctionType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name="REGLEMENTATION")
/**@SequenceGenerator(name = "ReglementationIdGenerator", initialValue = 100, sequenceName = "REGLEM_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "ReglementationIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "REGLEM_SEQ") } )
public class Reglementation extends AbstractSimpleEntity implements ToStringAuditable{

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "ReglementationIdGenerator")
    protected Long id;

    public enum ReglementationType {
        REGLEMENTATION_TEXT,
        INSTRUCTION,
        PROCEDURE
    }

    @Getter @Setter
    @AuditedField(AuditedFieldCode.REGLEMENTATION_TITLE)
    @Column(name="TITLE", nullable= false, length = 512)
    @EqualityField
    private String title;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.REGLEMENTATION_REFERENCE)
    @Column(name="REFERENCE", nullable=false, length = 512)
    @EqualityField
    private String reference;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.REGLEMENTATION_RECEPTION_DATE)
    @Column(name="RECEPTION_DATE")
    @EqualityField
    private LocalDate receptionDate;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.REGLEMENTATION_OFFICIAL_DATE)
    @Column(name="OFFICIAL_DATE", nullable=false)
    @EqualityField
    private LocalDate officialDate;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.REGLEMENTATION_URL)
    @Column(name="URL")
    @EqualityField
    private String url;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.REGLEMENTATION_TYPE)
    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE")
    @EqualityField
    private ReglementationType type;

    @Setter
    @AuditedField(AuditedFieldCode.REGLEMENTATION_SANCTION_TYPE)
    @ManyToOne
    @JoinColumn(name = "SANCTION_ID")//, nullable = false)
    @EqualityField
    private SanctionType sanctionType;

    @Setter
    @AuditedField(AuditedFieldCode.REGLEMENTATION_PROGRAM)
    @ManyToOne
    @JoinColumn(name = "PROGRAM_ID")// , nullable = false)
    @EqualityField
    private Program program;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.REGLEMENTATION_COMMENT)
    @Column(name = "COMMENTS")
    @EqualityField
    private String comment;

    @Getter @Setter
    @Lob @Basic(fetch=FetchType.LAZY)
    @Column(name="FILEDATA" ,nullable=false)
    @EqualityField
    private byte[] fileData;

    @Getter @Setter
    @Column(name="FILE_CONTENT_TYPE", nullable= false)
    @EqualityField
    private String fileContentType;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.REGLEMENTATION_FILE)
    @Column(name="FILE_NAME", nullable= false)
    @EqualityField
    private String fileName;

    @Getter @Setter
    @Column(name = "IS_DELETED", nullable = false)
    @EqualityField
    private boolean deleted;

    @Setter
    @ManyToOne
    @JoinColumn(name = "LIST_ID")
    private ReglissList list;

    @Setter
    @ManyToOne
    @JoinColumn(name = "VERSION_ID")
    private Version version;

    @Valid
    @AuditedCollection(createAction = AuditedAction.VERSION_REGLEMENTATION_CREATION,
            updateAction = AuditedAction.VERSION_REGLEMENTATION_MODIFICATION,
            deleteAction = AuditedAction.VERSION_REGLEMENTATION_DELETION)
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "VERSION_REGLEMENTATION", joinColumns = @JoinColumn(name = "REGLEMENTATION_ID"), inverseJoinColumns = @JoinColumn(name = "VERSION_ID"))
    private final Set<Version> versions = new HashSet<>();

    public Reglementation() {
    }


    public Reglementation(Reglementation other) {
        this.id = other.id;
        this.title = other.title;
        this.reference = other.reference;
        this.receptionDate = other.receptionDate;
        this.officialDate = other.officialDate;
        this.url = other.url;
        this.type = other.type;
        this.sanctionType = other.sanctionType;
        this.program = other.program;
        this.comment = other.comment;
        this.fileData = other.fileData;
        this.fileContentType = other.fileContentType;
        this.fileName = other.fileName;
        this.deleted = other.deleted;
    }


    public boolean belongsToList() { return list != null; }

    public boolean wasInheritedFromListToVersions() {
        boolean belongsToListAndVersions = belongsToList() && !versions.isEmpty();
        boolean belongsToSeveralVersions = !belongsToList() && versions.size() > 1;

        return belongsToListAndVersions || belongsToSeveralVersions;
    }

    public boolean wasNotInheritedFromListToVersions() { return !wasInheritedFromListToVersions(); }

    public boolean hasAllPrograms() { return program == null; }

    public void setAllPrograms() { program = null; }

    public Set<Version> getVersions() { return Collections.unmodifiableSet(this.versions); }

    public void addVersion(final Version version) {
        versions.add(version);
        version.addReglementation(this);
    }


    public void removeVersion(final Version version) {
        versions.remove(version);
        version.removeReglementation(this);
    }

    public Optional<SanctionType> getSanctionTypeOpt() { return Optional.ofNullable(sanctionType); }

    public Optional<Program> getProgramOpt() { return Optional.ofNullable(program); }

    public Optional<ReglissList> getListOpt() { return Optional.ofNullable(list); }
}
