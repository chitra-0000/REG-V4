package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.annotation.AuditedField;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "PERSON_IMAGE")
/**@SequenceGenerator(name = "PersonImageIdGenerator", initialValue = 100, sequenceName = "PERSON_IMAGE_SEQ", allocationSize = 100)*/
@GenericGenerator( name = "PersonImageIdGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "PERSON_IMAGE_SEQ") } )
public class PersonImage extends AbstractSimpleEntity implements RecordAwareChild, ToStringAuditable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(generator = "PersonImageIdGenerator")
    protected Long id;
    @Getter
    @Setter
    @EqualityField
    @AuditedField(AuditedFieldCode.PERSON_IMAGE_URL)
    @Column(name = "URL", length = 300)
    private String url;

    @ManyToMany(mappedBy = "personImages")
    private List<PersonRecord> personRecords = new ArrayList<>();

    public PersonImage() {
    }

    public PersonImage(Long id, String url) {
        this.id = id;
        this.url = url;
    }

    public PersonImage(Long id) { this.id = id; }

    public PersonImage(PersonImage other) {
        this.id = other.id;
        this.url = other.url;
    }

    void addPersonRecord(PersonRecord record) { personRecords.add(record); }

    void removeRecord(PersonRecord record) { personRecords.remove(record); }

    public List<PersonRecord> getRecords() { return unmodifiableList(personRecords); }

    @Override
    public void removeFromParent(Record parentToUnlink) {
        if (parentToUnlink instanceof PersonRecord) {
            ((PersonRecord) parentToUnlink).removePersonImage(this);
        } else {
            throw new IllegalArgumentException();
        }
    }

    void removePersonRecord(PersonRecord personRecord) { personRecords.remove(personRecord); }
}
