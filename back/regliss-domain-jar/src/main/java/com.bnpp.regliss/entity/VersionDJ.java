package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.*;

@Entity
@Table(name = "VERSION_DJ")
/**@SequenceGenerator(name = "VersionDJIdGenerator", initialValue = 100, sequenceName = "GENERATED_VERSION_DJ_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "VersionDJIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "GENERATED_VERSION_DJ_SEQ") } )
public class VersionDJ extends AbstractSimpleEntity {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "VersionDJIdGenerator")
    protected Long id;
    @EqualityField
    @Setter
    @Getter
    @Column(name = "REFERENTIALS_PREFIX_SNIPPET")
    @Lob
    private String referentialsXmlPrefixSnippet;

    @Setter
    @Getter
    @OneToOne(mappedBy = "versionDJ")
    private Version version;

    @Setter
    @Getter
    @Embedded
    private LogRecordNumbers logRecordNumbers;

    public VersionDJ() {
        // Do nothing
    }
}
