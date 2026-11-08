package com.bnpp.regliss.facade.dto.version;

// [MISSING FROM SCREENSHOTS: lines 3-9 of the original file were not visible in the photos]

public class VersionDtoView extends VersionDtoForm {

    public String fileName;
    public long numberOfRecordsInPreviousVersion;
    public LocalDateTime validationDate;
    public String validationUser;
    public LocalDateTime certificationDate;
    public String certificationUser;
    public LocalDateTime publicationDate;
    public LocalDateTime verificationDate;
    public String listName;
    public BreadcrumbDto breadcrumbDto;
    public boolean isLocked = false;
    public String lockingUser;
    public boolean isVerified;
    public boolean isReadOnly;
    public ImportFileType importFileType;
    public boolean allowsAutoUpload;
    public boolean allowsManualUpload;
    public Confidentiality listConfidentiality;
    public VersionActions actions;
    public ComputedRecordNumbersDto computedRecordNumbersDto;
    public boolean isImportedFileUploaded;
    public boolean canRegenerateAddModSupFile;
    public boolean canRegenerateAddModSupAuditTrailFile;
    public boolean canDownloadAddModSupAuditTrailFile;
    public boolean isDeprecatedAddModSupAuditTrailFile;
    public GenerationStatus addModSupGenerationStatus;
    public Long previousVersionId;
    public Long previousVersionNumber;
    public boolean showImportErrors;
}

