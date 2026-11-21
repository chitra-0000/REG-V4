package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Blob;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class ExportFile extends AbstractSimpleEntity {

    public enum GenerationStatus {
        OK, IN_PROGRESS, KO
    }

    @Getter @Setter
    @EqualityField
    @Column(name = "FILE_NAME")
    private String fileName;

    @Getter @Setter
    @EqualityField
    @Column(name = "GENERATION_TIME")
    private LocalDateTime generationDate;

    @Getter @Setter
    @EqualityField
    @Column(name = "GENERATION_STATUS")
    @Enumerated(EnumType.STRING)
    private GenerationStatus generationStatus = GenerationStatus.IN_PROGRESS;

    @Getter @Setter
    @EqualityField
    @Column(name = "ERROR_MESSAGE")
    @Lob
    private String errorMessage;

    @Getter @Setter
    @EqualityField
    @Column(name = "FILE_CONTENT")
    @Lob
    private Blob fileContent;

    @Getter @Setter
    @ManyToOne
    @JoinColumn(name = "VERSION_ID")
    private Version version;

    public boolean isCorrectlyGenerated() { return generationStatus == GenerationStatus.OK; }

    public boolean isNotCorrectlyGenerated() { return generationStatus != GenerationStatus.OK; }

    public boolean isKO() { return generationStatus == GenerationStatus.KO; }

    public boolean isInProgress() { return generationStatus == GenerationStatus.IN_PROGRESS; }

    @Override
    public String toString() {
        return "ExportFile {" +
                "fileName='" + fileName + '\'' +
                ", generationDate=" + generationDate +
                ", generationStatus=" + generationStatus +
                ", errorMessage='" + errorMessage + '\'' +
                ", version=" + version +
                ", id=" + getId() +
                '}';
    }

}
