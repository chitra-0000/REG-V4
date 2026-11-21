package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.ImportedFile.UploadStatus;
import com.bnpp.regliss.entity.ImportedFile.UploadType;
import com.bnpp.regliss.entity.batch.BatchJobExecution;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.entity.six.entity.InstrumentFile;
import com.bnpp.regliss.entity.six.entity.OptionsFile;
import com.bnpp.regliss.entity.six.entity.StructuredFile;
import com.bnpp.regliss.utils.TimeProvider;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.bnpp.regliss.entity.ImportFileType.NOT_SUPPORTED;
import static com.bnpp.regliss.entity.VersionStatus.CERTIFIED;
import static java.time.LocalDateTime.now;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "VERSION")
@DynamicUpdate
/**@SequenceGenerator(name = "VersionIdGenerator", initialValue = 100, sequenceName = "VERSION_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "VersionIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "VERSION_SEQ")} )
public class Version extends AbstractSimpleEntity {

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "VersionIdGenerator")
    protected Long id;

    @Getter @Setter
    @EqualityField
    @Column(name = "VERSION_NUMBER")
    private Long versionNumber;

    @ManyToOne
    @JoinColumn(name = "LIST_ID")
    private ReglissList list;

    @Getter @Setter
    @EqualityField
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private VersionStatus status = VersionStatus.DRAFT;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.VERSION_LEGAL_BASE)
    @Column(name = "LEGAL_BASE", length = 256)
    private String legalBase;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.VERSION_COMMENT_EN)
    @Column(name = "COMMENT_EN", length = 1000)
    private String commentEn;

    @Getter @Setter
    @AuditedField(AuditedFieldCode.VERSION_COMMENT_FR)
    @Column(name = "COMMENT_FR", length = 1000)
    private String commentFr;

    @Getter
    @EqualityField
    @Column(name = "CREATION_DATE")
    private LocalDateTime creationDate;

    @ManyToOne
    @Getter
    @JoinColumn(name = "CREATION_USER_ID")//, nullable = false)
    private User creationUser;

    @Getter
    @Setter
    @AuditedField(AuditedFieldCode.VERSION_START_DATE)
    @Column(name = "START_DATE")
    private LocalDate startDate = LocalDate.now();

    @Getter
    @Column(name = "PUBLICATION_DATE")
    private LocalDateTime publicationDate;

    @Getter
    @Column(name = "OFAC_PUBLICATION_DATE")
    private LocalDateTime concatenatedListPublicationDate;

    @Getter
    @Column(name = "VERIFICATION_DATE")
    private LocalDateTime verificationDate;

    @Getter
    @Column(name = "VERIFICATION_MESSAGE")
    private String verificationMessage;

    @Getter
    @Column(name = "VALIDATION_DATE")
    private LocalDateTime validationDate;

    @ManyToOne
    @JoinColumn(name = "VALIDATION_USER_ID")
    private User validationUser;

    @Getter
    @Column(name = "CERTIFICATION_DATE")
    private LocalDateTime certificationDate;

    @ManyToOne
    @JoinColumn(name = "CERTIFICATION_USER_ID")
    private User certificationUser;

    @Getter
    @Column(name = "RETURN_DATE")
    private LocalDateTime returnDate;

    @Getter
    @ManyToOne
    @JoinColumn(name = "RETURN_USER_ID")
    private User returnUser;

    @Getter
    @Column(name = "RETURN_COMMENT", length = 500)
    private String returnComment;

    @Getter @Setter
    @Column(name = "FILE_NAME")
    private String fileName;

    @Setter
    @Column(name = "IMPORT_FILE_TYPE")
    @Enumerated(EnumType.STRING)
    private ImportFileType importFileType = NOT_SUPPORTED;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL)
    private final List<GeneratedFile> generatedFiles = new ArrayList<>();

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL)
    private final List<AddModSupExportFile> addModSupExportFiles = new ArrayList<>();

    @Setter @Getter
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "VERSION_DJ_ID")
    private VersionDJ versionDJ;

    @Getter
    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL)
    private List<ImportedFile> importedFiles = new ArrayList<>();

    @Setter
    @Getter
    @Embedded
    private ComputedRecordNumbers computedRecordNumbers = new ComputedRecordNumbers();

    @Setter @Getter
    @OneToMany(mappedBy = "version")
    private List<BatchJobExecution> listBatchJobExecution;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<AddModSupAuditTrailFile> addModSupAuditTrailFiles = new ArrayList<>();

    @ManyToMany(mappedBy = "versions")
    private final Set<Reglementation> reglementations = new HashSet<>();

    @OneToMany
    private Set<StructuredFile> structuredFiles = new HashSet<>();

    @OneToMany
    private Set<InstrumentFile> instrumentFiles = new HashSet<>();

    @OneToMany
    private Set<OptionsFile> optionsFiles = new HashSet<>();

    public Set<Reglementation> getReglementations() { return unmodifiableSet(reglementations); }

    void addReglementation(Reglementation reglementation) { reglementations.add(reglementation); }

    void removeReglementation(Reglementation reglementation) { reglementations.remove(reglementation); }

    public Version() {
    }

    public Version(long id) { this.id = id; }


    public Version(Version other) {
        this.id = other.id;
        this.certificationDate = other.certificationDate;
        this.commentEn = other.commentEn;
        this.commentFr = other.commentFr;
        this.creationDate = other.creationDate;
        this.fileName = other.fileName;
        this.importFileType = other.importFileType;
        this.legalBase = other.legalBase;
        this.publicationDate = other.publicationDate;
        this.returnComment = other.returnComment;
        this.returnDate = other.returnDate;
        this.startDate = other.startDate;
        this.status = other.status;
        this.validationDate = other.validationDate;
        this.verificationDate = other.verificationDate;
        this.versionNumber = other.versionNumber;

        this.certificationUser = other.certificationUser;
        this.creationUser = other.creationUser;
        this.validationUser = other.validationUser;
        this.list = other.list;
    }

    public Optional<Version> getPreviousVersion() {
        return list.getVersions().stream()
                .filter(Version::isNotDeleted)
                .filter(v -> v.getVersionNumber() < versionNumber)
                .max(comparing(Version::getVersionNumber));
    }

    public boolean wasCertified() { return certificationDate != null; }

    public boolean wasValidated() { return validationDate != null; }

    public boolean wasNotCertified() { return certificationDate == null; }

    public boolean wasPublished() { return publicationDate != null; }

    public boolean wasNotPublished() { return publicationDate == null; }

    public boolean isDeleted() { return status == VersionStatus.DELETED; }

    public boolean isNotDeleted() { return status != VersionStatus.DELETED; }

    public boolean statusIsOneOf(VersionStatus... statuses) { return Arrays.asList(statuses).contains(status); }

    // TODO: [GAP] lines 238-270 not captured in screenshots — do NOT fabricate missing logic.

    public ReglissList getList() { return list; }

    public Long getListId(){
        if(list != null){
            return list.getId();
        }
        return null;
    }

    void setList(ReglissList list) { this.list = list; }

    public void create(User author) {
        status = VersionStatus.DRAFT;
        creationUser = author;
        creationDate = TimeProvider.now();
    }

    public void validate(User author) {
        status = VersionStatus.VALIDATED;
        validationUser = author;
        validationDate = LocalDateTime.now();
    }

    public void returnForValidation(User author, String comment) {
        status = VersionStatus.RETURNED;
        returnUser = author;
        returnDate = now();
        returnComment = comment;
        verificationDate = null;
        verificationMessage = null;
    }

    public void certify(User author) { certify(author, now()); }
    public void certify(User author, LocalDateTime certificationDate) {
        this.status = CERTIFIED;
        this.certificationUser = author;
        this.certificationDate = certificationDate;
    }

    public void confirm(User author) { certify(author); }

    // TODO: [GAP] lines 318-319 not captured in screenshots — do NOT fabricate missing logic.

    public void publish(VersionStatus publicationStatus, LocalDateTime publicationDate) {
        status = publicationStatus;
        setPublicationDate(publicationDate);
    }

    public void autoPublish() { publish(VersionStatus.PUBLISHED_OK, now()); }

    public void deliver(VersionStatus deliveryStatus) { status = deliveryStatus; }

    public void delete() { status = VersionStatus.DELETED; }

    public void setReturnComment(String returnComment) { this.returnComment = returnComment; }

    public String computeVersionReference() { return this.getList().getReference() + "." + this.getVersionNumber(); }

    @Override
    public String toString() { return "Version{id="+id+", number="+versionNumber+"}"; }

    public List<GeneratedFile> getGeneratedFiles() {
        return Collections.unmodifiableList(this.generatedFiles.stream()
                .filter(gf -> gf.getGeneratedFileFormat().getFileFormat() != FileExportFormat.DELIVERY_REPORT)
                .collect(toList()));
    }

    public void addGeneratedFile(final GeneratedFile generatedFile) {
        generatedFiles.add(generatedFile);
        generatedFile.setVersion(this);
    }

    public void addAddModSupExportFile(final AddModSupExportFile addModSupExportFile) {
        addModSupExportFiles.add(addModSupExportFile);
        addModSupExportFile.setVersion(this);
    }

    public void addAddModSupAuditTrailFile(final AddModSupAuditTrailFile addModSupAuditTrailFile) {
        addModSupAuditTrailFiles.add(addModSupAuditTrailFile);
        addModSupAuditTrailFile.setVersion(this);
    }

    @PreRemove
    public void removeAlladdModSupAuditTrailFiles() { addModSupAuditTrailFiles.clear(); }

    public void addImportedFile(final ImportedFile importedFile) {
        importedFiles.add(importedFile);
        importedFile.setVersion(this);
    }

    public void removeGeneratedFile(final GeneratedFile generatedFile) { generatedFiles.remove(generatedFile); }

    public String getListAndVersionReference() {
        return String.format("%s.%03d", list.getReference().replaceFirst("^0+", ""), versionNumber);
    }

    public String getListVersionPrefix() { return list.getReference() + "." + versionNumber + "."; }

    public String getVersionReference() { return String.format("%03d", versionNumber); }

    public void verify(LocalDateTime time, String verificationMessage) {
        if (isVerified())  {
            throw new IllegalStateException("Version was already verified on " + verificationDate);
        }
        verificationDate = time;
        this.verificationMessage = verificationMessage;
    }

    public boolean isVerified() { return verificationDate != null; }

    public boolean isIntegrated() { return this.wasAutoUploaded() && this.status == VersionStatus.VALIDATED; }

    public boolean isConfirmed() { return this.wasAutoUploaded() && this.wasCertified(); }

    public Optional<User> getCertificationUserOpt() { return ofNullable(certificationUser); }

    public Optional<User> getValidationUserOpt() { return ofNullable(validationUser); }

    public Optional<AddModSupExportFile> getLastAddModSupExportFileOpt() {
        return this.addModSupExportFiles.stream()
                .max(Comparator.comparing(AddModSupExportFile::getGenerationDate));
    }

    public Optional<AddModSupAuditTrailFile> getLastAddModSupAuditTrailFileOpt() {
        return this.addModSupAuditTrailFiles.stream()
                .max(Comparator.comparing(AddModSupAuditTrailFile::getGenerationDate));
    }

    public Optional<AddModSupAuditTrailFile> getLastOKAddModSupAuditTrailFileOpt() {
        return this.addModSupAuditTrailFiles.stream()
                .filter(AddModSupAuditTrailFile :: isCorrectlyGenerated)
                .max(Comparator.comparing(AddModSupAuditTrailFile::getGenerationDate));
    }

    public ImportFileType getImportFileType() {
        if(wasCertified())
            return importFileType;
        else
            return list.getImportFileType();
    }

    public boolean wasAutoUploaded() {
        return getImportedFileOk()
                .map(importedFile -> importedFile.getUploadType() == UploadType.AUTO)
                .orElse( versionDJ != null );
    }

    public Optional<ImportedFile> getImportedFileOk() {
        return getImportedFiles().stream()
                .filter(importedFile -> importedFile.getStatus() == UploadStatus.IMPORTED_OK)
                //TODO remove this null check in lot 2 of asap250, when all the files should have uploadDate, even for auto import
                .sorted(comparing((ImportedFile f) -> f.getUploadDate() != null ? f.getUploadDate() : LocalDateTime.now()).reversed())
                .findFirst();
    }

    public boolean isConfirmedForDJLists() { return wasCertified() && isDjFormat() && status == CERTIFIED; }

    public boolean isCertifiedForCustomAndNonAutoLists() { return wasCertified() && !isDjFormat() && status == CERTIFIED; }

    private boolean isDjFormat() { return getImportFileType().isDjFormat(); }

    public void setPublicationDate(LocalDateTime publicationDate) {
        if (wasNotPublished()) {
            this.publicationDate = publicationDate;
        }
    }

    public void setConcatenatedListPublicationDate(LocalDateTime concatenatedListPublicationDate) {
        if (this.concatenatedListPublicationDate == null) {
            this.concatenatedListPublicationDate = concatenatedListPublicationDate;
        }
    }

    public Optional<AddModSupAuditTrailFile> getPreviousaddModSupAuditTrailFileByGenerationDateOpt(LocalDateTime generationDate) {
        return this.addModSupAuditTrailFiles.stream()
                .filter(addModSupAuditTrailFile -> addModSupAuditTrailFile.getGenerationDate().isBefore(generationDate))
                .max(Comparator.comparing(AddModSupAuditTrailFile::getGenerationDate));
    }

    public Set<StructuredFile> getStructuredFile() { return unmodifiableSet(this.structuredFiles); }
    public Set<InstrumentFile> getInstrumentFiles() { return unmodifiableSet(this.instrumentFiles); }
    public Set<OptionsFile> getOptionsFiles() { return unmodifiableSet(this.optionsFiles); }
}
