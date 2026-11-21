package com.bnpp.regliss.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;

@Entity
@Table(name = "ADD_MOD_SUP_FILE")
/**@SequenceGenerator(name = "AddModSupExportFileIdGenerator", initialValue = 100, sequenceName = "SYNTHESIS_FILE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "AddModSupExportFileIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "SYNTHESIS_FILE_SEQ")} )
public class AddModSupExportFile extends ExportFile {
    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "AddModSupExportFileIdGenerator")
    protected Long id;

    public AddModSupExportFile() { super(); }
}
