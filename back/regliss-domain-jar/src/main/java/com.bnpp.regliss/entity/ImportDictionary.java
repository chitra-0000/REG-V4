package com.bnpp.regliss.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class ImportDictionary extends AbstractSimpleEntity {

    @Getter @Setter
    @Column(name = "LABEL_IN_FILE", nullable = false)
    private String labelInFile;

    @Getter @Setter
    @Column(name = "LABEL_TYPE", nullable = false)
    private String labelType;

    @Getter @Setter
    @Column(name = "LABEL_ID")
    private String labelId;

    public ImportDictionary() {
    }

    public ImportDictionary(String labelInFile, String labelType, String labelId) {
        this.labelInFile = labelInFile;
        this.labelType = labelType;
        this.labelId = labelId;
    }

    @Override
    public String toString() { return "\"" + labelInFile + "\" on " + labelType + " = " + labelId; }
}
