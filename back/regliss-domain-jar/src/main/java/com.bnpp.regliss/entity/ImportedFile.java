package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;
import java.sql.Blob;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "IMPORTED_FILE")
/**@SequenceGenerator(name = "ImportedFileIdGenerator", initialValue = 100, sequenceName = "IMPORTED_FILE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "ImportedFileIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "IMPORTED_FILE_SEQ") } )
public class ImportedFile extends AbstractSimpleEntity {

    public enum UploadStatus {
        UPLOAD_OK, IMPORTING, IMPORT_KO, IMPORTED_OK
    }

    public enum UploadType {
        MANUAL, AUTO
    }

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "ImportedFileIdGenerator")
    protected Long id;

    @EqualityField
    @Setter
    @Getter
    @Column(name = "ORIGINAL_FILE_NAME")
    private String originalFileName;

    @Getter @Setter
    @EqualityField
    @Column(name = "FILE_CONTENT")
    @Lob
    private Blob fileContent;

    @Getter @Setter
    @EqualityField
    @Column(name = "FILE_NAME")
    private String fileName;

    @Getter @Setter
    @EqualityField
    @Column(name = "UPLOAD_TIME")
    private LocalDateTime uploadDate;

    @Getter @Setter
    @EqualityField
    @Column(name = "USER_NAME")
    private String userName;

    @Getter @Setter
    @EqualityField
    @Column(name = "PROCESS_NODE")
    private String processNode;

    @Getter @Setter
    @EqualityField
    @Column(name = "STATUS", nullable=false)
    @Enumerated(EnumType.STRING)
    private UploadStatus status = UploadStatus.UPLOAD_OK;

    @Getter @Setter
    @EqualityField
    @Column(name = "UPLOAD_TYPE", nullable=false)
    @Enumerated(EnumType.STRING)
    private UploadType uploadType;

    @Setter
    @EqualityField
    @Column(name = "ERROR_MESSAGE")
    @Lob
    private String errorMessage;

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name = "VERSION_ID")
    private Version version;

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name = "LIST_ID")
    private ReglissList list;

    public Optional<String> getErrorMessageOpt() { return Optional.ofNullable(errorMessage); }

    public UploadedFileExtension getExtension() { return UploadedFileExtension.fromFileName(originalFileName); }

    public boolean isImportKO(){ return status == UploadStatus.IMPORT_KO;}

    public boolean isImportOK(){ return status == UploadStatus.IMPORTED_OK;}

}
