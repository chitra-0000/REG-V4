package com.bnpp.regliss.entity;

import com.bnpp.regliss.annotation.EqualityField;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.persistence.Entity;
import java.sql.Blob;
import java.time.LocalDateTime;

@Entity
@Table(name = "USER_GUIDE_FILE")
/**@SequenceGenerator(name = "UserGuideFileIdGenerator", initialValue = 100, sequenceName = "USER_GUIDE_FILE_SEQ", allocationSize = 1)*/
@GenericGenerator( name = "UserGuideFileIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "USER_GUIDE_FILE_SEQ")} )
public class UserGuideFile extends AbstractSimpleEntity{

    @Id
    @Getter
    @Setter
    @GeneratedValue( generator = "UserGuideFileIdGenerator")
    protected Long id;

    @Getter @Setter
    @EqualityField
    @Column(name = "FILE_NAME")
    private String fileName;

    @Getter @Setter
    @EqualityField
    @Column(name = "FILE_CONTENT")
    @Lob
    private Blob fileContent;

    @Getter @Setter
    @EqualityField
    @Column(name = "UPLOAD_TIME")
    private LocalDateTime uploadDate;

    @Getter @Setter
    @EqualityField
    @Column(name = "USER_NAME")
    private String userName;
}
